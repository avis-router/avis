package org.avis.management.web.pages;

import java.util.Date;

import org.avis.management.web.HTML;
import org.avis.router.Connection;
import org.avis.router.Router;
import org.avis.router.Subscription;

import static org.avis.management.web.HTML.num;

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
      ("<table class='client-list' border='1' cellspacing='0'>\n\n" +
       "  <tr><th class='numeric'>Client</th> <th>Connected</th> <th>Host</th>\n" + 
       "      <th class='numeric'>Subscriptions</th> " +
             "<th class='numeric'>Notifications<br/>(Sent / Received)</th></tr>\n");
    
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
          ("<tr><td rowspan='2' class='numeric'>${} (${})</td><td>${}</td>\n" + 
           "    <td>${}</td><td class='numeric'>${}</td>" +
               "<td class='numeric'>${} / ${}</td></tr>\n",
           connection.serial, connection.id (),
           formatConnectionTime (connection.connectedAt), 
           connection.remoteHost ().getCanonicalHostName (),
           num (connection.subscriptions.size ()),
           num (connection.sentNotificationCount), 
           num (connection.receivedNotificationCount));
      
        html.append ("<tr><td colspan='4'>\n");
  
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

  private static void outputSubscriptions (HTML html, Connection connection)
  {
    html.append ("<table width='100%'>\n");

    html.indent ();
   
    for (Subscription subscription : connection.subscriptions ())
    {
      html.append 
        ("<tr><td class='numeric'>${}</td><td class='sub-exp'>${}</td>" +
             "<td>(${})</td><td class='numeric'>${}</td></tr>\n",
         subscription.id, subscription.expr, guessSubType (subscription.expr),
         num (subscription.notificationCount));
    }
    
    html.outdent ();
    
    html.append ("</table>\n");
  }

  private static String guessSubType (String subExpr)
  {
    if (subExpr.contains ("Presence-Protocol"))
      return "Presence";
    else if (subExpr.contains ("org.tickertape.message"))
      return "Tickertape";
    else if (subExpr.contains ("Livespace-Protocol"))
      return "Livespaces";
    else if (subExpr.contains ("NEWSGROUPS"))
      return "Ticker News";
    else
      return "Unknown";
  }

  private String formatConnectionTime (long time)
  {
    return getDateTimeInstance (SHORT, SHORT).format (new Date (time));
  }
}
