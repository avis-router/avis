package org.avis.federation;

import java.util.HashMap;
import java.util.Map;

import java.net.InetAddress;

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
  private Map<String, FederationClass> dnsDomains;
  private Map<String, FederationClass> federatorDomains;
  private Map<String, FederationClass> hosts;

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
    this.classes = new HashMap<String, FederationClass> ();
    this.dnsDomains = new HashMap<String, FederationClass> ();
    this.hosts = new HashMap<String, FederationClass> ();
    this.federatorDomains = new HashMap<String, FederationClass> ();
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
   * @param serverDomain The federation server domain.
   * 
   * @return The federation class, or defaultClass if no explicit
   *         mapping found.
   */
  public FederationClass classFor (InetAddress address, String serverDomain)
  {
    String hostName = address.getCanonicalHostName ();

    // match host name/IP
    FederationClass fedClass = hosts.get (hostName);
    
    if (fedClass != null)
      return fedClass;
    
    fedClass = hosts.get (address.getHostAddress ());
    
    if (fedClass != null)
      return fedClass;

    // match domain
    for (Map.Entry<String, FederationClass> entry : dnsDomains.entrySet ())
    {
      if (hostName.endsWith (entry.getKey ()))
        return entry.getValue ();
    }
    
    return classFor (serverDomain);
  }

  /**
   * Match a federation class for the given server domain.
   */
  public FederationClass classFor (String serverDomain)
  {
    FederationClass fedClass = federatorDomains.get (serverDomain);
    
    if (fedClass != null)
      return fedClass;
    
    // fall back to default
    return defaultClass;
  }
  
  public void mapDnsDomain (String domain, FederationClass fedClass)
  {
    dnsDomains.put (domain, fedClass);
  }

  public void mapHost (String hostname, FederationClass fedClass)
  {
    hosts.put (hostname, fedClass);
  }
  
  public void mapServerDomain (String serverDomain, FederationClass fedClass)
  {
    federatorDomains.put (serverDomain, fedClass);
  }
}