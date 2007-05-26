package org.avis.server;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.avis.common.ElvinURI;
import org.avis.util.IllegalOptionException;
import org.avis.util.OptionSet;
import org.avis.util.Options;

import static java.util.Arrays.asList;

import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.common.ElvinURI.defaultProtocol;
import static org.avis.server.ConnectionOptionSet.CONNECTION_OPTION_SET;
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
      add ("Listen", "elvin://0.0.0.0");
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
   * as specified by the Listen setting.
   */
  public Set<InetSocketAddress> bindAddresses ()
    throws SocketException
  {
    Set<InetSocketAddress> addresses = new HashSet<InetSocketAddress> ();
    ElvinURI defaultUri = new ElvinURI ("0.0.0.0", getInt ("Port"));
    
    for (String listenItem : split (getString ("Listen"), "\\s+"))
    {
      try
      {
        ElvinURI uri = new ElvinURI (listenItem, defaultUri);
        
        if (!uri.protocol.equals (defaultProtocol ()))
        {
          throw new IllegalOptionException
            ("Avis only supports " + defaultProtocol () +" protocol: " + 
             listenItem);
        }
        
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
      } catch (URISyntaxException ex)
      {
        throw new IllegalOptionException
          ("Listen",
           "Invalid URI \"" + listenItem + "\": " + ex.getMessage ());
      }
    }
    
    return addresses;
  }

  private static Collection<InetAddress> addressesForHost (String host)
  {
    try
    {
      return asList (InetAddress.getAllByName (host));
    } catch (UnknownHostException ex)
    {
      throw new IllegalOptionException
        ("Unknown host name \"" + host + "\"");
    }
  }

  private static Collection<InetAddress> addressesForInterface (String name)
      throws SocketException, IllegalOptionException
  {
    NetworkInterface netInterface = NetworkInterface.getByName (name);
    
    if (netInterface == null)
      throw new IllegalOptionException
        ("Unknown interface name \"" + name + "\"");
    
    HashSet<InetAddress> addresses = new HashSet<InetAddress> ();

    for (Enumeration<InetAddress> i = netInterface.getInetAddresses ();
         i.hasMoreElements (); )
    {
      addresses.add (i.nextElement ());
    }
    
    return addresses;
  }
}