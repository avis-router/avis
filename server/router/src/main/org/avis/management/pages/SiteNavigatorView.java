package org.avis.management.pages;

import java.util.ArrayList;
import java.util.List;

import org.avis.management.HTML;
import org.avis.management.HtmlView;

import static org.avis.util.Text.join;

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
    
    String currentPageUri = uriFor (currentPage);
    ArrayList<String> classes = new ArrayList<String> (2);
    
    for (int i = 0; i < pages.size (); i++)
    {
      String page = pages.get (i);
      String href = uris.get (i);
    
      classes.clear ();
      
      if (href.equals (currentPageUri))
        classes.add ("nav-current");        

      if (i == pages.size () - 1)
        classes.add ("nav-last");
      
      if (i > 0)
        html.append ("\n");
      
      if (classes.isEmpty ())
        html.append ("<li>");        
      else
        html.append ("<li class='${}'>", join (classes, " "));        

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
