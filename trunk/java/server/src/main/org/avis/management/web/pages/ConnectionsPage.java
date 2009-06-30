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
    
    html.append ("<p>Number of connections: ${1}</p>", 
                 router.connections ().size ());
    
    html.appendClosingTags ();
    
    return html.asText ();
  }
}
