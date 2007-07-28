package org.avis.federation;

import java.util.Set;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import org.avis.router.Router;

import static org.avis.io.Net.addressesFor;
import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.internalError;
import static org.avis.logging.Log.shouldLog;

public class FederationListener implements IoHandler
{
  /**
   * todo
   * 
   * @param router
   * @param federationId
   * @param classMap
   * @param listenUris
   * @throws IOException 
   * @throws UnknownHostException 
   * @throws SocketException 
   */
  public FederationListener (Router router, String federationId,
                             FederationClassMap classMap, 
                             Set<EwafURI> listenUris)
    throws IOException
  {
    SocketAcceptorConfig acceptorConfig = new SocketAcceptorConfig ();
    
    acceptorConfig.setReuseAddress (true);
    acceptorConfig.setThreadModel (ThreadModel.MANUAL);
    
    DefaultIoFilterChainBuilder filterChainBuilder =
      acceptorConfig.getFilterChain ();

    filterChainBuilder.addLast
      ("codec", new ProtocolCodecFilter (FederationFrameCodec.INSTANCE));
    
    for (InetSocketAddress address : addressesFor (listenUris))
    {
      if (shouldLog (DIAGNOSTIC))
        diagnostic ("Federator listening on address: " + address, this);

      router.socketAcceptor ().bind (address, this, acceptorConfig);
    }
  }

  // IoHandler
  
  public void sessionOpened (IoSession session)
    throws Exception
  {
    // todo
  }

  public void sessionCreated (IoSession session)
    throws Exception
  {
    // zip
  }

  public void messageReceived (IoSession session, Object message)
    throws Exception
  {
    // todo
  }

  public void sessionClosed (IoSession session)
    throws Exception
  {
    // todo
  }
  
  public void exceptionCaught (IoSession session, Throwable cause)
    throws Exception
  {
    internalError ("Error in federator", this, cause);
  }

  public void messageSent (IoSession session, Object message)
    throws Exception
  {
    // zio
  }

  public void sessionIdle (IoSession session, IdleStatus status)
    throws Exception
  {
    // zip
  }
}
