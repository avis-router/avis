package org.avis.router;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import java.io.IOException;

import java.net.InetSocketAddress;

import org.avis.io.messages.NotifyEmit;

import static java.lang.Math.random;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

/**
 * Stresses a router, especially its memory footprint when in "harsh"
 * mode. Generates clusters of clients communicating with sub-clusters
 * and occasionally in broadcast to all. Notifications vary in size.
 * 
 * Harsh mode is designed to crash Avis if no memory overload
 * protection is in place. It was developed to test whether crash
 * protection added in Avis 1.3 is effective.
 * @author Matthew Phillips
 */
public class ActiveRouterTester
{
  static final InetSocketAddress ELVIN_ADDRESS = 
    new InetSocketAddress ("127.0.0.1", 29170);

  /* Flip to true to generate enough traffic to kill an unprotected router. */
  static boolean HARSH = true;
  
  /* Must be multiple of 8 */
  static final int CLIENTS = HARSH ? 3 * 8 : 2 * 8;
  /* Average notification rate (ntfns / sec) */
  static final int NOTIFY_RATE = HARSH ? 9 : 2;
  /* Max payload body size */
  static final int MAX_PAYLOAD = HARSH ? 120 * 1024 : 2048;

  static final double NOTIFY_DELAY = 1000.0 / NOTIFY_RATE;

  protected final AtomicLong totalMessagesSent = new AtomicLong ();
  protected final AtomicLong totalMessagesReceived = new AtomicLong ();
  
  public static void main (String [] args) 
    throws Exception
  {
    new ActiveRouterTester ().run ();
  }

  private void run () 
    throws Exception
  {
    ArrayList<Client> clients = new ArrayList<Client> (CLIENTS);
    
    for (int i = CLIENTS; i > 0; i -= 8)
    {
      clients.add (new Client (1, 7, 5, 6));
      clients.add (new Client (2, 7, 6, 5));
      clients.add (new Client (3, 6, 5, 4));
      clients.add (new Client (4, 4, 5));
      clients.add (new Client (5, 1, 4, 3));
      clients.add (new Client (6, 2, 1, 3));
      clients.add (new Client (7, 2, 1, 3));
      clients.add (new Client (8, 2, 3, 6));
    }
    
    for (Client client : clients)
      client.start ();
    
    sleep (1000000);
    
    for (Client client : clients)
      client.stop ();
  }
 
  static String join (int [] numbers)
  {
    StringBuilder str = new StringBuilder ();
    boolean first = true;
      
    for (int number : numbers)
    {
      if (!first)
        str.append (", ");
      
      first = false;

      str.append (number);
    } 
    
    return str.toString ();
  }
  
  static long randomisedDelay ()
  {
    return  (long)(NOTIFY_DELAY + (((random () * NOTIFY_DELAY) - (NOTIFY_DELAY / 2)) * 0.1));
  }

  static byte [] randomPayload ()
  {
    byte [] payload = new byte [(int)(random () * MAX_PAYLOAD)];

    for (int i = 0; i < payload.length; i++)
      payload [i] = (byte)((random () * 255) - 127);
    
    return payload;
  }

  public class Client implements Runnable
  {
    private int id;
    private int [] groups;
    private SimpleClient elvin;
    private Thread runner;

    public Client (int id, int ...groups)
    {
      this.id = id;
      this.groups = groups;
      this.elvin = null;
    }

    public void start () 
      throws Exception
    {
      runner = new Thread (this, "Client " + id);
      runner.start ();
    }

    private void open () 
      throws InterruptedException 
    {
      // info ("Client " + id + " start open", this);
      
      try
      {
        if (elvin != null)
          elvin.close ();
      } catch (Throwable ex)
      {
        // zip
      }
      
      do
      {
        elvin = new SimpleClient ("client " + id, ELVIN_ADDRESS);

        try
        {
          elvin.connect ();
          
          // info ("Client " + id + " opened", this);
          
          elvin.subscribe 
            ("To == " + id + " || " +
             "equals (Group, " + join (groups) + ") || Group == 0");
        } catch (Throwable ex)
        {
          elvin.closeImmediately ();
          
          if (ex instanceof InterruptedException)
            throw (InterruptedException)ex;
        }
      } while (!elvin.connected ());
    }
    
    public void run ()
    {
      try
      {
        open ();
      } catch (InterruptedException ex1)
      {
        // zip
      }

      int count = 0;
      
      try
      {
        while (!currentThread ().isInterrupted () && elvin.connected ())
        {
          try
          {
            sleep (randomisedDelay ());
  
            int groupIndex = (int)(random () * groups.length) - 1;
            int group = groupIndex == -1 ? 0 : groups [groupIndex];
  
            byte [] payload = randomPayload ();
            
            elvin.send (new NotifyEmit ("From", id, "Group", group, "Payload",
                                        payload));
  
            int received = elvin.drain ();
            
            count++;
            long totalSent = totalMessagesSent.incrementAndGet ();
            long totalReceived = totalMessagesReceived.addAndGet (received);
            
            if (count % 100 == 0)
            {
              System.out.println ("Client " + id + " has sent " + 
                                  count + " messages");
            }
            
            if (totalSent % 100 == 0)
              System.out.println ("Total messages sent = " + totalSent);
            
            if (totalReceived / 100 != (totalReceived - received) / 100)
              System.out.println ("Total messages received = " + totalReceived);
            
          } catch (NoConnectionException ex)
          {
            open ();
          } catch (Throwable ex)
          {
            if (ex instanceof InterruptedException)
              throw (InterruptedException)ex;
            
            open ();
          }
        }
      } catch (InterruptedException ex)
      {
        currentThread ().interrupt ();
      } 
    }

    public void stop () 
      throws IOException, InterruptedException
    {
      runner.interrupt ();
      runner.join ();
      elvin.close ();
    }
  }
}