package org.avis.router;

import java.util.Map;

import org.avis.config.Options;

import static org.avis.router.ClientConnectionOptionSet.CLIENT_CONNECTION_OPTION_SET;

import static java.util.Collections.emptyMap;

/**
 * Avis router connection options.
 * 
 * @author Matthew Phillips
 * 
 * @see ConnectionOptionSet
 */
public class ClientConnectionOptions extends Options
{
  private static final Map<String, Object> EMPTY_OPTIONS = emptyMap ();
  
  private Map<String, Object> requested;

  public ClientConnectionOptions ()
  {
    this (null, EMPTY_OPTIONS);
  }
  
  public ClientConnectionOptions (Map<String, Object> requested)
  {
    this (null, requested);
  }
  
  /**
   * Create a new instance from a set of requested values.
   * 
   * @param defaultOptions The default option values for options that
   *                are not set, usually set by the router. May be
   *                null for standard defaults.
   * @param requested The requested set of values, usually from the
   *                client creating the connection.
   */
  public ClientConnectionOptions (Options defaultOptions, 
                                  Map<String, Object> requested)
  {
    super (CLIENT_CONNECTION_OPTION_SET);
    
    if (defaultOptions != null)
      addDefaults (defaultOptions);
    
    this.requested = requested;
    
    setAll (requested);
  }
  
  /**
   * Generate the set of accepted client options.
   *
   * @see ClientConnectionOptionSet#accepted(Options, Map)
   */
  public Map<String, Object> accepted ()
  {
    return CLIENT_CONNECTION_OPTION_SET.accepted (this, requested);
  }

  /**
   * Set an option and its legacy option (if any).
   */
  public void setWithLegacy (String option, Object value)
  {
    set (option, value);
    set (CLIENT_CONNECTION_OPTION_SET.newToLegacy (option), value);
  }
}
