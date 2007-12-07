package org.avis.client;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.avis.security.Keys;

import static org.avis.client.ConnectionOptions.EMPTY_OPTIONS;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.util.Streams.close;
import static org.avis.util.Util.checkNotNull;

/**
 * Options for controlling an Elvin client connection.
 * 
 * @author Matthew Phillips
 */
public class ElvinOptions implements Cloneable
{
  /**
   * The options sent to the router to negotiate connection parameters.
   */
  public ConnectionOptions connectionOptions;
  
  /** The global notification keys that apply to all notifications. */
  public Keys notificationKeys;
  
  /** The global subscription keys that apply to all subscriptions. */
  public Keys subscriptionKeys;
  
  /**
   * The keystore used for TLS/SSL secure connections. The
   * keystorePassphrase option must also be set.
   */
  public KeyStore keystore;
  
  /**
   * The passphrase used to secure the keystore and its keys.
   */
  public String keystorePassphrase;

  /**
   * The amount of time (in milliseconds) that must pass before the
   * router is assumed to not be responding to a request.
   */
  public int receiveTimeout;

  /**
   * Set the liveness timeout period (default is 60 seconds). If no
   * messages are seen from the router in this period, a connection
   * test message is sent and, if no reply is seen, the connection is
   * deemed to be closed.
   */
  public int livenessTimeout;

  public ElvinOptions ()
  {
    this (EMPTY_OPTIONS, EMPTY_KEYS, EMPTY_KEYS);
  }

  public ElvinOptions (ConnectionOptions connectionOptions,
                       Keys notificationKeys, 
                       Keys subscriptionKeys)
  {
    checkNotNull (notificationKeys, "Notification keys");
    checkNotNull (subscriptionKeys, "Subscription keys");
    checkNotNull (connectionOptions, "Connection options");
  
    this.connectionOptions = connectionOptions;
    this.notificationKeys = notificationKeys;
    this.subscriptionKeys = subscriptionKeys;
    this.receiveTimeout = 10000;
    this.livenessTimeout = 60000;
  }

  @Override
  public ElvinOptions clone ()
  {
    try
    {
      ElvinOptions copy = (ElvinOptions)super.clone ();
      
      return copy;
    } catch (CloneNotSupportedException ex)
    {
      throw new Error (ex);
    }
  }
  
  /**
   * Shortcut to load a keystore from a Java keystore file.
   * 
   * @param keystoreUrl The URL for the keystore file.
   * @param passphrase The passphrase for the keystore.
   * 
   * @throws IOException if an error occurred while loading the keystore.
   */
  public void setKeystore (URL keystoreUrl, String passphrase)
    throws IOException 
  {
    InputStream keystoreStream = keystoreUrl.openStream ();

    try
    {
      KeyStore newKeystore = KeyStore.getInstance ("JKS");
      
      newKeystore.load (keystoreStream, passphrase.toCharArray ());
      
      keystore = newKeystore;
      keystorePassphrase = passphrase;
    } catch (GeneralSecurityException ex)
    {
      throw new IOException ("Error opening keystore: " + ex);
    } finally
    {
      close (keystoreStream);
    }
  }
}
