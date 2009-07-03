package org.avis.util;

/**
 * A filter that selects values of the generic type T.
 * 
 * @author Matthew Phillips
 */
public interface Filter<T>
{
  /**
   * Matches nothing.
   */
  public static final Filter<?> MATCH_NONE = new Filter<Object> ()
  {
    public boolean matches (Object value)
    {
      return false;
    }
  };
  
  /**
   * Matches anything.
   */
  public static final Filter<?> MATCH_ALL = new Filter<Object> ()
  {
    public boolean matches (Object value)
    {
      return true;
    }
  };

  /**
   * Test if the filter matches.
   * 
   * @param value The value to test.
   * 
   * @return True if the fiLter matches.
   */
  public boolean matches (T value);  
}
