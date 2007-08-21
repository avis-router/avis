package org.avis.router;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import java.io.IOException;

import java.net.InetSocketAddress;

import org.avis.common.ElvinURI;
import org.avis.common.InvalidURIException;
import org.avis.config.Options;
import org.avis.util.IllegalOptionException;

import static org.avis.common.ElvinURI.defaultProtocol;
import static org.avis.io.Net.addressesFor;
import static org.avis.util.Text.split;

/**
 * Options used to configure the Avis router.
 * 
 * @author Matthew Phillips
 */
public class RouterOptions extends Options
{
  public RouterOptions ()
  {
    this (new RouterOptionSet ());
  }
  
  public RouterOptions (RouterOptionSet optionSet)
  {
    super (optionSet);
  }

  /**
   * Shortcut to create an option set with a given "Port" setting.
   */
  public RouterOptions (int port)
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
  public RouterOptions (Properties properties)
  {
    this ();
    
    setAll (properties);
  }
  
  /**
   * Generate the set of network addresses the server should bind to
   * as specified by the Listen setting.
   */
  public Set<InetSocketAddress> listenAddresses ()
  {
    Set<ElvinURI> uris = new HashSet<ElvinURI> ();
    ElvinURI defaultUri = new ElvinURI ("0.0.0.0", getInt ("Port"));
    
    for (String listenItem : split (getString ("Listen"), "\\s+"))
    {
      try
      {
        ElvinURI uri = new ElvinURI (listenItem, defaultUri);
        
        if (!uri.protocol.equals (defaultProtocol ()))
        {
          throw new IllegalOptionException
            ("Listen",
             "Avis only supports " + defaultProtocol () +" protocol: " + 
             listenItem);
        }
        
        uris.add (uri);
        
      } catch (InvalidURIException ex)
      {
        throw new IllegalOptionException ("Listen", ex.getMessage ());
      }
    }
    
    try
    {
      return addressesFor (uris);
    } catch (IOException ex)
    {
      throw new IllegalOptionException ("Listen", ex.getMessage ());
    }
  }
}