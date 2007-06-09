package org.avis.client.examples;

import org.avis.client.Elvin;
import org.avis.client.Notification;
import org.avis.client.NotificationEvent;
import org.avis.client.NotificationListener;
import org.avis.client.Subscription;

/*
 * This example demonstrates two Elvin clients, one sending the
 * traditional "Hello World" message as a greeting, the other one
 * listening for all greetings and printing them to the console.
 * 
 * It also demonstrates an easy way to have one thread wait for
 * another to receive a message. In this example, we use the
 * subscription object to wait on the receipt of a greeting message.
 * 
 * Running The Example
 * 
 * You will need an Elvin router -- either install the Avis router (or
 * other compatible router) locally, or use the public Elvin router at
 * public.elvin.org. To run the example from the console:
 * 
 *   ant example-helloworld
 * 
 * or, if you aren't using a local router (elvin://localhost),
 *   
 *   ant example-helloworld -Delvin=elvin://public.elvin.org
 */
/**
 * The HelloWorld example. See source code for more information.
 */
public class HelloWorld
{
  public static void main (String [] args)
    throws Exception
  {
    // read command line options
    ExampleOptions options = new ExampleOptions ("hello");
    
    options.parseOrExit (args);
    
    // create a client that listens for messages with a "Greeting" field
    Elvin listeningClient = new Elvin (options.elvinUri);
    
    final Subscription greetingSubscription =
      listeningClient.subscribe ("require (Greeting)");
    
    greetingSubscription.addNotificationListener (new NotificationListener ()
    {
      public void notificationReceived (NotificationEvent e)
      {
        // show the greeting
        System.out.println
          ("Received greeting: " + e.notification.get ("Greeting"));
        
        // notify the waiting main thread that we got the message
        synchronized (greetingSubscription)
        {
          greetingSubscription.notify ();
        }
      }
    });
    
    // create a client that sends a greeting
    Elvin sendingClient = new Elvin (options.elvinUri);
    
    Notification greeting = new Notification ();
    
    greeting.set ("Greeting", "Hello World!");    
    
    synchronized (greetingSubscription)
    {
      // send greeting...
      sendingClient.send (greeting);
      
      // ... and wait for the listener to let us know it came through
      greetingSubscription.wait ();
    }
    
    listeningClient.close ();
    sendingClient.close ();
  }
}
