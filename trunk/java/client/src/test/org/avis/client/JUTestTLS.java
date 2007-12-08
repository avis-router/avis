package org.avis.client;

import java.util.ArrayList;

import java.io.Closeable;
import java.io.IOException;

import java.net.URL;

import org.avis.logging.Log;
import org.avis.router.Router;
import org.avis.router.RouterOptions;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;

/*
 * Command to generate the test key store:

    keytool -genkey -alias test -keysize 512 -validity 3650 -keyalg RSA \
      -dname "CN=test.com, OU=test, O=Test Inc, L=Adelaide, \
      S=South Australia, C=AU" -keypass testing -storepass testing \
      -keystore client.ks
  
   Commands to export server/client certs and import into other keystore 
   to enable client/server auth:
   
     keytool -keystore client.ks -alias test -storepass testing \
       -file client.cer

     keytool -keystore router.ks -alias test -storepass testing \
       -file router.cer

     keytool -import -keystore router.ks -alias client -storepass testing \
       -file client.cer
        
     keytool -import -keystore client.ks -alias router -storepass testing \
       -file router.cer 
 */
public class JUTestTLS
{
  private static final String SECURE_URI = "elvin:/secure/127.0.0.1:29170";
  
  private ArrayList<Closeable> autoClose;
  private LogFailTester logTester;
  
  /**
   * Test a basic client/server connect over TLS.
   */
  @Test
  public void connect () 
    throws Exception
  {
    URL clientKeystoreUrl = getClass ().getResource ("client.ks");
    URL routerKeystoreUrl = getClass ().getResource ("router.ks");

    RouterOptions routerOptions = new RouterOptions ();
    
    routerOptions.set ("Listen", SECURE_URI);
    routerOptions.set ("TLS.Keystore", routerKeystoreUrl);
    routerOptions.set ("TLS.Keystore-Passphrase", "testing");
    
    Router router = new Router (routerOptions);
    
    ElvinOptions options = new ElvinOptions ();
    
    options.setKeystore (clientKeystoreUrl, "testing");
    
    Elvin elvin = new Elvin (SECURE_URI, options);
    
    /*
     * todo: when no gap, we occasionally get an SSL exception which
     * seems to be indicating SSL filter is being used after it has
     * been disposed.
     */
    sleep (1000);
    
    elvin.close ();
    router.close ();
  }
  
  /**
   * Test a client/server connect using required authorization over TLS.
   */
  @Test
  public void connectAuthRequired () 
    throws Exception
  {
    // todo test failure when server cert is missing
    
    URL clientKeystoreUrl = getClass ().getResource ("client.ks");
    URL routerKeystoreUrl = getClass ().getResource ("router.ks");

    RouterOptions routerOptions = new RouterOptions ();
    
    routerOptions.set ("Listen", SECURE_URI);
    routerOptions.set ("TLS.Keystore", routerKeystoreUrl);
    routerOptions.set ("TLS.Keystore-Passphrase", "testing");
    routerOptions.set ("TLS.Require-Trusted-Client", true);
    
    Router router = new Router (routerOptions);
   
    autoClose (router);
    
    ElvinOptions options = new ElvinOptions ();
    
    options.setKeystore (clientKeystoreUrl, "testing");
    options.requireTrustedServer = true;
    
    Elvin elvin = new Elvin (SECURE_URI, options);
    
    elvin.close ();
    router.close ();
  }
  
  private void autoClose (Closeable thing)
  {
    autoClose.add (thing);
  }
  
  @Before
  public void init ()
  {
    autoClose = new ArrayList<Closeable> ();
  }
  
  @After
  public void close ()
  {
    for (Closeable thing : autoClose)
    {
      try
      {
        thing.close ();
      } catch (IOException ex)
      {
        ex.printStackTrace ();
      }
    }
  }
  
  @Before
  public void logBefore ()
  {
    logTester = new LogFailTester ();
    
    Log.enableLogging (Log.TRACE, false);
  }
  
  @After
  public void logAfter ()
  {
    logTester.assertOkAndDispose ();
  }
}
