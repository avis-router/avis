package org.avis.federation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.avis.io.InetAddressFilter;
import org.avis.util.Filter;
import org.avis.util.Pair;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

/**
 * Defines a mapping from remote hosts to the FederationClass that
 * should be applied to them.
 * 
 * @see FederationClass
 * 
 * @author Matthew Phillips
 */
public class FederationClasses
{
  private FederationClass defaultClass;
  private Map<String, FederationClass> classes;
  private List<Pair<Filter<InetAddress>, FederationClass>> hostToClass;

  public FederationClasses ()
  {
    this (new FederationClass ());
  }
  
  /**
   * Create a new instance.
   * 
   * @param defaultClass The default federation class.
   */
  public FederationClasses (FederationClass defaultClass)
  {
    this.defaultClass = defaultClass;
    this.classes = 
      new TreeMap<String, FederationClass> (CASE_INSENSITIVE_ORDER);
    this.hostToClass = new ArrayList<Pair<Filter<InetAddress>,FederationClass>> ();
  }

  public void setDefaultClass (FederationClass newDefaultClass)
  {
    defaultClass = newDefaultClass;
  }
  
  public FederationClass defaultClass ()
  {
    return defaultClass;
  }
  
  /**
   * Find an existing federation class with the given name or create a
   * new one.
   */
  public FederationClass define (String name)
  {
    FederationClass fedClass = classes.get (name);
    
    if (fedClass == null)
    {
      fedClass = new FederationClass ();
      
      fedClass.name = name;
      
      classes.put (name, fedClass);
    }
    
    return fedClass;
  }
  
  /**
   * Get the federation class mapped to a given host.
   * 
   * @param address The host's address.
   * 
   * @return The federation class, or defaultClass if no explicit
   *         mapping found.
   */
  public FederationClass classFor (InetAddress address)
  {
    for (Pair<Filter<InetAddress>, FederationClass> entry : hostToClass)
    {
      if (entry.item1.matches (address))
        return entry.item2;
    }
    
    return defaultClass;
  }
  
  /**
   * Get the federation class mapped to a given host.
   * 
   * @param hostName The host's canonical name.
   * 
   * @return The federation class, or defaultClass if no explicit
   *         mapping found.
   */
  public FederationClass classFor (String hostName)
  {
    try
    {
      return classFor (InetAddress.getByName (hostName));
    } catch (UnknownHostException ex)
    {
      throw new IllegalArgumentException (ex.getMessage ());
    }
  }
  
  /**
   * Map a host pattern to a federation class.
   * 
   * @param matcher A host-matching filter.
   * @param fedClass The federation class to use for matching hosts.
   */
  public void map (Filter<InetAddress> matcher, FederationClass fedClass)
  {
    hostToClass.add 
      (new Pair<Filter<InetAddress>, FederationClass> (matcher, fedClass));
  }

  /**
   * Map a wildcard host pattern to a federation class.
   * 
   * @param host A wildcard host-matching pattern.
   * @param fedClass The federation class to use for matching hosts.
   */
  public void map (String host, FederationClass fedClass)
  {
    map (new InetAddressFilter (host), fedClass);
  }

  /**
   * Clear all mappings and classes.
   */
  public void clear ()
  {
    classes.clear ();
    hostToClass.clear ();
  }
}