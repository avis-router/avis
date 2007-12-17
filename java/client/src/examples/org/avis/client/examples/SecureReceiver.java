package org.avis.client.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.avis.client.Elvin;
import org.avis.client.GeneralNotificationEvent;
import org.avis.client.GeneralNotificationListener;
import org.avis.security.Key;
import org.avis.security.Keys;

import static org.avis.client.SecureMode.REQUIRE_SECURE_DELIVERY;
import static org.avis.client.examples.ExampleOptions.USAGE;
import static org.avis.security.KeyScheme.SHA1_CONSUMER;
import static org.avis.util.CommandLineOptions.handleError;

/*
 * This is the receiver part of the security example. In this example
 * a sender uses the Elvin security facility to send messages only to
 * trusted receivers.
 * 
 * See the documentation in "doc/examples.txt" for instructions on how
 * to use the Avis examples.
 */
public class SecureReceiver
{
  public static void main (String [] args)
    throws Exception
  {
    try
    {
      run (new ExampleOptions (args));
    } catch (Exception ex)
    {
      handleError ("secure-receiver", USAGE, ex);
    }
  }
  
  private static void run (ExampleOptions options)
    throws IOException
  {
    Elvin elvin = new Elvin (options.elvinUri);
    
    elvin.closeOnExit ();
    
    // read the password from the console
    
    BufferedReader stdin = 
      new BufferedReader (new InputStreamReader (System.in));

    System.out.println ("Enter the password for receiving: ");
    String password = stdin.readLine ();

    /*
     * Create the key and add it as a private key in the SHA-1
     * consumer scheme. The secure sender will do the same, but with
     * the public version of the key. See SecureSender for more
     * details.
     */
    Key privateKey = new Key (password);
    Keys keys = new Keys ();
    keys.add (SHA1_CONSUMER, privateKey);
    
    elvin.subscribe ("From == 'secure-sender' && string (Message)",
                     keys, REQUIRE_SECURE_DELIVERY);
    
    elvin.addNotificationListener (new GeneralNotificationListener ()
    {
      public void notificationReceived (GeneralNotificationEvent e)
      {
        System.out.println ("Received message: " +
                            e.notification.get ("Message"));
      }
    });
    
    System.out.println ("Listening for messages...");
  }
}
