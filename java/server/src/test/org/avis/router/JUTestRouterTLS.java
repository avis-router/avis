package org.avis.router;

import java.net.InetSocketAddress;
import java.net.URI;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.mina.core.ExceptionMonitor;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.ssl.SslFilter;
import org.junit.Before;
import org.junit.Test;

import org.avis.io.ClientFrameCodec;
import org.avis.io.ExceptionMonitorLogger;

public class JUTestRouterTLS
{
  private static final int DEFAULT_PORT = 29170;
  private static final int SECURE_PORT = 29171;

  @Before
  public void setup ()
  {
    // route MINA exceptions to log
    ExceptionMonitor.setInstance (ExceptionMonitorLogger.INSTANCE);
  }
  
  /*
   * Command to generate the test key store:
   *   keytool -genkey -alias test -keysize 512 -validity 3650 -keyalg RSA \
   *     -dname "CN=test.com, OU=test, O=Test Inc, L=Adelaide, \
   *     S=South Australia, C=AU" -keypass testing -storepass testing \
   *     -keystore tls_test.ks
   */
  @Test
  public void connect () 
    throws Exception
  {
    ExceptionMonitor.setInstance (ExceptionMonitorLogger.INSTANCE);
    
    RouterOptions options = new RouterOptions ();
    URI keystore = getClass ().getResource ("tls_test.ks").toURI ();
    
    options.set ("Listen", 
                 "elvin://127.0.0.1:" + DEFAULT_PORT + " " + 
                 "elvin:/secure/127.0.0.1:" + SECURE_PORT);
    options.set ("TLS.Keystore", keystore);
    options.set ("TLS.Keystore-Passphrase", "testing");
   
    Router router = new Router (options);
    
    SimpleClient standardClient = 
      new SimpleClient (new InetSocketAddress ("127.0.0.1", DEFAULT_PORT));
    
    standardClient.connect ();
    
    standardClient.close ();
    
    SimpleClient secureClient = 
      new SimpleClient (new InetSocketAddress ("127.0.0.1", SECURE_PORT), 
                        createTLSFilters ());
    
    secureClient.connect ();
    
    secureClient.close ();
    
    router.close ();
  }

  private static DefaultIoFilterChainBuilder createTLSFilters () 
    throws Exception
  {
    DefaultIoFilterChainBuilder filters = new DefaultIoFilterChainBuilder ();
    
    SSLContext sslContext = SSLContext.getInstance ("TLS");
    sslContext.init (null, new TrustManager [] {ACCEPT_ALL_MANAGER}, null);
    
    SslFilter sslFilter = new SslFilter (sslContext);
    sslFilter.setUseClientMode (true);
    
    filters.addLast ("ssl", sslFilter);
    filters.addLast  ("codec", ClientFrameCodec.FILTER);
                                
    return filters;
  }
  
  static final X509TrustManager ACCEPT_ALL_MANAGER = new X509TrustManager ()
  {
    public void checkClientTrusted (X509Certificate [] x509Certificates,
                                    String s)
      throws CertificateException
    {
      // zip: allow anything
    }

    public void checkServerTrusted (X509Certificate [] x509Certificates,
                                    String s)
      throws CertificateException
    {
      // zip: allow anything
    }

    public X509Certificate [] getAcceptedIssuers ()
    {
      return new X509Certificate [0];
    }
  };
}
