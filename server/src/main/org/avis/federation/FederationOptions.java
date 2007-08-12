package org.avis.federation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.avis.util.IllegalOptionException;
import org.avis.util.OptionSet;
import org.avis.util.Options;

public class FederationOptions extends Options
{
  public static OptionSet OPTION_SET = new FederationOptionSet ();
  
  static class FederationOptionSet extends OptionSet
  {
    protected static final Pattern CUSTOM_OPTION_PATTERN = 
      Pattern.compile ("([^:]+:?)([^:]+)?$");
    
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
      Matcher matcher = CUSTOM_OPTION_PATTERN.matcher (option);
      matcher.find ();
      
      String baseOption = matcher.group (1);
      
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
}
