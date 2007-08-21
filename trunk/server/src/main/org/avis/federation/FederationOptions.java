package org.avis.federation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.avis.common.InvalidURIException;
import org.avis.config.OptionSet;
import org.avis.config.OptionType;
import org.avis.config.OptionTypeSet;
import org.avis.config.Options;
import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.util.IllegalOptionException;
import org.avis.util.Pair;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import static org.avis.federation.FederationClass.parse;

public class FederationOptions extends Options
{
  public static OptionSet OPTION_SET = new FederationOptionSet ();

  private static final List<String> EMPTY_STRING_LIST = emptyList ();
  
  public FederationOptions ()
  {
    super (OPTION_SET);
  }

  /**
   * Get a value for a parameterised option. e.g.
   * "Federation.Subscribe[Param]".
   * 
   * @param option The option, minus the parameters.
   * 
   * @return The value of the option, mapping parameters to values.
   */
  public Map<String, Object> getParamOption (String option)
  {
    return getParamOption (this, option);
  }
  
  @SuppressWarnings("unchecked")
  public static Map<String, Object> getParamOption (Options options, 
                                                    String option)
  {
    OptionType type = options.optionSet ().optionTypeFor (option);
    
    if (type instanceof ParamOption)
      return (Map<String, Object>)options.get (option);
    else
      throw new IllegalOptionException (option, "Not a parameterised option");
  }

  /**
   * Split a parameterised option into a (base option, params) pair.
   */
  protected static Pair<String,List<String>> splitOptionParam (String option)
  {
    int index = option.indexOf ('[');
    
    if (index == -1)
    {
      if (option.indexOf (']') == -1)
        return new Pair<String, List<String>> (option, EMPTY_STRING_LIST);
      else
        throw new IllegalOptionException (option, "Orphan ]");
    }
    
    String base = option.substring (0, index);
    List<String> params = new ArrayList<String> ();

    while (index < option.length ())
    {
      int end = option.indexOf (']', index);
      
      if (end == -1)
        throw new IllegalOptionException (option, "Missing ]");
      
      params.add (option.substring (index + 1, end));
      
      index = end + 1;
      
      if (index < option.length () && option.charAt (index) != '[')
          throw new IllegalOptionException (option, "Junk in parameter list");
    }
    
    return new Pair<String, List<String>> (base, params);
  }
  
  static class FederationOptionSet extends OptionSet
  {
    public FederationOptionSet ()
    {
      ParamOption fedClassOption = new ParamOption (new SubExpOption ());
      EwafUriOption ewafUriOption = new EwafUriOption ();
      
      add ("Federation.Activated", false);
      add ("Federation.Router-Name", "");
      add ("Federation.Listen", new OptionTypeSet (EwafURI.class), emptySet ());
      add ("Federation.Subscribe", fedClassOption, emptyMap ());
      add ("Federation.Provide", fedClassOption, emptyMap ());
      add ("Federation.Apply-Class", 
           new ParamOption (new OptionTypeSet (String.class)), emptyMap ());
      add ("Federation.Connect", 
           new ParamOption (ewafUriOption), emptyMap ());
      add ("Federation.Connect-Timeout", 1, 20, Integer.MAX_VALUE);
    }
    
    @Override
    protected OptionType peekOptionTypeFor (String option)
    {
      Pair<String, List<String>> optionItems = splitOptionParam (option);
      
      return optionTypes.get (optionItems.item1);
    }
    
    /**
     * Override validation to allow parameterised options like
     * "Federation.Subscribe[Public]".
     */
    @Override
    protected void validateAndSet (Options options, String option,
                                   Object value)
      throws IllegalOptionException
    {
      Pair<String, List<String>> optionItems = splitOptionParam (option);
      
      OptionType type = optionTypeFor (optionItems.item1);
      
      if (type instanceof ParamOption)
      {
        // allow param option to create/update param values
        value = 
          ((ParamOption)type).updateValue (options, option, 
                                           optionItems.item1,
                                           optionItems.item2, value);
      } else
      {
        if (!optionItems.item2.isEmpty ())
        {
          throw new IllegalOptionException
            (option, "Cannot specify parameters for option");
        }
      }

      super.validateAndSet (options, optionItems.item1, value);
    }
  }
  
  /**
   * An subscription expression option.
   */
  static class SubExpOption extends OptionType
  {
    @Override
    public Object convert (String option, Object value)
      throws IllegalOptionException
    {
      try
      {
        if (value instanceof Node)
          return value;
        else
          return parse (value.toString ());
      } catch (ParseException ex)
      {
        throw new IllegalOptionException 
          (option, "Invalid subscription: " + ex.getMessage ());
      }
    }

    @Override
    public String validate (String option, Object value)
    {
      return validateType (value, Node.class);
    }
  }

  /**
   * An EWAF URI option.
   */
  static class EwafUriOption extends OptionType
  {
    @Override
    public Object convert (String option, Object value)
      throws IllegalOptionException
    {
      try
      {
        if (!(value instanceof EwafURI))
          value = new EwafURI (value.toString ());
        
        return value;
      } catch (InvalidURIException ex)
      {
        throw new IllegalOptionException (option, ex.getMessage ());
      }
    }

    @Override
    public String validate (String option, Object value)
    {
      return validateType (value, EwafURI.class);
    }
  }
  
  /**
   * A parameterised option. These options have a Map as a value,
   * mapping parameters to their values (which may themselves be maps
   * for multi-dimensional options).
   */
  static class ParamOption extends OptionType
  {
    private OptionType subOption;
    private int paramCount;

    public ParamOption (OptionType option)
    {
      this (option, 1);
    }

    public ParamOption (OptionType subOption, int paramCount)
    {
      this.subOption = subOption;
      this.paramCount = paramCount;
    }

    @Override
    public Object convert (String option, Object value)
      throws IllegalOptionException
    {
      return value;
    }

    @Override
    public String validate (String option, Object value)
    {
      return validateType (value, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Object updateValue (Options options, 
                               String option, 
                               String baseOption,
                               List<String> params, Object value)
    {
      if (params.size () != paramCount)
        throw new IllegalOptionException (option, 
                                          "Parameters required " + paramCount);
      
      value = subOption.convert (option, value);

      String valid = subOption.validate (option, value);
      
      if (valid != null)
        throw new IllegalOptionException (option, valid);
      
      // create/lookup base Map value
      Map<String, Object> baseValue = 
        (Map<String, Object>)options.get (baseOption);
      
      if (baseValue.isEmpty ())
        baseValue = new HashMap<String, Object> ();
      
      // create/lookup any sub-map's
      Map<String, Object> map = baseValue;
      
      for (int i = paramCount; i > 1; i--)
        map = createEntry (map, params.get (i));
      
      // store value
      map.put (params.get (params.size () - 1), value);
      
      return baseValue;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> createEntry (Map<String, Object> map,
                                                    String key)
    {
      Map<String, Object> entry = (Map<String, Object>)map.get (key);
      
      if (entry == null)
      {
        entry = new HashMap<String, Object> ();
        
        map.put (key, entry);
      }
      
      return entry;
    }
  }
}
