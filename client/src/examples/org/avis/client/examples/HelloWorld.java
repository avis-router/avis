package org.avis.client.examples;

import org.avis.client.Elvin;
import org.avis.client.Notification;
import org.avis.client.NotificationEvent;
import org.avis.client.NotificationListener;
import org.avis.client.Subscription;

public class HelloWorld
{
  public static void main (String [] args)
    throws Exception
  {
    // read command line options
    ExampleOptions options = new ExampleOptions ("hello");
    
    options.parseOrExit (args);
    
    // create a client to listen for messages with a Greeting field
    Elvin listeningClient = new Elvin (options.elvinUri);
    
    final Subscription greetingSubscription =
      listeningClient.subscribe ("require (Greeting)");
    
    greetingSubscription.addNotificationListener (new NotificationListener ()
    {
      public void notificationReceived (NotificationEvent e)
      {
        // let others into the greeting
        System.out.println ("Greeting: " + e.notification.get ("Greeting"));
        
        // notify the main thread that we got the message
        synchronized (greetingSubscription)
        {
          greetingSubscription.notify ();
        }
      }
    });
    
    // create a client to send a greeting
    Elvin sendingClient = new Elvin (options.elvinUri);
    
    Notification greeting = new Notification ();
    greeting.set ("Greeting", "Hello World!");    
    
    synchronized (greetingSubscription)
    {
      // send greeting...
      sendingClient.send (greeting);
      
      // ... and wait for subscription to let us know it came through
      greetingSubscription.wait ();
    }
    
    listeningClient.close ();
    sendingClient.close ();
  }
}
