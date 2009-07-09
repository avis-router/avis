package org.avis.management.web.pages;

import org.avis.management.web.HTML;
import org.avis.management.web.HtmlView;

public class SiteNavigatorView implements HtmlView
{
  private String currentPage;
  private String [] pages;

  public SiteNavigatorView (String currentPage, String [] pages)
  {
    this.currentPage = currentPage;
    this.pages = pages;
  }

  public void render (HTML html)
  {
    html.append ("<p class='nav'>\n").indent ();
    
    for (int index = 0; index < pages.length; index++)
    {
      String page = pages [index];
      String href = page.toLowerCase ();
      String cssClass = 
        href.equals (currentPage.toLowerCase ()) ? 
          "nav-item nav-current" : "nav-item";
      
      if (index > 0)
        html.append ("&nbsp;| \n");
      
      html.append ("<span class='${}'><a href='${}'>${}</a></span>",
                   cssClass, href, page);
    }
    
    html.outdent ().append ("\n</p>\n");
  }
}
