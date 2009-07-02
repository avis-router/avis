package org.avis.management.web.pages;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.avis.management.web.HTML;
import org.avis.router.Connection;
import org.avis.router.Router;
import org.avis.router.Subscription;

import static org.avis.management.web.HTML.num;

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
       "  <tr><th class='numeric'>Client</th>" +
             "<th>Connected</th>" +
             "<th>Host</th>\n" + 
             "<th class='numeric'>Keys (Subscription&nbsp;/&nbsp;Notification)</th>\n" +
             "<th class='numeric'>Subscriptions</th>\n" +
             "<th class='numeric'>Notifications (Sent&nbsp;/&nbsp;Received)</th></tr>\n");
    
    html.indent ();
    
    List<Connection> connections = router.connections ();
    
    Collections.sort (connections, new Comparator<Connection> ()
    {
      public int compare (Connection c1, Connection c2)
      {
        return c1.serial - c2.serial;
      }
    });
    
    for (Connection connection : connections)
    {
      // TODO add relative connection time
      connection.lockRead ();
      
      try
      {
        if (!connection.isOpen ())
          continue;
        
        html.append 
          ("<tr><td rowspan='2' class='numeric'>${} (${})</td>" +
               "<td>${}</td>" +
               "<td>${}</td>" +
               "<td class='numeric'>${} / ${}</td>" +
               "<td class='numeric'>${}</td>" +
               "<td class='numeric'>${} / ${}</td></tr>\n",
           connection.serial, connection.id (),
           formatConnectionTime (connection.connectedAt), 
           connection.remoteHost ().getCanonicalHostName (),
           num (connection.subscriptionKeys.size ()),
           num (connection.notificationKeys.size ()),
           num (connection.subscriptions.size ()),
           num (connection.sentNotificationCount), 
           num (connection.receivedNotificationCount));
      
        html.append ("<tr><td colspan='5' class='sub-list'>\n");
  
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
    html.append ("<table class='sub-list'>\n");

    html.indent ();
   
    List<Subscription> subscriptions = connection.subscriptions ();
    
    Collections.sort (subscriptions, new Comparator<Subscription> ()
    {
      public int compare (Subscription s1, Subscription s2)
      {
        return s1.id > s2.id ? 1 : 0;
      }
    });
    
    int row = 0;
    
    for (Subscription subscription : subscriptions)
    {
      html.append (row % 2 == 0 ? "<tr class='even'>" : "<tr class='odd'>");

      html.append ("<td class='numeric'>${}</td><td class='sub-exp'>", 
                   subscription.id);

      if (!subscription.acceptInsecure)
        html.appendImage ("lock.png", "Only allows secure notifications");

      if (!subscription.keys.isEmpty ())
      {
        html.appendImage ("key.png", "Security keys attached");
        html.append ("(${}) ", num (subscription.keys.size ()));
      }

      html.append ("${}</td><td class='numeric'>${}</td></tr>\n",
                   subscription.expr,
                   num (subscription.notificationCount));
      
      row++;
    }
    
    html.outdent ();
    
    html.append ("</table>\n");
  }

//  private static String guessSubType (String subExpr)
//  {
//    if (subExpr.contains ("Presence-Protocol"))
//      return "Presence";
//    else if (subExpr.contains ("org.tickertape.message"))
//      return "Tickertape";
//    else if (subExpr.contains ("Livespace-Protocol"))
//      return "Livespaces";
//    else if (subExpr.contains ("NEWSGROUPS"))
//      return "Ticker News";
//    else
//      return "Unknown";
//  }

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSSZ");
  
  private String formatConnectionTime (long time)
  {
    return DATE_FORMAT.format (new Date (time));
  }
}
