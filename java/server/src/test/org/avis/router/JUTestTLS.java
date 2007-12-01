package org.avis.router;

import java.net.InetSocketAddress;
import java.net.URI;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.io.ClientFrameCodec;
import org.avis.io.ExceptionMonitorLogger;

import org.junit.Test;

public class JUTestTLS
{
  private static final int PORT = 29170;

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
    // Log.enableLogging (Log.DIAGNOSTIC, true);
    // route MINA exceptions to log
    ExceptionMonitor.setInstance (ExceptionMonitorLogger.INSTANCE);
    
    RouterOptions options = new RouterOptions ();
    URI keystore = getClass ().getResource ("tls_test.ks").toURI ();
    
    options.set ("Listen", "elvin:/secure/127.0.0.1:" + PORT);
    options.set ("TLS.Router-Keystore", keystore);
    options.set ("TLS.Router-Keystore.Passphrase", "testing");
   
    Router router = new Router (options);
    
    SimpleClient client = 
      new SimpleClient (new InetSocketAddress ("127.0.0.1", PORT), 
                        createTLSConfig ());
    
    client.connect ();
    
    client.close ();
    router.close ();
  }

  private static SocketConnectorConfig createTLSConfig () 
    throws Exception
  {
    SocketConnectorConfig connectorConfig = new SocketConnectorConfig ();
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (10);
    
    SSLContext sslContext = SSLContext.getInstance ("TLS");
    sslContext.init (null, new TrustManager [] {ACCCEPT_ALL_MANAGER}, null);
    
    SSLFilter sslFilter = new SSLFilter (sslContext);
    sslFilter.setUseClientMode (true);
    
    connectorConfig.getFilterChain ().addFirst 
      ("ssl", sslFilter);
                                
    connectorConfig.getFilterChain ().addLast 
      ("codec", ClientFrameCodec.FILTER);
    
    return connectorConfig;
  }
  
  static final X509TrustManager ACCCEPT_ALL_MANAGER = new X509TrustManager ()
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
