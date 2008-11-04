package org.avis.router;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.security.KeyStore;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.TrafficMask;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.ssl.SslFilter;

import org.avis.util.Filter;

import static org.avis.io.TLS.sslContextFor;
import static org.avis.logging.Log.diagnostic;

/**
 * A front end filter that wraps a MINA SSL filter for secure
 * connections. This allows the policy for determining which hosts
 * must be authenticated to be determined on a per host basis.
 * 
 * @author Matthew Phillips
 */
public class SecurityFilter implements IoFilter
{
  private KeyStore keystore;
  private String keystorePassphrase;
  private Filter<InetAddress> authRequired;
  private boolean clientMode;

  public SecurityFilter (KeyStore keystore,
                         String keystorePassphrase,
                         Filter<InetAddress> authRequired, 
                         boolean clientMode)
  {
    this.keystore = keystore;
    this.keystorePassphrase = keystorePassphrase;
    this.authRequired = authRequired;
    this.clientMode = clientMode;
  }
  
  private SslFilter sslFilterFor (IoSession session)
    throws Exception
  {
    SslFilter filter = (SslFilter)session.getAttribute ("securityFilterSSL");
    
    if (filter == null)
    {
      InetAddress address = 
        ((InetSocketAddress)session.getRemoteAddress ()).getAddress ();
      
      boolean needAuth = authRequired.matches (address);
      
      diagnostic ("Host " + address + " connecting via TLS " +
                  (needAuth ? "needs authentication" : 
                               "does not require authentication"), this);
      
      filter = 
        new SslFilter (sslContextFor (keystore, keystorePassphrase, needAuth));
    
      filter.setUseClientMode (clientMode);
      filter.setNeedClientAuth (needAuth);
      
      session.setAttribute ("securityFilterSSL", filter);
    }
    
    return filter;
  }

  public void init () 
    throws Exception
  {
    // zip
  }
  
  public void destroy ()  
    throws Exception
  {
    // zip
  }
  
  public void filterSetTrafficMask (NextFilter nextFilter,
                                    IoSession session,
                                    TrafficMask trafficMask)
    throws Exception
  {
    sslFilterFor (session).filterSetTrafficMask 
      (nextFilter, session, trafficMask);
  }

  public void sessionCreated (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    sslFilterFor (session).sessionCreated (nextFilter, session);
  }

  public void exceptionCaught (NextFilter nextFilter,
                               IoSession session, Throwable cause)
    throws Exception
  {
    sslFilterFor (session).exceptionCaught (nextFilter, session, cause);
  }

  public void filterClose (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    sslFilterFor (session).filterClose (nextFilter, session);
  }

  public void filterWrite (NextFilter nextFilter, IoSession session,
                           WriteRequest writeRequest)
    throws Exception
  {
    sslFilterFor (session).filterWrite (nextFilter, session, writeRequest);
  }

  public void messageReceived (NextFilter nextFilter,
                               IoSession session, Object message)
    throws Exception
  {
    sslFilterFor (session).messageReceived (nextFilter, session, message);
  }

  public void messageSent (NextFilter nextFilter, IoSession session,
                           WriteRequest writeRequest)
    throws Exception
  {
    sslFilterFor (session).messageSent (nextFilter, session, writeRequest);
  }

  public void onPostAdd (IoFilterChain parent, String name,
                         NextFilter nextFilter) throws Exception
  {
    sslFilterFor (parent.getSession ()).onPostAdd (parent, name, nextFilter);
  }

  public void onPostRemove (IoFilterChain parent, String name,
                            NextFilter nextFilter) throws Exception
  {
    sslFilterFor (parent.getSession ()).onPostRemove (parent, name, nextFilter);
  }

  public void onPreAdd (IoFilterChain parent, String name,
                        NextFilter nextFilter) throws Exception
  {
    sslFilterFor (parent.getSession ()).onPreAdd (parent, name, nextFilter);
  }

  public void onPreRemove (IoFilterChain parent, String name,
                           NextFilter nextFilter) throws Exception
  {
    sslFilterFor (parent.getSession ()).onPreRemove (parent, name, nextFilter);
  }

  public void sessionClosed (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    sslFilterFor (session).sessionClosed (nextFilter, session);
  }

  public void sessionIdle (NextFilter nextFilter, IoSession session,
                           IdleStatus status) throws Exception
  {
    sslFilterFor (session).sessionIdle (nextFilter, session, status);
  }

  public void sessionOpened (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    sslFilterFor (session).sessionOpened (nextFilter, session);
  }
}
