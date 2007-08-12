package org.avis.federation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.avis.util.IllegalOptionException;
import org.avis.util.OptionSet;
import org.avis.util.Options;

public class FederationOptions extends Options
{
  private static final Pattern CUSTOM_OPTION_PATTERN = 
    Pattern.compile ("([^:]+:?)([^:]+)?$");
  
  public static OptionSet OPTION_SET = new FederationOptionSet ();

  static class FederationOptionSet extends OptionSet
  {
    public FederationOptionSet ()
    {
      add ("Federation.Activated", false);
      add ("Federation.Router-Name", "");
      add ("Federation.Listen", "ewaf://0.0.0.0:2916");
      add ("Federation.Listen:", "");
      add ("Federation.Subscribe:", "");
      add ("Federation.Provide:", "");
      add ("Federation.Connect:", "");
      add ("Federation.Connect-Timeout", 1, 20, Integer.MAX_VALUE);
    }
    
    /**
     * Override validation to allow parameterised options like
     * "Federation.Subscribe:Public".
     */
    @Override
    protected void validateAndPut (Options options, String option,
                                   Object value)
      throws IllegalOptionException
    {
      String baseOption = splitParam (option) [0];
      
      value = convert (baseOption, value);
      
      String message = validate (baseOption, value);
      
      if (message != null)
        throw new IllegalOptionException (option, message);
      
      set (options, option, value);
    }
  }
  
  public FederationOptions ()
  {
    super (OPTION_SET);
  }

  /**
   * Split a parameterised option into a (base option, param) pair.
   */
  public static String [] splitParam (String option)
  {
    Matcher matcher = CUSTOM_OPTION_PATTERN.matcher (option);
    
    matcher.find ();
    
    return new String [] {matcher.group (1), matcher.group (2)};
  }
}
