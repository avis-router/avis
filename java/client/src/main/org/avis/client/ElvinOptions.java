package org.avis.client;

import java.io.File;
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
 * Options for controlling an Elvin client connection. The options
 * object used to initialise the Elvin connection cannot be changed
 * directly after the connection is established, but some can be
 * changed on a live connection using methods on the connection
 * itself, e.g. {@link Elvin#setNotificationKeys(Keys)}.
 * 
 * @author Matthew Phillips
 */
public final class ElvinOptions implements Cloneable
{
  /**
   * The options sent to the router to negotiate connection parameters.
   */
  public ConnectionOptions connectionOptions;
  
  /** 
   * The global notification keys that apply to all notifications. 
   */
  public Keys notificationKeys;
  
  /** 
   * The global subscription keys that apply to all subscriptions.
   */
  public Keys subscriptionKeys;
  
  /**
   * The keystore used for TLS/SSL secure connections. This may be
   * null to use the default JVM TLS certificate chain. If it is set,
   * the keystorePassphrase option must also be set.
   * 
   * @see #setKeystore(URL, String)
   */
  public KeyStore keystore;
  
  /**
   * The passphrase used to secure the keystore and its keys.
   * 
   * @see #keystore
   */
  public String keystorePassphrase;

  /**
   * When true, only servers with a certificate matching the trusted
   * certificates in the supplied keystore or the JVM's CA
   * certificates will be acceptable for secure connections. See the
   * documentation for <a
   * href="http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#X509TrustManager">
   * JSSE's X509TrustManager</a> for more information.
   * 
   * @see #keystore
   */
  public boolean requireTrustedServer;
  
  /**
   * The amount of time (in milliseconds) that must pass before the
   * router is assumed to not be responding to a request. Default is
   * 10 seconds.
   *
   * @see Elvin#setReceiveTimeout(long)
   */
  public long receiveTimeout;

  /**
   * Set the liveness timeout period (in milliseconds). If no messages
   * are seen from the router in this period, a connection test
   * message is sent and, if no reply is seen, the connection is
   * deemed to be closed. Default is 60 seconds.
   *
   * @see Elvin#setLivenessTimeout(long)
   */
  public long livenessTimeout;

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
    this.requireTrustedServer = false;
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
   * @param keystorePath The file path for the keystore.
   * @param passphrase The passphrase for the keystore.
   * 
   * @throws IOException if an error occurred while loading the keystore.
   *
   * @see #setKeystore(URL, String)
   */
  public void setKeystore (String keystorePath, String passphrase)
    throws IOException 
  {
    setKeystore (new File (keystorePath).toURL (), passphrase);
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
