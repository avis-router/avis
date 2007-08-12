package org.avis.io;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.avis.common.ElvinURI;

import static java.util.Arrays.asList;

/**
 * General networking utilities.
 * 
 * @author Matthew Phillips
 */
public final class Net
{
  private Net ()
  {
    // zip
  }
  
  /**
   * Try to discover local host name by scanning network interfaces
   * for non site-local, non-loopback addresses.
   * 
   * @return The canonical host name.
   * 
   * @throws IOException if no host name can be found.
   */
  public static String localHostName () 
    throws IOException
  {
    for (Enumeration<NetworkInterface> i = 
        NetworkInterface.getNetworkInterfaces (); i.hasMoreElements (); )
    {
      NetworkInterface ni = i.nextElement ();
      
      for (Enumeration<InetAddress> j = ni.getInetAddresses ();
           j.hasMoreElements (); )
      {
        InetAddress address = j.nextElement ();
        
        if (!address.isLoopbackAddress () && !address.isSiteLocalAddress ())
          return address.getCanonicalHostName ();
      }
    }
    
    throw new IOException ("Cannot determine a valid local host name");
  }
  
  /**
   * Generate a set of socket addresses for a given set of URI's. This
   * method allows interface names to be used rather than host names
   * by prefixing the host name with "!".
   * 
   * @param uris The URI's to turn into addresses.
   * 
   * @return The corresponding set of InetSocketAddress's for the URI's.
   * 
   * @throws IOException
   * @throws SocketException
   * @throws UnknownHostException
   */
  public static Set<InetSocketAddress>
    addressesFor (Set<? extends ElvinURI> uris)
      throws IOException, SocketException, UnknownHostException
  {
    Set<InetSocketAddress> addresses = new HashSet<InetSocketAddress> ();
    
    for (ElvinURI uri : uris)
    {
      Collection<InetAddress> inetAddresses;
      
      if (uri.host.startsWith ("!"))
        inetAddresses = addressesForInterface (uri.host.substring (1));
      else
        inetAddresses = addressesForHost (uri.host);
      
      for (InetAddress address : inetAddresses)
      {
        if (address.isAnyLocalAddress ())
          addresses.add (new InetSocketAddress (uri.port));
        else if (!address.isLinkLocalAddress ())
          addresses.add (new InetSocketAddress (address, uri.port));
      }
    }
    
    return addresses;
  }
  
  private static Collection<InetAddress> addressesForHost (String host)
    throws UnknownHostException
  {
    return asList (InetAddress.getAllByName (host));
  }

  private static Collection<InetAddress> addressesForInterface (String name)
    throws SocketException, IOException
  {
    NetworkInterface netInterface = NetworkInterface.getByName (name);
    
    if (netInterface == null)
    {
      throw new IOException
        ("Unknown interface name \"" + name + "\"");
    }
    
    HashSet<InetAddress> addresses = new HashSet<InetAddress> ();

    for (Enumeration<InetAddress> i = netInterface.getInetAddresses ();
         i.hasMoreElements (); )
    {
      addresses.add (i.nextElement ());
    }
    
    return addresses;
  }
}
