package org.avis.federation;

public class FederationClassMap
{
  private FederationClass defaultClass;

  /**
   * Create a new instance.
   * 
   * @param defaultClass The default federation class.
   */
  public FederationClassMap (FederationClass defaultClass)
  {
    this.defaultClass = defaultClass;
  }

  /**
   * Get the federation class mapped to a given host.
   * 
   * @param hostname The host's name.
   * 
   * @return The federation class, or defaultClass if no explicit
   *         mapping found.
   */
  public FederationClass classFor (String hostname)
  {
    // todo
    return defaultClass;
  }
}
