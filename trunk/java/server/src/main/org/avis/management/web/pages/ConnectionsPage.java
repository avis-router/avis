package org.avis.management.web.pages;

import org.avis.management.web.HTML;
import org.avis.router.Router;

public class ConnectionsPage extends Page
{
  private Router router;

  public ConnectionsPage (Router router)
  {
    this.router = router;
  }

  @Override
  protected String content ()
  {
    HTML html = new HTML ();
    
    html.appendXHTMLHeader ("Connections - Avis").appendBody ();
    
    html.append ("<p>Number of connections: ${1}</p>\n", 
                 router.connections ().size ());
 
    html.append 
      ("<table class=\"client-list\" border=\"1\" cellspacing=\"0\">\n\n" +
       "  <tr><th>Client</th> <th>Connected</th> <th>Host</th> \n" + 
       "      <th>Notifications</th> <th>Subscriptions</th></tr>\n");
    
    html.append ("</table>");
    
    html.appendClosingTags ();
    
    return html.asText ();
  }
}
