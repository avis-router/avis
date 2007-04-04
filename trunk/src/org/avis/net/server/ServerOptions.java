package org.avis.net.server;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.avis.util.IllegalOptionException;
import org.avis.util.OptionSet;
import org.avis.util.Options;
import org.avis.util.Pair;

import static java.lang.Integer.parseInt;
import static java.util.Collections.singleton;

import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.net.common.ConnectionOptionSet.CONNECTION_OPTION_SET;
import static org.avis.util.Text.split;

/**
 * Options used to configure the Avis server.
 * 
 * @author Matthew Phillips
 */
public class ServerOptions extends Options
{
  private static final OptionSet OPTION_SET = new ServerOptionSet ();

  static class ServerOptionSet extends OptionSet
  {
    public ServerOptionSet ()
    {
      add ("Port", 1, DEFAULT_PORT, 65535);
      add ("Bind.Interfaces", "*");
      add ("Bind.Hosts", "*");
    }
  }
  
  public ServerOptions ()
  {
    super (OPTION_SET);
    
    // allow default connection options to be specified also
    optionSet.inheritFrom (CONNECTION_OPTION_SET);
  }

  /**
   * Shortcut to create an option set with a given "Port" setting.
   */
  public ServerOptions (int port)
  {
    this ();
    
    set ("Port", port);
  }
  
  /**
   * Shortcut to create an option set from an initial set of
   * properties.
   * 
   * @param properties The initial settings.
   * 
   * @see #setAll(Properties)
   */
  public ServerOptions (Properties properties)
  {
    this ();
    
    setAll (properties);
  }
  
  /**
   * Generate the set of network addresses the server should bind to
   * as specified by the Bind.Hosts and Bind.Interfaces settings.
   */
  public Set<InetSocketAddress> bindAddresses ()
    throws SocketException
  {
    String interfaceNames = getString ("Bind.Interfaces");
    String hostNames = getString ("Bind.Hosts");
    int defaultPort = getInt ("Port");
  
    if (interfaceNames.equals ("*") || hostNames.equals ("*"))
      return singleton (new InetSocketAddress (defaultPort));
    
    Set<InetSocketAddress> addresses = new HashSet<InetSocketAddress> ();
    
    for (String interfaceName : split (interfaceNames, " *, *"))
    {
      Pair<String,Integer> hostPort = hostPortFor (interfaceName, defaultPort);
      NetworkInterface netInterface = NetworkInterface.getByName (hostPort.item1);
      
      if (netInterface == null)
        throw new IllegalOptionException
          ("Unknown interface name \"" + interfaceName + "\"");
      
      for (Enumeration<InetAddress> i = netInterface.getInetAddresses ();
           i.hasMoreElements (); )
        addAddress (addresses, i.nextElement (), hostPort.item2);
    }
    
    for (String hostName : split (hostNames, " *, *"))
    {
      Pair<String,Integer> hostPort = hostPortFor (hostName, defaultPort);
      
      try
      {
        for (InetAddress address : InetAddress.getAllByName (hostPort.item1))
          addAddress (addresses, address, hostPort.item2);
      } catch (UnknownHostException ex)
      {
        throw new IllegalOptionException
          ("Unknown host name \"" + hostPort.item1 + "\"");
      }
    }
    
    return addresses;
  }

  private static void addAddress (Set<InetSocketAddress> addresses,
                                  InetAddress address, int port)
  {
    if (!address.isLinkLocalAddress ())
      addresses.add (new InetSocketAddress (address, port));
  }

  private static Pair<String, Integer> hostPortFor (String hostPortStr,
                                                    int defaultPort)
  {
    Pair<String, Integer> hostPort = new Pair<String, Integer> ();
    
    hostPortStr = hostPortStr.trim ();
    
    hostPort.item1 = hostPortStr;
    hostPort.item2 = defaultPort;
    
    int colon = hostPortStr.indexOf (':');
    
    if (colon != -1)
    {
      if (colon == 0 || colon == hostPortStr.length () - 1)
        throw new IllegalOptionException
          ("Invalid name:port item: \"" + hostPortStr + "\"");
  
      try
      {
        hostPort.item1 = hostPortStr.substring (0, colon);
        hostPort.item2 = parseInt (hostPortStr.substring (colon + 1));
      } catch (NumberFormatException ex)
      {
        throw new IllegalOptionException
          ("Invalid port number in \"" + hostPortStr + "\"");
      }
    }
    
    return hostPort;
  }
}