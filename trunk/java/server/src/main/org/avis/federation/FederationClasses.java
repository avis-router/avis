package org.avis.federation;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import java.net.InetAddress;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

import static org.avis.util.Wildcard.toPattern;

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
  private Map<Pattern, FederationClass> hostToClass;

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
    this.hostToClass = new HashMap<Pattern, FederationClass> ();
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
    return classFor (address.getCanonicalHostName (), 
                     address.getHostAddress ());
  }
  
  /**
   * Get the federation class mapped to a given host.
   * 
   * @param hostName The host's canonical name.
   * @param ip The host's IP address.
   * 
   * @return The federation class, or defaultClass if no explicit
   *         mapping found.
   */
  public FederationClass classFor (String hostName, String ip)
  {
    for (Map.Entry<Pattern, FederationClass> entry : hostToClass.entrySet ())
    {
      Pattern hostPattern = entry.getKey ();
      
      if (hostPattern.matcher (hostName).matches () ||
          hostPattern.matcher (ip).matches ())
      {
        return entry.getValue ();
      }
    }
    
    return defaultClass;
  }
  
  /**
   * Map a host pattern to a federation class.
   * 
   * @param hostPattern A host-matching pattern.
   * @param fedClass The federation class to use for matching hosts.
   */
  public void map (Pattern hostPattern, FederationClass fedClass)
  {
    hostToClass.put (hostPattern, fedClass);
  }

  /**
   * Map a wildcard host pattern to a federation class.
   * 
   * @param host A wildcard host-matching pattern.
   * @param fedClass The federation class to use for matching hosts.
   */
  public void map (String host, FederationClass fedClass)
  {
    map (toPattern (host, CASE_INSENSITIVE), fedClass);
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