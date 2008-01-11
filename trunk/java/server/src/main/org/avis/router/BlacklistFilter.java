package org.avis.router;

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
  private Filter<InetSocketAddress> blacklist;

  public BlacklistFilter (Filter<InetSocketAddress> blacklist)
  {
    this.blacklist = blacklist;
  }

  @Override
  public void sessionOpened (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    if (blacklist.matches ((InetSocketAddress)session.getRemoteAddress ()))
    {
      diagnostic ("Refusing unauthenticated connection attempt from " + 
                  session.getRemoteAddress () + 
                  " due to it matching the hosts requiring authentication", 
                  this);
      
      session.close ();
    } else
    {
      nextFilter.sessionOpened (session);
    }
  }
}
