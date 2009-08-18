package org.avis.router;

import java.io.Closeable;
import java.io.IOException;

import java.net.InetAddress;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.common.ElvinURI;
import org.avis.io.ClientFrameCodec;
import org.avis.io.messages.ConnRply;
import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.Message;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubRply;
import org.avis.logging.Log;
import org.avis.util.Filter;

import static java.util.Collections.singleton;

import static org.avis.io.Net.idFor;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.info;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.trace;

/**
 * A deliberately broken router for testing clients.
 * 
 * @author Matthew Phillips
 */
public class BrokenRouter 
  extends IoHandlerAdapter implements IoHandler, Closeable
{
  public static void main (String [] args) 
    throws IOException
  {
    Log.enableLogging (Log.DIAGNOSTIC, true);
    Log.enableLogging (Log.TRACE, true);
    
    new BrokenRouter (System.getProperty ("avis.uri", "elvin://0.0.0.0"), 
                      true, true);
  }

  private IoManager ioManager;
  private long subscriptionId = 1;
  private boolean subAddEnabled;
  private boolean connectionEnabled;
  
  @SuppressWarnings ("unchecked")
  public BrokenRouter (String uri, boolean connectionEnabled,
                       boolean subAddEnabled) 
    throws IOException
  {
    this.connectionEnabled = connectionEnabled;
    this.subAddEnabled = subAddEnabled;
    this.ioManager = new IoManager (null, null, 2 * 1024 * 1024, false); 
    
    /*
     * Setup IO filter chain. NOTE re thread pool: we do this in order
     * to install the IO event queue throttle to handle spammy
     * clients. It's not clear that we gain any other benefit from it
     * since Avis notification processing is non-blocking. See
     * http://mina.apache.org/configuring-thread-model.html.
     */
    DefaultIoFilterChainBuilder filters = new DefaultIoFilterChainBuilder ();

    filters.addLast ("codec", ClientFrameCodec.FILTER);
    
    ioManager.bind 
      (singleton (new ElvinURI (uri)), this, filters, 
       (Filter<InetAddress>)Filter.MATCH_NONE);
    
    info ("Listening on " + uri, this);
  }
  
  public void close () 
    throws IOException
  {
    ioManager.close ();
  }
  
  public void messageReceived (IoSession session, Object messageObject)
    throws Exception
  {
    if (shouldLog (TRACE))
    {
      trace ("Server got message from " + idFor (session) +
             ": " + messageObject, this);
    }
    
    Message message = (Message)messageObject;
  
    try
    {
      switch (message.typeId ())
      {
        case ConnRqst.ID:
          handleConnRqst (session, (ConnRqst)message);
          break;
        case SubAddRqst.ID:
          handleSubAddRqst (session, (SubAddRqst)message);
          break;
        default:
          diagnostic ("Ignoring message type: " + message, this);
      }
    } catch (ProtocolCodecException ex)
    {
      session.close (true);
    }
  }
  
  private void handleConnRqst (IoSession session, ConnRqst message)
    throws ProtocolCodecException
  {
    if (connectionEnabled)
      send (session, new ConnRply (message, message.options));
  }
  
  private void handleSubAddRqst (IoSession session, SubAddRqst message)
    throws NoConnectionException
  {
    if (subAddEnabled)
      send (session, new SubRply (message, subscriptionId ++));
  }
  
  private static void send (IoSession session, Message message)
  {    
    if (shouldLog (TRACE))
    {
      trace ("Server sent message to " + idFor (session) + ": " + message,
             Router.class);
    }
    
    session.write (message);
  }
}
