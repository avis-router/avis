package org.avis.management.pages;

import java.util.List;

import org.avis.management.HTML;
import org.avis.management.HtmlView;

public class SiteNavigatorView implements HtmlView
{
  private String currentPage;
  private List<String> pages;
  private List<String> uris;

  public SiteNavigatorView (String currentPage, 
                            List<String> pages, List<String> uris)
  {
    this.currentPage = currentPage;
    this.pages = pages;
    this.uris = uris;
  }

  public void render (HTML html)
  {
    html.append ("<ul id='nav'>\n").indent ();
    
    for (int i = 0; i < pages.size (); i++)
    {
      String page = pages.get (i);
      String href = uris.get (i);
              
      if (i > 0)
        html.append ("\n");
      
      if (href.equals (uriFor (currentPage)))
        html.append ("<li class='nav-current'>");        
      else
        html.append ("<li>");

      if (i > 0)
        html.append (" | ");

      html.append ("<a href='${}'>${}</a></li>", href, page);
    }
    
    html.outdent ().append ("\n</ul>\n");
  }

  private String uriFor (String page)
  {
    for (int i = 0; i < pages.size (); i++)
    {
      if (pages.get (i).equals (page))
        return uris.get (i);
    }
    
    return null;
  }
}
