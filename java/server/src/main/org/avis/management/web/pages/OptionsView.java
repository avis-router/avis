package org.avis.management.web.pages;

import java.util.Map;
import java.util.regex.Pattern;

import org.avis.config.OptionType;
import org.avis.config.OptionTypeParam;
import org.avis.config.Options;
import org.avis.management.web.HTML;
import org.avis.management.web.HtmlView;

import static org.avis.config.OptionTypeParam.getParamOption;

public class OptionsView implements HtmlView
{
  private Options options;
  private Pattern filter;

  public OptionsView (Options options)
  {
    this.options = options;
    this.filter = null;
  }
  
  public OptionsView (Options options, String filterRegex)
  {
    this.options = options;
    this.filter = Pattern.compile (filterRegex);
  }

  public void render (HTML html)
  {
    html.append ("<table>\n").indent ();
    
    for (Map.Entry<String, OptionType> e : 
         options.optionSet ().all ().entrySet ())
    {
      String option = e.getKey ();
      
      if (filter == null || filter.matcher (option).matches ())
      {
        if (e.getValue () instanceof OptionTypeParam)
        {
          renderParamOption (html, option, 
                             getParamOption (options, option));
        } else
        {
          renderOptionValue (html, option, options.get (option));
        }
      }
    }
    
    html.outdent ().append ("</table>\n");
  }

  public void renderOptionValue (HTML html, String option, Object value)
  {
    html.append ("<tr class='${}'><td>${}:</td><td>${}</td></tr>\n", 
                 options.isDefaultValue (stripParams (option)) ? 
                   "option-default" : "option-set",
                 option, value);
  }

  @SuppressWarnings ("unchecked")
  private void renderParamOption (HTML html, String baseName,
                                  Map<String, Object> paramValues)
  {
    for (Map.Entry<String, Object> e : paramValues.entrySet ())
    {
      String name = baseName + '[' + e.getKey () + ']';
      Object value = e.getValue ();
      
      if (value instanceof Map)
        renderParamOption (html, name, (Map<String, Object>)value);
      else
        renderOptionValue (html, name, value);
    }
  }
  
  private static String stripParams (String option)
  {
    int bracket = option.indexOf ('[');
    
    return bracket == -1 ? option : option.substring (0, bracket);
  }
}