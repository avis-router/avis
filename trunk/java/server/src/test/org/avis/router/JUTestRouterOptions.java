package org.avis.router;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import org.avis.federation.FederationOptionSet;

import org.junit.Test;

import static java.net.NetworkInterface.getNetworkInterfaces;

import static org.avis.common.Common.DEFAULT_PORT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JUTestRouterOptions
{
  @Test
  public void bindAll ()
    throws Exception
  {
    RouterOptions options = new RouterOptions ();
    options.set ("Port", "29170");
    options.set ("Listen", "elvin://0.0.0.0");
    
    Set<InetSocketAddress> addresses = options.listenAddresses ();
    
    assertEquals (1, addresses.size ());
    
    assertTrue
      (addresses.contains (new InetSocketAddress (options.getInt ("Port"))));
  }
  
  /**
   * Test binding to an interface name.
   */
  @Test
  public void bindInterface ()
    throws Exception
  {
    NetworkInterface netInterface = getNetworkInterfaces ().nextElement ();
    
    Set<InetAddress> interfaceAddresses = new HashSet<InetAddress> ();
    
    for (Enumeration<InetAddress> i = netInterface.getInetAddresses ();
        i.hasMoreElements (); )
    {
      InetAddress address = i.nextElement ();

      // System.out.println ("Interface address = " + address);
      if (!address.isLinkLocalAddress ())
        interfaceAddresses.add (address);        
    }
    
    testInterfaces (interfaceAddresses, netInterface.getName (), DEFAULT_PORT);
    testInterfaces (interfaceAddresses,
                    netInterface.getName () + ":29170", 29170);
  }
  
  /**
   * Test binding to a host name.
   */
  @Test
  public void bindHost ()
    throws Exception
  {
    InetAddress localhost = InetAddress.getLocalHost ();
    Set<InetAddress> localhostAddresses = new HashSet<InetAddress> ();
    
    for (InetAddress address : InetAddress.getAllByName (localhost.getHostName ()))
    {
      // System.out.println ("Host address = " + address);
      if (!address.isLinkLocalAddress ())
        localhostAddresses.add (address);
    }

    testHost (localhostAddresses, localhost.getHostName (), DEFAULT_PORT);
    testHost (localhostAddresses, localhost.getHostName () + ":29170", 29170);
  }

  /**
   * Test multiple URI's in Listen field with whitespace.
   * @throws Exception
   */
  @Test
  public void multipleURIs ()
    throws Exception
  {
    RouterOptions options = new RouterOptions ();
    options.set ("Listen",
                 "elvin:/tcp,none,xdr/localhost:1234 \t elvin://localhost");
    
    Set<InetSocketAddress> addresses = options.listenAddresses ();
    
    boolean found1234 = false;
    
    for (InetSocketAddress address : addresses)
    {
      if (address.getPort () == 1234)
        found1234 = true;
    }
    
    assertTrue (found1234);
  }
  
  /**
   * Test that adding federator options (as router's Main does) to
   * router's works
   */
  @Test
  public void federationOptions ()
    throws Exception
  {
    RouterOptionSet routerOptionSet = new RouterOptionSet ();
    
    routerOptionSet.inheritFrom (FederationOptionSet.OPTION_SET);
    
    RouterOptions config = new RouterOptions (routerOptionSet);
    
    config.set ("Federation.Subscribe[Test]", "require (federated)");
  }

  private void testHost (Set<InetAddress> hostAddresses, String hostOption, int port)
    throws SocketException
  {
    RouterOptions options = new RouterOptions ();
    options.set ("Listen", "elvin://" + hostOption);
    
    Set<InetSocketAddress> addresses = options.listenAddresses ();
    
    assertEquals (hostAddresses.size (), addresses.size ());
    
    for (InetAddress address : hostAddresses)
      assertTrue (addresses.contains (new InetSocketAddress (address, port)));
  }

  private void testInterfaces (Set<InetAddress> interfaceAddresses,
                               String interfaceOption,
                               int port)
    throws SocketException
  {
    RouterOptions options = new RouterOptions ();
    options.set ("Listen", "elvin://!" + interfaceOption);
    
    Set<InetSocketAddress> addresses = options.listenAddresses ();
    
    assertEquals (interfaceAddresses.size (), addresses.size ());
    
    for (InetAddress address : interfaceAddresses)
      assertTrue (addresses.contains (new InetSocketAddress (address, port)));
  }
}
