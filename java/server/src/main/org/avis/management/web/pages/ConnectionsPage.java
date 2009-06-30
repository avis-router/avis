package org.avis.management.web.pages;

import java.util.Date;

import org.avis.management.web.HTML;
import org.avis.router.Connection;
import org.avis.router.Router;

import static java.text.DateFormat.SHORT;
import static java.text.DateFormat.getDateTimeInstance;

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
       "      <th>Notifications<br/>(Sent / Received)</th> <th>Subscriptions</th></tr>\n");
    
    html.indent ();
    
    for (Connection connection : router.connections ())
    {
      // TODO implement ${} option
      html.append 
        ("<tr><td rowspan=\"2\">${1} (${2})</td><td>${3}</td>\n" + 
         "    <td>${4}</td><td>${5} / ${6}</td><td>${7}</td></tr>",
         connection.serial, connection.id (),
         formatConnectionTime (connection.connectedAt), 
         connection.hostname (),
         connection.sentNotificationCount, connection.receivedNotificationCount, 
         connection.subscriptions.size ());
    }

    html.outdent ();
    
    html.append ("</table>");
    
    html.appendClosingTags ();
    
    return html.asText ();
  }

  private String formatConnectionTime (long time)
  {
    return getDateTimeInstance (SHORT, SHORT).format (new Date (time));
  }
}
