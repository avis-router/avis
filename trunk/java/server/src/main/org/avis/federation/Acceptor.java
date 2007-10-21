package org.avis.federation;

import java.util.HashSet;
import java.util.Set;

import java.io.Closeable;
import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import org.avis.config.Options;
import org.avis.federation.io.FederationFrameCodec;
import org.avis.federation.io.messages.FedConnRply;
import org.avis.federation.io.messages.FedConnRqst;
import org.avis.io.LivenessFilter;
import org.avis.io.RequestTrackingFilter;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.router.Router;

import static org.apache.mina.common.IdleStatus.READER_IDLE;
import static org.apache.mina.common.IoFutureListener.CLOSE;

import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.federation.Federation.logError;
import static org.avis.federation.Federation.logMessageReceived;
import static org.avis.io.FrameCodec.setMaxFrameLengthFor;
import static org.avis.io.Net.hostAddressFor;
import static org.avis.io.Net.hostIdFor;
import static org.avis.io.messages.Nack.PROT_INCOMPAT;
import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.info;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.warn;

/**
 * Listens for incoming federation connections and establishes
 * FederationLink's for them. After accepting a connection and
 * successfully handshaking with a FedConnRqst/FedConnRply, it creates
 * and hands over processing to a Link.
 * 
 * @see Link
 * @see Connector
 * 
 * @author Matthew Phillips
 */
public class Acceptor implements IoHandler, Closeable
{
  /**
   * Avis-specific NACK code used when an invalid server domain is
   * detected (NACK codes in range 2500-2599 are allocated for
   * implementation-specific use).
   */
  public static final int INVALID_DOMAIN = 2500;
  
  protected Router router;
  protected Options options;
  protected String serverDomain;
  protected FederationClasses federationClasses;
  protected Set<Link> links;
  protected Set<InetSocketAddress> listenAddresses;
  protected volatile boolean closing;

  public Acceptor (Router router,
                   String serverDomain,
                   FederationClasses federationClasses, 
                   Set<InetSocketAddress> listenAddresses, 
                   Options options)
    throws IOException
  {
    this.router = router;
    this.serverDomain = serverDomain;
    this.federationClasses = federationClasses;
    this.listenAddresses = listenAddresses;
    this.options = options;
    this.links = new HashSet<Link> ();
    
    int requestTimeout = options.getInt ("Federation.Request-Timeout") * 1000;
    int keepaliveInterval = 
      options.getInt ("Federation.Keepalive-Interval") * 1000;
    
    SocketAcceptorConfig acceptorConfig = new SocketAcceptorConfig ();
    
    acceptorConfig.setReuseAddress (true);
    acceptorConfig.setThreadModel (ThreadModel.MANUAL);
    
    DefaultIoFilterChainBuilder filterChain = 
      acceptorConfig.getFilterChain ();

    filterChain.addLast ("codec", FederationFrameCodec.FILTER);
    
    filterChain.addLast
      ("requestTracker", new RequestTrackingFilter (requestTimeout));
    
    filterChain.addLast
      ("liveness", new LivenessFilter (keepaliveInterval, requestTimeout));
    
    SocketAcceptor acceptor = router.socketAcceptor ();

    for (InetSocketAddress address : listenAddresses)
    {
      if (shouldLog (DIAGNOSTIC))
        diagnostic ("Federator listening on address: " + address, this);

      acceptor.bind (address, this, acceptorConfig);
    }
  }

  public synchronized void close ()
  {
    if (closing)
      return;
    
    closing = true;
    
    for (Link link : links)
      link.close ();
    
    links.clear ();

    SocketAcceptor socketAcceptor = router.socketAcceptor ();

    for (InetSocketAddress address : listenAddresses)
    {
      // wait for pending messages to be written
      // todo check that this really flushes messages
      for (IoSession session : socketAcceptor.getManagedSessions (address))
        session.close ().join (10000);
      
      socketAcceptor.unbind (address);
    }
  }
  
  /**
   * Simulate a hang by stopping all responses to messages.
   */
  public void hang ()
  {
    for (InetSocketAddress address : listenAddresses)
    {
      for (IoSession session : 
           router.socketAcceptor ().getManagedSessions (address))
      {
        RequestTrackingFilter requestTracker = 
          (RequestTrackingFilter)session.getFilterChain ().get ("requestTracker");
        
        requestTracker.hang (session);
      }
    }
    
    closing = true;
  }
  
  private void handleMessage (IoSession session,
                              Message message)
  {
    switch (message.typeId ())
    {
      case FedConnRqst.ID:
        handleFedConnRqst (session, (FedConnRqst)message);
        break;
      case ErrorMessage.ID:
        logError ((ErrorMessage)message, this);
        session.close ();
        break;
      default:
        warn ("Unexpected handshake message from connecting remote " +
              "federator at " + hostIdFor (session) + 
              " (disconnecting): " + message.name (), this);
        session.close ();
    }
  }

  private void handleFedConnRqst (IoSession session, FedConnRqst message)
  {
    InetAddress remoteHost = hostAddressFor (session);
    String hostName = remoteHost.getCanonicalHostName ();
    
    if (message.versionMajor != VERSION_MAJOR || 
        message.versionMinor > VERSION_MINOR)
    {
      String disconnMessage =
        "Incompatible federation protocol version: " + 
         message.versionMajor + "." + message.versionMinor +
         " not compatible with this federator's " + 
         VERSION_MAJOR + "." + VERSION_MINOR;
      
      warn ("Rejected federation request from " + hostName + ": " + 
            disconnMessage, this);
      
      send (session,
            new Nack (message,
                      PROT_INCOMPAT, disconnMessage)).addListener (CLOSE);
    } else
    {
      Link existingLink = linkForDomain (message.serverDomain);
      FederationClass fedClass = 
        federationClasses.classFor (remoteHost, message.serverDomain);

      if (existingLink != null)
      {
        nackInvalidDomain 
          (session, message, 
           "using a federation domain already in use by \"" + 
           hostIdFor (existingLink.session ()),
           "Server domain " + message.serverDomain + " already in use");
      } else if (fedClass.allowsNothing ())
      {
        nackInvalidDomain 
          (session, message, 
           "no provide/subscribe defined for its hostname/server " +
           "domain", "No federation import/export allowed for host");
      } else if (message.serverDomain.equalsIgnoreCase (serverDomain))
      {
        nackInvalidDomain 
          (session, message, 
           "using the same server domain the remote router", 
           "Server domain is the same as the remote router's");
      } else
      {
        send (session, new FedConnRply (message, serverDomain));
        
        info ("Federation incoming link established with \"" + 
              hostIdFor (session) + "\", remote server domain \"" + 
              message.serverDomain + "\", federation class \"" + 
              fedClass.name + "\"", this);
        
        createLink
          (session, message.serverDomain, hostName, fedClass);
      }
    }
  }
  
  /**
   * Send a NACK for invalid server domain.
   */
  private void nackInvalidDomain (IoSession session,
                                  FedConnRqst message, String logMessage,
                                  String nackMessage)
  {
    warn ("Remote federator has been denied connection due to " + logMessage + 
          ", host = " + hostIdFor (session) + 
          ", server domain = " + message.serverDomain, this);
  
    send (session, 
          new Nack
            (message, INVALID_DOMAIN, nackMessage)).addListener (CLOSE);
  }

  private synchronized void createLink (IoSession session,
                                        String remoteServerDomain,
                                        String remoteHost, 
                                        FederationClass federationClass)
  {
    Link link =
      new Link (session, router, federationClass,
                serverDomain, remoteServerDomain, remoteHost);
    
    session.setAttribute ("federationLink", link);
    
    links.add (link);
  }

  private synchronized void removeLink (Link link)
  {
    links.remove (link);
  }
  
  private synchronized Link linkForDomain (String domain)
  {
    for (Link link : links)
    {
      if (link.remoteServerDomain ().equalsIgnoreCase (domain))
        return link;
    }
    
    return null;
  }
  
  private static Link linkFor (IoSession session)
  {
    return (Link)session.getAttribute ("federationLink");
  }

  private WriteFuture send (IoSession session, Message message)
  {
    return Federation.send (session, serverDomain, message);
  }
  
  // IoHandler
  
  public void sessionCreated (IoSession session)
    throws Exception
  {
    // federators have 20 seconds to send a FedConnRqst
    session.setIdleTime (READER_IDLE, 20);
    
    setMaxFrameLengthFor (session, options.getInt ("Packet.Max-Length"));
  }

  public void sessionOpened (IoSession session)
    throws Exception
  {
    if (closing)
      session.close ();
  }

  public void messageReceived (IoSession session, Object theMessage)
    throws Exception
  {
    if (closing)
      return;
    
    Message message = (Message)theMessage;
    
    logMessageReceived (message, serverDomain, this);
    
    Link link = linkFor (session);
    
    if (link == null)
      handleMessage (session, message);
    else if (!link.isClosed ())
      link.handleMessage (message);
  }

  public void sessionClosed (IoSession session)
    throws Exception
  {
    Link link = linkFor (session);
    
    if (link != null)
    {
      if (!link.isClosed ())
      {
        warn ("Remote host \"" + hostIdFor (session) + 
              "\" closed incoming federation link with no warning", this);

        link.close ();
      } else
      {
        info ("Federation link with \"" + hostIdFor (session) + 
              "\" disconnected", this);
      }
      
      removeLink (link);
    }
  }
  
  public void exceptionCaught (IoSession session, Throwable cause)
    throws Exception
  {
    warn ("Unexpected exception while processing federation message", 
          this, cause);
  }

  public void messageSent (IoSession session, Object message)
    throws Exception
  {
    // zip
  }

  public void sessionIdle (IoSession session, IdleStatus status)
    throws Exception
  {
    if (status == READER_IDLE && linkFor (session) == null)
    {
      warn ("Disconnecting incoming federation connection from " + 
            hostIdFor (session) + 
            " due to failure to send connect request", this);
      
      session.close ();
    }
  }
}
