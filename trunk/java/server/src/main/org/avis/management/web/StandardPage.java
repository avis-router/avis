package org.avis.management.web;

import java.util.List;

import org.apache.asyncweb.common.HttpRequest;
import org.apache.asyncweb.server.resolver.ServiceResolver;

import org.avis.management.web.pages.SiteNavigatorView;

import static java.util.Arrays.asList;

/**
 * A standard web management page with a navigator and main content area.
 * 
 * @author Matthew Phillips
 */
public class StandardPage extends HtmlPage
{
  public static final List<String> PAGES = 
    asList (new String [] 
      {"Overview", "Clients", "Federation", "Configuration"});
  
  public static final List<String> URIs = 
    asList (new String [] 
      {"overview", "clients", "federation", "configuration"});

  /**
   * Resolves any known standard pages.
   */
  public static final ServiceResolver RESOLVER = new ServiceResolver ()
  {
    public String resolveService (HttpRequest request)
    {
      String service = request.getRequestUri ().getPath ().substring (1);
      
      if (URIs.contains (service))
        return service;
      else     
        return null;
    }
  };

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
