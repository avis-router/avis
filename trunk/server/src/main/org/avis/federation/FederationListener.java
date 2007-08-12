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
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import org.avis.federation.messages.FedConnRply;
import org.avis.federation.messages.FedConnRqst;
import org.avis.io.RequestTrackingFilter;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.router.Router;

import static org.apache.mina.common.IoFutureListener.CLOSE;
import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.federation.Federation.logError;
import static org.avis.federation.Federation.logMessageReceived;
import static org.avis.io.messages.Disconn.REASON_PROTOCOL_VIOLATION;
import static org.avis.io.messages.Nack.IMPL_LIMIT;
import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.warn;

/**
 * Listens for incoming federation connections and establishes
 * FederationLink's for them.
 * 
 * @see FederationConnector
 * 
 * @author Matthew Phillips
 */
public class FederationListener implements IoHandler, Closeable
{
  protected Router router;
  protected Set<FederationLink> links;
  protected Set<InetSocketAddress> addresses;
  protected String serverDomain;
  protected FederationClassMap federationClassMap;
  protected volatile boolean closing;

  public FederationListener (Router router,
                             String serverDomain,
                             FederationClassMap federationClassMap, 
                             Set<InetSocketAddress> addresses)
    throws IOException
  {
    this.router = router;
    this.serverDomain = serverDomain;
    this.federationClassMap = federationClassMap;
    this.addresses = addresses;
    this.links = new HashSet<FederationLink> ();
    
    SocketAcceptorConfig acceptorConfig = new SocketAcceptorConfig ();
    
    acceptorConfig.setReuseAddress (true);
    acceptorConfig.setThreadModel (ThreadModel.MANUAL);
    
    DefaultIoFilterChainBuilder filterChain = acceptorConfig.getFilterChain ();

    filterChain.addLast ("codec", FederationFrameCodec.FILTER);
    filterChain.addLast ("requestTracker", new RequestTrackingFilter (20));
    
    for (InetSocketAddress address : addresses)
    {
      if (shouldLog (DIAGNOSTIC))
        diagnostic ("Federator listening on address: " + address, this);

      router.socketAcceptor ().bind (address, this, acceptorConfig);
    }
  }

  public void close ()
  {
    synchronized (this)
    {
      if (closing)
        return;
      
      closing = true;
      
      for (FederationLink link : links)
        link.close ();
      
      links.clear ();

      for (InetSocketAddress address : addresses)
        router.socketAcceptor ().unbind (address);
      
      addresses.clear ();
    }
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
              "federator at " + remoteHostFor (session) + 
              " (disconnecting): " + message.name (), this);
        session.close ();
    }
  }

  private void handleFedConnRqst (IoSession session, FedConnRqst message)
  {
    InetAddress remoteHost = remoteHostFor (session);
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
            new Disconn (REASON_PROTOCOL_VIOLATION, 
                         disconnMessage)).addListener (CLOSE);
    } else
    {
      // todo should check that server domain is not already known
      FederationClass fedClass = 
        federationClassMap.classFor (remoteHost, message.serverDomain);

      if (!fedClass.isNull ())
      {
        send (session, new FedConnRply (message, serverDomain));
       
        diagnostic ("Federation incoming link established with " + 
                    hostName + ", remote server domain \"" + 
                    message.serverDomain + "\"", this);
      
        createFederationLink
          (session, message.serverDomain, hostName, fedClass);
      } else
      {
        warn ("Remote federator has been denied connection due to no " +
              "provide/subscribe defined for its hostname/server domain: " +
              "host = " + hostName + ", " + 
              "server domain = " + message.serverDomain, this);
        
        // todo what NACK code to use here?
        send (session, 
              new Nack
                (message, IMPL_LIMIT,
                 "No federation import/export allowed")).addListener (CLOSE);
      }
    }
  }
  
  private void createFederationLink (IoSession session,
                                     String remoteServerDomain,
                                     String remoteHost, 
                                     FederationClass federationClass)
  {
    FederationLink link =
      new FederationLink (session, router,
                          federationClass, serverDomain, 
                          remoteServerDomain, remoteHost);
    
    addLink (session, link);
  }

  private synchronized void addLink (IoSession session, FederationLink link)
  {
    session.setAttribute ("federationLink", link);
    links.add (link);
  }
  
  private static FederationLink linkFor (IoSession session)
  {
    return (FederationLink)session.getAttribute ("federationLink");
  }

  private static InetAddress remoteHostFor (IoSession session)
  {
    if (session.getRemoteAddress () instanceof InetSocketAddress)
    {
      return ((InetSocketAddress)session.getRemoteAddress ()).getAddress ();
    } else
    {
      throw new Error ("Can't get host name for address type " + 
                       session.getRemoteAddress ().getClass ());
    }
  }

  private WriteFuture send (IoSession session, Message message)
  {
    return Federation.send (session, serverDomain, message);
  }
  
  // IoHandler
  
  public void sessionCreated (IoSession session)
    throws Exception
  {
    // zip
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
    
    FederationLink link = linkFor (session);
    
    if (link == null)
      handleMessage (session, message);
    else if (!link.isClosed ())
      link.handleMessage (message);
  }

  public void sessionClosed (IoSession session)
    throws Exception
  {
    // todo
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
    // todo
  }
}
