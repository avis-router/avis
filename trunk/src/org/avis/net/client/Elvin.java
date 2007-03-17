package org.avis.net.client;

import java.io.IOException;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.net.FrameCodec;

import static dsto.dfc.logging.Log.alarm;

import static org.avis.net.client.ElvinURI.defaultProtocol;

public class Elvin
{
  private ElvinURI elvinUri;
  private IoSession clientSession;

  public Elvin (String elvinUri)
    throws URISyntaxException, IllegalArgumentException,
           ConnectException, IOException
  {
    this (new ElvinURI (elvinUri));
  }
  
  public Elvin (ElvinURI elvinUri)
    throws IllegalArgumentException, ConnectException, IOException
  {
    this.elvinUri = elvinUri;
    
    if (!elvinUri.protocol.equals (defaultProtocol ()))
      throw new IllegalArgumentException
        ("Only the default protocol stack (" +
         defaultProtocol () + ") is currently supported");
    
    connect ();
  }

  private void connect ()
    throws IOException
  {
    try
    {
      SocketConnector connector = new SocketConnector ();

      /* Change the worker timeout to 1 second to make the I/O thread
       * quit soon when there's no connection to manage. */
      connector.setWorkerTimeout (1);
      
      SocketConnectorConfig cfg = new SocketConnectorConfig ();
      cfg.setConnectTimeout (10);
      
      DemuxingProtocolCodecFactory codecFactory =
        new DemuxingProtocolCodecFactory ();
      codecFactory.register (FrameCodec.class);
      
      cfg.getFilterChain ().addLast
        ("codec", new ProtocolCodecFilter (codecFactory));
      
      ConnectFuture future =
        connector.connect
          (new InetSocketAddress (elvinUri.host, elvinUri.port),
           new MessageHandler (), cfg);
                                       
      future.join ();
      clientSession = future.getSession ();
    } catch (RuntimeIOException ex)
    {
      // unwrap MINA's RuntimeIOException
      throw (IOException)ex.getCause ();
    }
  }
  
  public void close ()
  {
    clientSession.close ().join ();
  }
  
  class MessageHandler extends IoHandlerAdapter
  {
    public void exceptionCaught (IoSession session, Throwable cause)
      throws Exception
    {
      alarm ("Unexpected exception", this, cause);
    }

    public void messageReceived (IoSession session, Object message)
      throws Exception
    {
      // todo
    }
  }
}
