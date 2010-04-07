package org.avis.management.pages;

import java.util.ArrayList;
import java.util.List;

import org.avis.management.HTML;
import org.avis.management.HtmlView;
import org.avis.management.Page;

import static org.avis.util.Text.join;

public class SiteNavigatorView implements HtmlView
{
  private Page currentPage;
  private List<Page> pages;

  public SiteNavigatorView (Page currentPage, List<Page> pages)
  {
    this.currentPage = currentPage;
    this.pages = pages;
  }

  public void render (HTML html)
  {    
    html.append ("<ul id='nav'>\n").indent ();
    
    ArrayList<String> classes = new ArrayList<String> (2);
    
    for (int i = 0; i < pages.size (); i++)
    {
      Page page = pages.get (i);
    
      classes.clear ();
      
      if (page == currentPage)
        classes.add ("nav-current");        

      if (i == pages.size () - 1)
        classes.add ("nav-last");
      
      if (i > 0)
        html.append ("\n");
      
      if (classes.isEmpty ())
        html.append ("<li>");        
      else
        html.append ("<li class='${}'>", join (classes, " "));        

      html.append ("<a href='${}'>${}</a></li>", page.uri, page.title);
    }
    
    html.outdent ().append ("\n</ul>\n");
  }
}
