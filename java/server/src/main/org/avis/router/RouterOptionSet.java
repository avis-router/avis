package org.avis.router;

import org.apache.mina.core.session.IoSession;

import org.avis.config.OptionSet;
import org.avis.config.OptionTypeFromString;
import org.avis.config.OptionTypeURI;
import org.avis.io.InetAddressFilter;
import org.avis.util.Filter;

import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.common.Common.MB;
import static org.avis.config.OptionTypeURI.EMPTY_URI;
import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;

/**
 * The configuration options accepted by the router.
 * 
 * @author Matthew Phillips
 */
public class RouterOptionSet extends OptionSet
{
  public static final RouterOptionSet ROUTER_OPTION_SET =
    new RouterOptionSet ();
  
  public RouterOptionSet ()
  {
    add ("Port", 1, DEFAULT_PORT, 65535);
    add ("Listen", "elvin://0.0.0.0");
    add ("IO.Idle-Connection-Timeout", 1, 15, Integer.MAX_VALUE);
    add ("IO.Use-Direct-Buffers", false);
    add ("IO.Low-Memory-Protection.Min-Free-Memory", 0, 4*MB, Integer.MAX_VALUE);
    add ("TLS.Keystore", new OptionTypeURI (), EMPTY_URI);
    add ("TLS.Keystore-Passphrase", "");
    add ("Require-Authenticated", 
         new OptionTypeFromString (InetAddressFilter.class), Filter.MATCH_NONE);
    
    inheritFrom (CONNECTION_OPTION_SET);
  }

  public static void setSendQueueMaxLength (IoSession session, long length)
  {
    session.setAttribute ("Send-Queue.Max-Length", length);
  }
  
  public static void setSendQueueDropPolicy (IoSession session, String policy)
  {
    session.setAttribute ("Send-Queue.Drop-Policy", policy);
  }
  
  public static long sendQueueMaxLength (IoSession session)
  {
    Long maxLength = (Long)session.getAttribute ("Send-Queue.Max-Length");
    
    if (maxLength == null)
      return (Long)ROUTER_OPTION_SET.defaultValue ("Send-Queue.Max-Length");
    else
      return maxLength;
  }
  
  public static String sendQueueDropPolicy (IoSession session)
  {
    String policy = (String)session.getAttribute ("Send-Queue.Drop-Policy");
    
    if (policy == null)
      return (String)ROUTER_OPTION_SET.defaultValue ("Send-Queue.Drop-Policy");
    else
      return policy;
  }
}