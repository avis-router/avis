package org.avis.management.web;

import org.avis.management.web.pages.SiteNavigatorView;

public class StandardPage extends Page
{
  private static final String [] PAGES = 
    {"Overview", "Clients", "Federation", "Configuration"};
  
  private String title;
  private HtmlView navigator;
  private HtmlView main;

  public StandardPage (String title, HtmlView main)
  {
    this.title = title;
    this.navigator = new SiteNavigatorView (title, PAGES);
    this.main = main;
  }
  
  @Override
  protected String content ()
  {
    HTML html = new HTML ();
    
    html.appendXHTMLHeader (title + " - Avis").appendBody ();
    
    navigator.render (html);
    
    main.render (html);
    
    html.appendClosingTags ();
    
    return html.asText ();
  }
}
