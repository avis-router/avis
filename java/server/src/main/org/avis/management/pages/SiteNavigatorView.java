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
    html.append ("<p class='nav'>\n").indent ();
    
    for (int i = 0; i < pages.size (); i++)
    {
      String page = pages.get (i);
      String href = uris.get (i);
      String cssClass = 
        href.equals (uriFor (currentPage)) ? 
          "nav-item nav-current" : "nav-item";
      
      if (i > 0)
        html.append ("&nbsp;| \n");
      
      html.append ("<span class='${}'><a href='${}'>${}</a></span>",
                   cssClass, href, page);
    }
    
    html.outdent ().append ("\n</p>\n");
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
