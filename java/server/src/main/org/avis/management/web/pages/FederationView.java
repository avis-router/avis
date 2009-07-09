package org.avis.management.web.pages;

import java.util.Comparator;

import org.avis.federation.FederationManager;
import org.avis.federation.Link;
import org.avis.management.web.HTML;
import org.avis.router.Router;

import static org.avis.federation.FederationManager.federationManagerFor;
import static org.avis.management.web.HTML.formatTime;
import static org.avis.util.Collections.sort;

public class FederationView implements HtmlView
{
  private static final Comparator<Link> LINK_COMPARATOR = 
    new Comparator<Link> ()
    {
      public int compare (Link link1, Link link2)
      {
        return link1.serial - link2.serial;
      }
    };

  private Router router;

  public FederationView (Router router)
  {
    this.router = router;
  }

  public void render (HTML html)
  {
    FederationManager manager = managerFor (router);
    
    if (manager == null)
    {
      html.append ("<p>Federation is not activated</p>");
      
      return;
    }
    
    html.append ("<table class='fed-list' border='1' cellspacing='0'>\n");
    html.indent ();
    
    html.append ("<tr><th class='numeric title'>Federator</th>" +
    		     "<th class='title'>Remote Host</th>" +
    		     "<th class='title'>Class</th>" +
    		     "<th class='title'>Connected</th>" +
    		     "<th class='numeric title'>Notifications " +
    		       "(Sent&nbsp;/&nbsp;Received)</th></tr>\n");
    
    for (Link link : sort (manager.links (), LINK_COMPARATOR))
    {
      html.append ("<tr><td class='number'>${}</td>" +
      		       "<td>${}</td>" +
      		       "<td>${}</td>" +
      		       "<td class='date'>${}</td>" +
      		       "<td class='number'>${}</td></tr>\n",
      		   link.serial, link.remoteHostAddress, 
      		   link.federationClass.name,
      		   formatTime (link.createdAt),
      		   "?? / ??");
    }
    
    html.outdent ().append ("</table>");
  }

  private static FederationManager managerFor (Router router)
  {
    try
    {
      return federationManagerFor (router);
    } catch (IllegalArgumentException ex)
    {
      return null;
    }
  }
}
