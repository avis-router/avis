package org.avis.management;

import java.util.List;

import org.apache.asyncweb.common.HttpRequest;
import org.apache.asyncweb.server.resolver.ServiceResolver;

import org.avis.management.pages.SiteNavigatorView;

/**
 * A standard web management page with a navigator and main content area.
 * 
 * @author Matthew Phillips
 */
public class StandardPage extends HtmlPage
{
  private String title;
  private HtmlView navigator;
  private HtmlView main;

  public StandardPage (String title, HtmlView main, SiteNavigatorView siteNav)
  {
    this.title = title;
    this.navigator = siteNav;
    this.main = main;
  }
  
  @Override
  protected String content ()
  {
    HTML html = new HTML ();
    
    appendXHTMLHeader (html);
    
    html.append ("<body>\n");

    navigator.render (html);
    
    main.render (html);
    
    html.append ("\n</body>\n</html>\n");

    return html.asText ();
  }
  
  private void appendXHTMLHeader (HTML html)
  {
    html.append 
      ("<!DOCTYPE html>\n" + 
       "<head>\n" + 
       "  <meta http-equiv=\"content-type\" " +
             "content=\"text/html; charset=UTF-8\" />\n" + 
       "  <title>${1} - Avis Router</title>\n" + 
       "  <link href=\"screen.css\" media=\"screen\" " +
             "rel=\"stylesheet\" type=\"text/css\" />\n" + 
       "  <link href=\"print.css\" media=\"print\" " +
             "rel=\"stylesheet\" type=\"text/css\" />", title);
    
    if (main instanceof JavascriptView)
    {
      html.append ("\n").indent ();
      ((JavascriptView)main).renderJavascript (html);
      html.outdent ();
    }
    
    html.append ("\n</head>\n");
  }

  /**
   * Resolves any known standard pages.
   */
  public static class Resolver implements ServiceResolver
  {
    private List<Page> pages;

    public Resolver (List<Page> pages)
    {
      this.pages = pages;
    }

    public String resolveService (HttpRequest request)
    {
      String service = request.getRequestUri ().getPath ().substring (1);
      
      for (Page page : pages)
      {
        if (page.uri.equals (service))
          return service;
      }
      
      return null;
    }
  }
}
