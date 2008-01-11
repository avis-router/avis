package org.avis.io;

import java.net.InetSocketAddress;

import org.avis.util.Filter;
import org.avis.util.WildcardFilter;

/**
 * A filter for internet addresses, matching against either the host
 * name or IP address.
 * 
 * @author Matthew Phillips
 */
public class InetAddressFilter implements Filter<InetSocketAddress>
{
  private WildcardFilter filter;

  public InetAddressFilter (String patterns)
  {
    this.filter = new WildcardFilter (patterns);
  }
  
  public boolean matches (InetSocketAddress address)
  {
    return filter.matches (address.getHostName ()) ||
           filter.matches (address.getAddress ().getHostAddress ()) ||
           filter.matches (address.getAddress ().getCanonicalHostName ());
  }
}
