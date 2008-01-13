package org.avis.router;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;

import org.avis.util.Filter;

import static org.avis.logging.Log.diagnostic;

/**
 * An IO filter that blocks hosts matching a given selection filter.
 * 
 * @author Matthew Phillips
 */
public class BlacklistFilter extends IoFilterAdapter implements IoFilter
{
  private Filter<InetAddress> blacklist;

  public BlacklistFilter (Filter<InetAddress> blacklist)
  {
    this.blacklist = blacklist;
  }

  @Override
  public void sessionOpened (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    InetAddress address = 
      ((InetSocketAddress)session.getRemoteAddress ()).getAddress ();
    
    if (blacklist.matches (address))
    {
      diagnostic 
        ("Refusing non-TLS connection from host " + address + 
         " due to it matching the hosts requiring authentication", this);
      
      session.close ();
    } else
    {
      nextFilter.sessionOpened (session);
    }
  }
}
