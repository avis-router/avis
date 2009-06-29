package org.avis.management.web;

import org.avis.router.Router;

public class ConnectionsPage extends WebPage
{
  private Router router;

  public ConnectionsPage (Router router)
  {
    this.router = router;
  }

  @Override
  protected String htmlText ()
  {
    HTML html = new HTML ();
    
    html.appendStandardHeader ("Connections - Avis").appendBody ();
    
    html.append ("<p>Number of connections: ${1}</p>", 
                 router.connections ().size ());
    
    html.appendStandardClose ();
    
    return html.asText ();
  }
}
