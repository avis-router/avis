package org.avis.io;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketSessionConfig;

import org.avis.common.ElvinURI;
import org.avis.common.InvalidURIException;
import org.avis.io.messages.Message;
import org.avis.util.Text;

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
   * Get local host name.
   * 
   * @return The canonical host name.
   * 
   * @throws IOException if no host name can be found.
   */
  public static String localHostName () 
    throws IOException
  {
    return InetAddress.getLocalHost ().getCanonicalHostName ();
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
    addressesFor (Iterable<? extends ElvinURI> uris)
      throws IOException, SocketException, UnknownHostException
  {
    Set<InetSocketAddress> addresses = new HashSet<InetSocketAddress> ();
    
    for (ElvinURI uri : uris)
      addAddressFor (addresses, uri.host, uri.port);
    
    return addresses;
  }

  /**
   * Generate a set of socket addresses for a URL. This method allows
   * interface names to be used rather than host names by prefixing
   * the host name with "!".
   * 
   * @param url The URL to turn into addresses.
   * @param defaultPort The default port to use if none specified.
   * 
   * @return The corresponding set of InetSocketAddress's for the
   *         URL's.
   * 
   * @throws IOException
   * @throws SocketException
   * @throws UnknownHostException
   */
  public static Set<InetSocketAddress> addressesFor (URL url, 
                                                     int defaultPort) 
    throws IOException, SocketException, UnknownHostException
  {
    Set<InetSocketAddress> addresses = new HashSet<InetSocketAddress> ();
    
    addAddressFor 
      (addresses, url.getHost (), 
       url.getPort () == -1 ? defaultPort : url.getPort ());
    
    return addresses;
  }
  
  /**
   * Generate a set of socket addresses for a given set of URL's. This
   * method allows interface names to be used rather than host names
   * by prefixing the host name with "!".
   * 
   * @param urls The URL's to turn into addresses.
   * @param defaultPort The default port to use if none specified.
   * 
   * @return The corresponding set of InetSocketAddress's for the URL's.
   * 
   * @throws IOException
   * @throws SocketException
   * @throws UnknownHostException
   */
  public static Set<InetSocketAddress> addressesFor (Iterable<URL> urls, 
                                                     int defaultPort) 
    throws IOException, SocketException, UnknownHostException
  {
    Set<InetSocketAddress> addresses = new HashSet<InetSocketAddress> ();
    
    for (URL url : urls)
    {
      addAddressFor 
        (addresses, url.getHost (), 
         url.getPort () == -1 ? defaultPort : url.getPort ());
    }
    
    return addresses;
  }

  /**
   * Generate the addresses for a given URI. This method allows
   * interface names to be used rather than host names by prefixing
   * the host name with "!".
   * 
   * @param uri The URI.
   * @return The set of network addresses that correspond to the URI.
   * 
   * @throws IOException
   * @throws SocketException
   * @throws UnknownHostException
   */
  public static Set<InetSocketAddress> addressesFor (ElvinURI uri)
    throws IOException, SocketException, UnknownHostException
  {
    Set<InetSocketAddress> addresses = new HashSet<InetSocketAddress> ();
    
    addAddressFor (addresses, uri.host, uri.port);
    
    return addresses;
  }

  private static void addAddressFor (Set<InetSocketAddress> addresses,
                                     String host, int port)
    throws SocketException, IOException, UnknownHostException
  {
    Collection<InetAddress> inetAddresses;
    
    if (host.startsWith ("!"))
      inetAddresses = addressesForInterface (host.substring (1));
    else
      inetAddresses = addressesForHost (host);
    
    for (InetAddress address : inetAddresses)
    {
      if (address.isAnyLocalAddress ())
        addresses.add (new InetSocketAddress (port));
      else if (!address.isLinkLocalAddress ())
        addresses.add (new InetSocketAddress (address, port));
    }
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

  /**
   * Find the host address for an InetSocketAddress session.
   */
  public static InetAddress remoteHostAddressFor (IoSession session)
  {
    if (session.getRemoteAddress () == null)
      throw new IllegalStateException ("Session is not connected");
    
    if (session.getRemoteAddress () instanceof InetSocketAddress)
    {
      return ((InetSocketAddress)session.getRemoteAddress ()).getAddress ();
    } else
    {
      throw new Error ("Can't get host name for address type " + 
                       session.getRemoteAddress ().getClass ());
    }
  }
  
  /**
   * Generate a standard host ID for a session using host name and IP.
   */
  public static String hostIdFor (IoSession session)
  {
    return hostIdFor (remoteHostAddressFor (session));
  }

  /**
   * Generate a standard host ID for a session using host name and IP.
   */
  public static String hostIdFor (InetAddress address)
  {
    String name = address.getCanonicalHostName ();
    String ip = address.getHostAddress ();
    
    if (name.equals (ip))
      return ip;
    else
      return name + " / " + ip; 
  }
  
  /**
   * Generate a human-readable ID for a session.
   */
  public static String idFor (IoSession session)
  {
    return session == null ? "<null>" : Long.toString (session.getId ());
  }
  
  /**
   * Generate a human-readable ID for a message.
   */
  public static String idFor (Message message)
  {
    return Text.idFor (message);
  }

  /**
   * Set TCP no-delay flag for socket connections.
   * 
   * @param session The IO session.
   * @param noDelay The new setting for TCP_NODELAY.
   * 
   * @return true if the setting could be changed.
   */
  public static boolean enableTcpNoDelay (IoSession session, boolean noDelay)
  {
    if (session.getConfig () instanceof SocketSessionConfig)
    {
      ((SocketSessionConfig)session.getConfig ()).setTcpNoDelay (noDelay);
      
      return true;
    } else
    {
      return false; 
    }
  }

  /**
   * Create a new URI without the annoying checked exception.
   * 
   * @param uriString The URI string.
   * @return A new URI.
   * @throws InvalidURIException if uriString is invalid.
   */
  public static URI uri (String uriString)
    throws InvalidURIException
  {
    try
    {
      return new URI (uriString);
    } catch (URISyntaxException ex)
    {
      throw new InvalidURIException (ex);
    }
  }
  
  /**
   * Create a new URL without the annoying checked exception.
   * 
   * @param urlString The URI string.
   * @return A new URL.
   * @throws InvalidURIException if uriString is invalid.
   */
  public static URL url (String urlString)
    throws InvalidURIException
  {
    try
    {
      return new URL (urlString);
    } catch (MalformedURLException ex)
    {
      throw new InvalidURIException (ex);
    }
  }
}
