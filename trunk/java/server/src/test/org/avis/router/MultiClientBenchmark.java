package org.avis.router;

import java.util.ArrayList;
import java.util.Random;

import java.net.InetSocketAddress;

import javax.swing.text.Element;

import org.avis.common.ElvinURI;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.NotifyEmit;

import static java.lang.Math.round;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Arrays.sort;

import static org.avis.logging.Log.info;

public class MultiClientBenchmark
{
  static final InetSocketAddress ELVIN_ADDRESS = 
    new InetSocketAddress ("127.0.0.1", 29170);

  static final int MESSAGE_COUNT = 10;
  static final int RUNS = 12;
  
  /* Must be multiple of 8 */
  static final int CLIENTS = 2 * 8;
  /* Average notification rate (ntfns / sec) */
  static final int NOTIFY_RATE = 2;
  /* Max payload body size */
  static final int MAX_PAYLOAD = 256;

  static final double NOTIFY_DELAY = 1000.0 / NOTIFY_RATE;

  public static void main (String [] args) 
    throws Exception
  {
    long times [] = new long [RUNS];
    
    for (int i = 0; i < RUNS; i++)
    {
      info ("Run " + i + "...", MultiClientBenchmark.class);
      
      long start = currentTimeMillis ();
      
      new MultiClientBenchmark ().run ();
      
      times [i] = currentTimeMillis () - start;
      
      info (format ("Done: %,d ms", times [i]), MultiClientBenchmark.class);
      
      sleep (2000);
    }
    
    sort (times);
    
    info 
      (format ("Done all: Average %,d ms, mean %,d ms, min %,d ms, max %,d ms", 
               avg (times), times [times.length / 2], 
               times [0], times [times.length - 1]), MultiClientBenchmark.class);
  }

  private static long avg (long [] times)
  {
    double avg = 0;
    
    for (long time : times)
      avg += (double)time / (double)times.length;
    
    return round (avg);
  }

  private Random rand;

  public MultiClientBenchmark ()
  {
    this.rand = new Random (42);
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
      client.sendMessages ();
    
    for (Client client : clients)
      client.waitSendMessages ();
    
    SimpleClient elvin = new SimpleClient (ELVIN_ADDRESS);
    elvin.connect ();
    elvin.send (new NotifyEmit ("Group", 0, "Quit", 1));
    elvin.close ();
    
    for (Client client : clients)
      client.drainToQuitMessage ();
    
    for (Client client : clients)
      client.waitQuit ();
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
  
  long randomisedDelay ()
  {
    return  (long)(NOTIFY_DELAY + (((random () * NOTIFY_DELAY) - (NOTIFY_DELAY / 2)) * 0.1));
  }

  protected double random ()
  {
    return rand.nextDouble ();
  }

  byte [] randomPayload ()
  {
    byte [] payload = new byte [(int)(random () * MAX_PAYLOAD)];

    for (int i = 0; i < payload.length; i++)
      payload [i] = (byte)((random () * 255) - 127);
    
    return payload;
  }

  public class Client 
  {
    protected int id;
    protected int [] groups;
    protected SimpleClient elvin;
    protected Thread runner;
    protected boolean quit;

    public Client (int id, int ...groups)
    {
      this.id = id;
      this.groups = groups;
      this.elvin = null;
    }

    public void drainToQuitMessage ()
    {
      runner = new Thread ("Client " + id)
      {
        public void run ()
        {
          try
          {
            drain ();
          } catch (Exception ex)
          {
            ex.printStackTrace ();
          }
        }
      };
      
      runner.start ();
    }

    public void waitQuit () 
      throws Exception
    {
      runner.join ();
      elvin.close ();
    }

    public void sendMessages () 
      throws Exception
    {
      runner = new Thread ("Client " + id)
      {
        public void run ()
        {
          open ();
          doSendMessages ();
          
          System.out.println ("Done send: " + id);
        }
      };
      
      runner.start ();
    }

    public void waitSendMessages () 
      throws InterruptedException
    {
      runner.join ();
    }

    protected void open () 
    {
      elvin = new SimpleClient ("Client " + id, ELVIN_ADDRESS);

      try
      {
        elvin.connect ();
        
        elvin.subscribe 
          ("To == " + id + " || " +
           "equals (Group, " + join (groups) + ") || Group == 0");
      } catch (Exception ex)
      {
        throw new RuntimeException (ex);
      }
    }
    
    protected void doSendMessages ()
    {
      int count = 0;
      
      while (count < MESSAGE_COUNT && !quit)
      {
        int groupIndex = (int)(random () * groups.length) - 1;
        int group = groupIndex == -1 ? 0 : groups [groupIndex];

        try
        {
          elvin.send
            (new NotifyEmit 
              ("From", id, "Group", group, "Payload", randomPayload ()));
          
        } catch (Exception ex)
        {
          ex.printStackTrace ();
          
          quit = true;
        }

        elvin.drain ();
        
        count++;        
      }      
    }

    private void drain () 
      throws InterruptedException
    {
      Message message;
      
      while (!quit && (message = elvin.incomingMessages.take ()) != null)
      {
        if (message instanceof ErrorMessage)
          System.err.println ("client " + id + " message = " + message);
        
        if (message instanceof NotifyDeliver)
        {
          NotifyDeliver notify = (NotifyDeliver)message;
          
          if (notify.attributes.containsKey ("Quit"))
          {
            System.out.println ("Quit! " + id);
            quit = true;
          }
        }
      }
    }
  }
}
