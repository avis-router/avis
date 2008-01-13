package org.avis.client;

import java.util.ArrayList;

import java.io.Closeable;
import java.io.IOException;

import java.net.URL;

import javax.net.ssl.SSLException;

import org.avis.logging.Log;
import org.avis.router.Router;
import org.avis.router.RouterOptions;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/*
 * Command to generate the test key store:

    keytool -genkey -alias test -keysize 512 -validity 3650 -keyalg RSA \
      -dname "CN=test.com, OU=test, O=Test Inc, L=Adelaide, \
      S=South Australia, C=AU" -keypass testing -storepass testing \
      -keystore client.ks
  
   Commands to export server/client certs and import into other keystore 
   to enable client/server auth:
   
     keytool -export -keystore client.ks -alias test -storepass testing \
       -file client.cer

     keytool -export -keystore router.ks -alias test -storepass testing \
       -file router.cer

     keytool -import -keystore router.ks -alias client -storepass testing \
       -file client.cer
        
     keytool -import -keystore client.ks -alias router -storepass testing \
       -file router.cer 
 */
public class JUTestClientTLS
{
  private static final String SECURE_URI = "elvin:/secure/127.0.0.1:29170";
  
  private ArrayList<Closeable> autoClose;
  private LogFailTester logTester;
  
  /**
   * Test a basic client/server connect over TLS with client default
   * keystore.
   */
  @Test
  public void connect () 
    throws Exception
  {
    URL routerKeystoreUrl = getClass ().getResource ("router.ks");

    RouterOptions routerOptions = new RouterOptions ();
    
    routerOptions.set ("Listen", SECURE_URI);
    routerOptions.set ("TLS.Keystore", routerKeystoreUrl);
    routerOptions.set ("TLS.Keystore-Passphrase", "testing");
    
    Router router = new Router (routerOptions);
    
    autoClose (router);
    
    Elvin elvin = new Elvin (SECURE_URI);
    
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
    URL clientKeystoreUrl = getClass ().getResource ("client.ks");
    URL routerKeystoreUrl = getClass ().getResource ("router.ks");

    RouterOptions routerOptions = new RouterOptions ();
    
    routerOptions.set ("Listen", SECURE_URI);
    routerOptions.set ("Require-Authenticated", "*");
    routerOptions.set ("TLS.Keystore", routerKeystoreUrl);
    routerOptions.set ("TLS.Keystore-Passphrase", "testing");
    
    Router router = new Router (routerOptions);
   
    autoClose (router);
    
    ElvinOptions options = new ElvinOptions ();
    
    options.setKeystore (clientKeystoreUrl, "testing");
    options.requireAuthenticatedServer = true;
    
    Elvin elvin = new Elvin (SECURE_URI, options);
    
    elvin.close ();
    router.close ();
  }
  
  /**
   * Test a client/server connect fails when using required
   * authorization and no trusted server/client is available.
   */
  @Test
  public void connectAuthRequiredNoCert () 
    throws Exception
  {
    URL clientKeystoreUrl = getClass ().getResource ("client_no_router_cert.ks");
    URL routerKeystoreUrl = getClass ().getResource ("router.ks");

    RouterOptions routerOptions = new RouterOptions ();
    
    routerOptions.set ("Listen", SECURE_URI);
    routerOptions.set ("Require-Authenticated", "*");
    routerOptions.set ("TLS.Keystore", routerKeystoreUrl);
    routerOptions.set ("TLS.Keystore-Passphrase", "testing");
    
    Router router = new Router (routerOptions);
   
    autoClose (router);
    
    ElvinOptions options = new ElvinOptions ();
    
    options.setKeystore (clientKeystoreUrl, "testing");
    options.requireAuthenticatedServer = true;
    
    try
    {
      Elvin elvin = new Elvin (SECURE_URI, options);

      elvin.close ();
      
      fail ();
    } catch (SSLException ex)
    {
      // ok
    }
    
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
    
    // Log.enableLogging (Log.INFO, true);
    // Log.enableLogging (Log.TRACE, true);
    // Log.enableLogging (Log.DIAGNOSTIC, true);
  }
  
  @After
  public void logAfter () throws InterruptedException
  {
    logTester.assertOkAndDispose ();
  }
}
