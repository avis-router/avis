package org.avis.management.web;

import java.util.List;

import org.avis.management.web.pages.SiteNavigatorView;

import static java.util.Arrays.asList;

public class StandardPage extends Page
{
  public static final List<String> PAGES = 
    asList (new String [] 
      {"Overview", "Clients", "Federation", "Configuration"});
  
  public static final List<String> URIs = 
    asList (new String [] 
      {"overview", "clients", "federation", "configuration"});
  
  private String title;
  private HtmlView navigator;
  private HtmlView main;

  public StandardPage (String title, HtmlView main)
  {
    this.title = title;
    this.navigator = new SiteNavigatorView (title, PAGES, URIs);
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
