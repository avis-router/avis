package org.avis.client;

import java.util.Map;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.avis.security.Keys;

import static org.avis.client.ConnectionOptions.EMPTY_OPTIONS;
import static org.avis.util.Streams.close;
import static org.avis.util.Util.checkNotNull;

/**
 * Options for configuring an Elvin client connection. Includes router
 * connection parameters, security keys, SSL parameters and timeouts.
 * 
 * The options object used to initialise the Elvin connection cannot
 * be directly changed after the connection is created, but some
 * settings can be changed on a live connection object using supported
 * methods such as {@link Elvin#setNotificationKeys(Keys)}, etc.
 * 
 * @author Matthew Phillips
 */
public final class ElvinOptions implements Cloneable
{
  /**
   * The options sent to the router to negotiate connection
   * parameters. After connection, these will be updated to include
   * any extra values sent by the router, including the
   * Vendor-Identification option.
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
   * The keystore used for TLS/SSL secure connections (i.e.
   * connections via "elvin:/secure/..." URI's). This may be null to
   * use the default JVM keystore. If it is set, the
   * keystorePassphrase option must also be set.
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
   * Used to ensure that the router the client is connected to is
   * authentic. When true, only servers with a certificate
   * authenticated against the trusted certificates in the supplied
   * keystore or the JVM's CA certificates will be acceptable for
   * secure connections. See the <a
   * href="http://avis.sourceforge.net/tls.html">documentation at the
   * Avis web site</a> and <a
   * href="http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#X509TrustManager">
   * the description of JSSE's X509TrustManager</a> for more
   * information.
   * 
   * @see #keystore
   */
  public boolean requireAuthenticatedServer;
  
  /**
   * The amount of time (in milliseconds) that must pass before the
   * router is assumed to not be responding to a request. Default is
   * 10 seconds.
   *
   * @see Elvin#setReceiveTimeout(long)
   */
  public long receiveTimeout;

  /**
   * The liveness timeout period (in milliseconds). If no messages are
   * seen from the router in this period a connection test message is
   * sent and if no reply is seen the connection is deemed to be
   * defunct and automatically closed. Default is 60 seconds.
   * 
   * @see Elvin#setLivenessTimeout(long)
   */
  public long livenessTimeout;

  public ElvinOptions ()
  {
    this (new ConnectionOptions (), new Keys (), new Keys ());
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
    this.requireAuthenticatedServer = false;
    this.receiveTimeout = 10000;
    this.livenessTimeout = 60000;
  }
  
  /**
   * Update connection options to include any new values from a given map.
   */
  protected void updateConnectionOptions (Map<String, Object> options)
  {
    if (options.size () > 0)
    {
      if (connectionOptions == EMPTY_OPTIONS)
        connectionOptions = new ConnectionOptions ();
      
      connectionOptions.setAll (options);
    }
  }

  /**
   * Create a copy of this option set. This does not deep clone the
   * keys and connection options, since these should be treated as
   * immutable after first use.
   */
  @Override
  public ElvinOptions clone ()
  {
    try
    {
      return (ElvinOptions)super.clone ();
    } catch (CloneNotSupportedException ex)
    {
      throw new Error (ex);
    }
  }

  /**
   * Shortcut to load a keystore from a <a
   * href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/windows/keytool.html">Java
   * keystore file</a>.
   * 
   * @param keystorePath The file path for the keystore.
   * @param passphrase The passphrase for the keystore.
   * 
   * @throws IOException if an error occurred while loading the
   *                 keystore.
   * 
   * @see #setKeystore(URL, String)
   */
  public void setKeystore (String keystorePath, String passphrase)
    throws IOException 
  {
    setKeystore (new File (keystorePath).toURI ().toURL (), passphrase);
  }
  
  /**
   * Shortcut to load a keystore from a <a
   * href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/windows/keytool.html">Java
   * keystore file</a>.
   * 
   * @param keystoreUrl The URL for the keystore file.
   * @param passphrase The passphrase for the keystore.
   * 
   * @throws IOException if an error occurred while loading the
   *                 keystore.
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