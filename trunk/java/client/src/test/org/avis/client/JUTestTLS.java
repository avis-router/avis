package org.avis.client;

import java.net.URL;

import org.avis.router.Router;
import org.avis.router.RouterOptions;

import org.junit.Test;

/*
 * Command to generate the test key store:
 *   keytool -genkey -alias test -keysize 512 -validity 3650 -keyalg RSA \
 *     -dname "CN=test.com, OU=test, O=Test Inc, L=Adelaide, \
 *     S=South Australia, C=AU" -keypass testing -storepass testing \
 *     -keystore client.ks
 */
public class JUTestTLS
{
  private static final String SECURE_URI = "elvin:/secure/127.0.0.1:29170";

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
    
    elvin.close ();
    router.close ();
  }
}
