package org.avis.federation;

import org.avis.common.InvalidURIException;
import org.avis.config.OptionSet;
import org.avis.config.OptionType;
import org.avis.config.OptionTypeParam;
import org.avis.config.OptionTypeSet;
import org.avis.config.OptionTypeValueExpr;
import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.util.IllegalOptionException;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import static org.avis.federation.FederationClass.parse;
import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;

/**
 * Configuration option set for Avis federation.
 * 
 * @author Matthew Phillips
 */
public class FederationOptionSet extends OptionSet
{
  public static final OptionSet OPTION_SET = new FederationOptionSet ();
  
  protected FederationOptionSet ()
  {
    OptionTypeParam fedClassOption = new OptionTypeParam (new SubExpOption ());
    EwafUriOption ewafUriOption = new EwafUriOption ();
    OptionTypeParam attrOption = 
      new OptionTypeParam (new OptionTypeValueExpr (), 2);
    
    add ("Federation.Activated", false);
    add ("Federation.Router-Name", "");
    add ("Federation.Listen", new OptionTypeSet (EwafURI.class), emptySet ());
    add ("Federation.Subscribe", fedClassOption, emptyMap ());
    add ("Federation.Provide", fedClassOption, emptyMap ());
    add ("Federation.Apply-Class", 
         new OptionTypeParam (new OptionTypeSet (String.class)), emptyMap ());
    add ("Federation.Connect", 
         new OptionTypeParam (ewafUriOption), emptyMap ());
    add ("Federation.Add-Incoming-Attribute", attrOption, emptyMap ());
    add ("Federation.Add-Outgoing-Attribute", attrOption, emptyMap ());
    add ("Federation.Request-Timeout", 1, 20, Integer.MAX_VALUE);
    add ("Federation.Keepalive-Interval", 1, 60, Integer.MAX_VALUE);
    
    // allow connection options such as Packet.Max-Length
    inheritFrom (CONNECTION_OPTION_SET);
  }
  
  /**
   * A subscription expression option.
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
}