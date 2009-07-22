package org.avis.management;

import java.util.List;

import org.apache.asyncweb.common.HttpRequest;
import org.apache.asyncweb.server.resolver.ServiceResolver;

import org.avis.management.pages.SiteNavigatorView;

import static java.util.Arrays.asList;

/**
 * A standard web management page with a navigator and main content area.
 * 
 * @author Matthew Phillips
 */
public class StandardPage extends HtmlPage
{
  public static final List<String> PAGES = 
    asList ("Overview", "Clients", "Federation", "Configuration", "Log");
  
  public static final List<String> URIs = 
    asList ("overview", "clients", "federation", "configuration", "log");

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
    
    appendXHTMLHeader (html, title);
    
    html.append ("<body>\n");

    navigator.render (html);
    
    main.render (html);
    
    html.append ("\n</body>\n</html>\n");

    return html.asText ();
  }
  
  private static void appendXHTMLHeader (HTML html, String title)
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
             "rel=\"stylesheet\" type=\"text/css\" />\n" + 
      "</head>\n", title);
  }
}
