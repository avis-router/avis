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
 * See the documentation in "doc/examples.txt" for instructions on how to
 * use the Avis examples.
 */
public class HelloWorld
{
  public static void main (String [] args)
    throws Exception
  {
    String elvinUri = System.getProperty ("elvin", "elvin://localhost");

    // create a client that listens for messages with a "Greeting" field
    Elvin listeningClient = new Elvin (elvinUri);
    
    final Subscription greetingSubscription =
      listeningClient.subscribe ("require (Greeting)");
    
    greetingSubscription.addListener (new NotificationListener ()
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
    Elvin sendingClient = new Elvin (elvinUri);
    
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
