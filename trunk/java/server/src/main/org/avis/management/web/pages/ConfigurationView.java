package org.avis.management.web.pages;

import org.avis.config.Options;
import org.avis.management.web.HTML;
import org.avis.management.web.HtmlView;

public class ConfigurationView implements HtmlView
{
  private OptionsView optionsView;

  public ConfigurationView (Options options)
  {
    this.optionsView = new OptionsView (options);
  }

  public void render (HTML html)
  {
    html.append ("<h2>Configuration</h2>\n");

    html.append ("<div class='option-list'>\n").indent ();
    optionsView.render (html);
    html.outdent ().append ("</div>\n");
  }
}
