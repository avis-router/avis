package org.avis.federation;

import java.util.HashMap;
import java.util.Map;

import java.net.InetAddress;

/**
 * Defines the mapping from a remote federator to the FederationClass
 * that should be applied to it.
 * 
 * @see FederationClass
 * 
 * @author Matthew Phillips
 */
public class FederationClassMap
{
  private FederationClass defaultClass;
  private Map<String, FederationClass> classes;
  private Map<String, FederationClass> dnsDomains;
  private Map<String, FederationClass> federatorDomains;
  private Map<String, FederationClass> hosts;

  /**
   * Create a new instance.
   * 
   * @param defaultClass The default federation class.
   */
  public FederationClassMap (FederationClass defaultClass)
  {
    this.defaultClass = defaultClass;
    this.classes = new HashMap<String, FederationClass> ();
    this.dnsDomains = new HashMap<String, FederationClass> ();
    this.hosts = new HashMap<String, FederationClass> ();
    this.federatorDomains = new HashMap<String, FederationClass> ();
  }

  /**
   * Find an existing federation class with the given name or create a
   * new one.
   */
  public FederationClass findOrCreate (String name)
  {
    FederationClass fedClass = classes.get (name);
    
    if (fedClass == null)
      classes.put (name, fedClass = new FederationClass ());
    
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
    String canonicalHostName = address.getCanonicalHostName ();

    // match host name/IP
    FederationClass fedClass = hosts.get (canonicalHostName);
    
    if (fedClass != null)
      return fedClass;
    
    fedClass = hosts.get (address.getHostAddress ());
    
    if (fedClass != null)
      return fedClass;

    // match domain
    for (Map.Entry<String, FederationClass> entry : dnsDomains.entrySet ())
    {
      if (canonicalHostName.endsWith (entry.getKey ()))
        return entry.getValue ();
    }
    
    // match server domain
    fedClass = federatorDomains.get (serverDomain);
    
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