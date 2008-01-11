package org.avis.client.examples;

import java.io.IOException;

import org.avis.client.Elvin;
import org.avis.client.Notification;
import org.avis.client.NotificationEvent;
import org.avis.client.NotificationListener;
import org.avis.client.Subscription;

import static org.avis.client.examples.ExampleOptions.USAGE;
import static org.avis.util.CommandLineOptions.handleError;

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
    try
    {
      // todo simplify: just have a method that extracts the Elvin URI?
      run (new ExampleOptions (args));
    } catch (Exception ex)
    {
      handleError ("hello", USAGE, ex);
    }
  }
  
  private static void run (ExampleOptions options) 
    throws IOException, InterruptedException
  {
    // create a client that listens for messages with a "Greeting" field
    Elvin listeningClient = new Elvin (options.elvinUri);
    
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
