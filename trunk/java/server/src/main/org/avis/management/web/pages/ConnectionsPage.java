package org.avis.management.web.pages;

import java.util.Date;

import org.avis.management.web.HTML;
import org.avis.router.Connection;
import org.avis.router.Router;
import org.avis.router.Subscription;

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
      // TODO add relative connection time
      connection.lockRead ();
      
      try
      {
        if (!connection.isOpen ())
          continue;
        
        html.append 
          ("<tr><td rowspan=\"2\">${1} (${2})</td><td>${3}</td>\n" + 
           "    <td>${4}</td><td>${5} / ${6}</td><td>${7}</td></tr>",
           connection.serial, connection.id (),
           formatConnectionTime (connection.connectedAt), 
           connection.hostname (),
           connection.sentNotificationCount, connection.receivedNotificationCount, 
           connection.subscriptions.size ());
      
        html.append ("<tr><td colspan=\"4\">\n");
  
        html.indent ();
        outputSubscriptions (html, connection);
        html.outdent ();
        
        html.append ("</td></tr>\n");
      } finally
      {
        connection.unlockRead ();
      }
    }

    html.outdent ();
    html.append ("</table>");
    
    html.appendClosingTags ();
    
    return html.asText ();
  }

  private void outputSubscriptions (HTML html, Connection connection)
  {
    html.append ("<table>\n");

    html.indent ();
   
    for (Subscription subscription : connection.subscriptions ())
    {
      html.append 
        ("<tr><td>${1}</td><td class=\"sub-exp\">${2}</td><td>(${3})</td></tr>",
         subscription.id, subscription.expr, guessSubType (subscription.expr));
    }
    
    html.outdent ();
    
    html.append ("</table>\n");
  }

  private String guessSubType (String subExpr)
  {
    // TODO implement
    return "Unknown";
  }

  private String formatConnectionTime (long time)
  {
    return getDateTimeInstance (SHORT, SHORT).format (new Date (time));
  }
}
