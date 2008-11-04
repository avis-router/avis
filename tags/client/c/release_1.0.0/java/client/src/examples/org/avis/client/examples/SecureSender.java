package org.avis.client.examples;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.avis.client.Elvin;
import org.avis.client.Notification;
import org.avis.security.Key;
import org.avis.security.Keys;

import static org.avis.client.SecureMode.REQUIRE_SECURE_DELIVERY;
import static org.avis.security.KeyScheme.SHA1_CONSUMER;

/*
 * This is the sender part of the security example. In this example a
 * sender uses the Elvin security facility to send messages only to
 * trusted receivers.
 * 
 * See the documentation in "doc/examples.txt" for instructions on how
 * to use the Avis examples.
 */
public class SecureSender
{
  public static void main (String [] args)
    throws Exception
  {
    Elvin elvin = 
      new Elvin (System.getProperty ("elvin", "elvin://localhost"));
    
    elvin.closeOnExit ();
    
    // read the password and message text from the console
    
    BufferedReader stdin = 
      new BufferedReader (new InputStreamReader (System.in));

    System.out.println ("Enter the password for sending: ");
    String password = stdin.readLine ();
    
    System.out.println ("Enter the text to send: ");
    String message = stdin.readLine ();
    
    // create the notification
    
    Notification secretMessage = new Notification ();
    secretMessage.set ("From", "secure-sender");
    secretMessage.set ("Message", message);
    
    /*
     * To ensure only known consumers can receive this message, we use
     * a consumer key scheme, where the producer (this class) uses a
     * public key for its messages and consumers can receive only if
     * they know the private version of the key. It is effectively
     * impossible for untrusted consumers to work out the private key
     * even if they know its public half due to the use of a secure
     * one-way hash such as SHA-1.
     */
    Key privateKey = new Key (password);
    Keys sendingKeys = new Keys ();
    
    // add the public key
    sendingKeys.add (SHA1_CONSUMER, privateKey.publicKeyFor (SHA1_CONSUMER));
    
    // send message, requiring it be delivered securely
    elvin.send (secretMessage, sendingKeys, REQUIRE_SECURE_DELIVERY);
    
    System.out.println ("Message sent!");
    
    elvin.close ();
  }
}
