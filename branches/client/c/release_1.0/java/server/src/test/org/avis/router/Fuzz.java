package org.avis.router;

import java.util.Random;

import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import static java.lang.Thread.sleep;

/**
 * Fuzz tests an Elvin router by writing message frames with random payloads.
 * 
 * @author Matthew Phillips
 */
public class Fuzz
{
  final static int [] MESSAGES = new int []
    {32, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
     64, 80, 81, 82, 83};
  
  public static void main (String [] args)
    throws Exception
  {
    new Fuzz ((args.length > 0 ? args [0] : "127.0.0.1")).run ();
  }

  private NioSocketConnector connector;
  private InetSocketAddress remoteAddress;
  private Random random;
  
  public Fuzz (String host)
  {
    // connector
    connector = new NioSocketConnector (1);
    
    remoteAddress = new InetSocketAddress (host, 2917);
    
    random = new Random (hashCode ());
  }

  public void run ()
    throws Exception
  {
    while (true)
    {
      IoSession session = connect ();
     
      IoBuffer buffer = IoBuffer.allocate (128 * 1024);
      
      int bytes = random.nextInt (buffer.capacity () - 8);
      
      bytes = bytes - (bytes % 4);
      
      buffer.clear ();

      buffer.putInt (bytes + 4);
      buffer.putInt (MESSAGES [random.nextInt (MESSAGES.length)]);
      
      for (int i = bytes; i > 0; i--)
        buffer.put ((byte)(random.nextInt (256) - 127));
      
      buffer.flip ();
      session.write (buffer).addListener (IoFutureListener.CLOSE);
      
      System.out.println ("Wrote " + bytes + " bytes");
      
      sleep (10);
    }
  }
  
  private IoSession connect () 
    throws Exception
  {
    connector.setConnectTimeoutMillis (20000);
    connector.setHandler (new IoHandlerAdapter ());
    
    ConnectFuture future = connector.connect (remoteAddress);
    
    future.await ();
    
    return future.getSession ();
  }
}
