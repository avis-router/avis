package org.avis.management.web.pages;

import java.util.Map;
import java.util.regex.Pattern;

import org.avis.config.OptionType;
import org.avis.config.Options;
import org.avis.management.web.HTML;
import org.avis.management.web.HtmlView;

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
    html.append ("<p><div class='prop-list small-scrolling'><table>\n");
    html.indent ();
    
    for (Map.Entry<String, OptionType> e : 
         options.optionSet ().all ().entrySet ())
    {
      if (filter == null || filter.matcher (e.getKey ()).matches ())
      {
        html.append ("<tr><td>${}:</td><td>${}</td></tr>\n",
                     e.getKey (), options.get (e.getKey ()));
      }
    }
    
    html.outdent ();
    html.append ("</table></div></p>\n");
  }
}
