package org.avis.io;

import java.net.InetAddress;

import org.avis.util.Filter;
import org.avis.util.WildcardFilter;

import static java.util.Arrays.asList;

import static org.avis.util.Text.split;

/**
 * A filter for internet addresses, matching against either the host
 * name or IP address.
 * 
 * @author Matthew Phillips
 */
public class InetAddressFilter implements Filter<InetAddress>
{
  private WildcardFilter filter;

  public InetAddressFilter (String patterns)
  {
    this.filter = new WildcardFilter (asList (split (patterns)));
  }
  
  public boolean matches (InetAddress address)
  {
    return filter.matches (address.getHostName ()) ||
           filter.matches (address.getHostAddress ()) ||
           filter.matches (address.getCanonicalHostName ());
  }
}
