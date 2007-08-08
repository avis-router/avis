package org.avis.router;

import java.util.Map;

import org.avis.util.Options;

import static java.util.Collections.emptyMap;

import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;

/**
 * Avis router connection options.
 * 
 * @author Matthew Phillips
 * 
 * @see ConnectionOptionSet
 */
public class ConnectionOptions extends Options
{
  private static final Map<String, Object> EMPTY_OPTIONS = emptyMap ();
  
  private Map<String, Object> requested;

  public ConnectionOptions ()
  {
    this (null, EMPTY_OPTIONS);
  }
  
  public ConnectionOptions (Map<String, Object> requested)
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
  public ConnectionOptions (Options defaultOptions, 
                            Map<String, Object> requested)
  {
    super (CONNECTION_OPTION_SET);
    
    if (defaultOptions != null)
      addDefaults (defaultOptions);
    
    this.requested = requested;
    
    setAll (requested);
  }
  
  /**
   * Generate the set of accepted client options.
   *
   * @see ConnectionOptionSet#accepted(Options, Map)
   */
  public Map<String, Object> accepted ()
  {
    return CONNECTION_OPTION_SET.accepted (this, requested);
  }

  /**
   * Set an option and its legacy option (if any).
   */
  public void setWithLegacy (String option, Object value)
  {
    set (option, value);
    set (CONNECTION_OPTION_SET.newToLegacy (option), value);
  }
}
