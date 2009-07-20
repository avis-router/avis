package org.avis.router;

import java.util.ArrayList;

import java.io.IOException;

import java.net.InetSocketAddress;

import org.avis.io.messages.NotifyEmit;

import static java.lang.Thread.currentThread;

import static java.lang.Math.random;

import static java.lang.Thread.sleep;

public class ActiveRouterTester
{
  static final InetSocketAddress ELVIN_ADDRESS = 
    new InetSocketAddress ("127.0.0.1", 29170);

  static final int NOTIFY_RATE = 3;
  static final double NOTIFY_DELAY = 1000.0 / NOTIFY_RATE;

  static final int MAX_PAYLOAD = 20 * 1024;
  
  public static void main (String [] args) 
    throws Exception
  {
    new ActiveRouterTester ().run ();
  }

  private void run () 
    throws Exception
  {
    ArrayList<Client> clients = new ArrayList<Client> (8);
    
    clients.add (new Client (1, 7, 5, 6));
    clients.add (new Client (2, 7, 6, 5));
    clients.add (new Client (3, 6, 5, 4));
    clients.add (new Client (4, 4, 5));
    clients.add (new Client (5, 1, 4, 3));
    clients.add (new Client (6, 2, 1, 3));
    clients.add (new Client (7, 2, 1, 3));
    clients.add (new Client (8, 2, 3, 6));
    
    for (Client client : clients)
      client.start ();
    
    sleep (30000);
    
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
      this.elvin = new SimpleClient ("client " + id, ELVIN_ADDRESS);      
    }

    public void start () 
      throws Exception
    {
      elvin.connect ();
      elvin.subscribe 
        ("To == " + id + " || " +
         "equals (Group, " + join (groups) + ") || Group == 0");
      
      runner = new Thread (this, "Client " + id);
      runner.start ();
    }
    
    public void run ()
    {
      try
      {
        while (!currentThread ().isInterrupted ())
        {
          sleep (randomisedDelay ());

          int groupIndex = (int)(random () * groups.length) - 1;
          int group = groupIndex == -1 ? 0 : groups [groupIndex];

          byte [] payload = randomPayload ();
          
//          System.out.println ("Client " + id + " message to " + group + 
//                              " payload " + payload.length);
//          
          elvin.send (new NotifyEmit ("From", id, "Group", group, "Payload",
                                      payload));
        }
      } catch (InterruptedException ex)
      {
        currentThread ().interrupt ();
      } catch (NoConnectionException ex)
      {
        // exit
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
