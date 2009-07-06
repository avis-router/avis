package org.avis.management.web.pages;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import java.text.SimpleDateFormat;

import org.avis.management.web.HTML;
import org.avis.router.Connection;
import org.avis.router.Router;
import org.avis.router.Subscription;

import static java.lang.System.currentTimeMillis;

import static org.avis.management.web.HTML.num;

public class ConnectionsPage extends Page
{
  private static final Comparator<Connection> CONNECTION_COMPARATOR = 
    new Comparator<Connection> ()
    {
      public int compare (Connection c1, Connection c2)
      {
        return c1.serial - c2.serial;
      }
    };

  private static final Comparator<Subscription> SUBSCRIPTION_COMPARATOR = 
    new Comparator<Subscription> ()
    {
      public int compare (Subscription s1, Subscription s2)
      {
        long diff = s1.id - s2.id;
        
        if (diff < 0)
          return -1;
        else if (diff > 0)
          return 1;
        else
          return 0;
      }
    };
    
  private Router router;
  private long startedAt;

  public ConnectionsPage (Router router)
  {
    this.router = router;
    this.startedAt = currentTimeMillis ();
  }

  @Override
  protected String content ()
  {
    HTML html = new HTML ();
    
    html.appendXHTMLHeader ("Connections - Avis").appendBody ();
   
    html.append 
      ("<p>Router running since: <span class='number'>${}</span></p>\n",
       formatTime (startedAt));
    
    html.append 
       ("<p>Number of connections: <span class='number'>${}</span></p>\n", 
        router.connections ().size ());
 
    html.append ("<p>Total notifications sent/received: " +
    		"<span class='number'>${} / ${}</span></p>\n", 
                 num (router.sentNotificationCount), 
                 num (router.receivedNotificationCount));
    
    html.append 
      ("<table class='client-list' border='1' cellspacing='0'>\n");

    html.indent ();

    html.append 
      ("<tr><th class='numeric'>Client</th>" +
       "<th>Connected</th>" +
       "<th>Host</th>\n" + 
       "<th class='numeric'>Keys (Subscription&nbsp;/&nbsp;Notification)</th>\n" +
       "<th class='numeric'>Subscriptions</th>\n" +
       "<th class='numeric'>Notifications (Received&nbsp;/&nbsp;Sent)</th></tr>\n");
    
    for (Connection connection : 
         sort (router.connections (), CONNECTION_COMPARATOR))
    {
      // TODO add relative connection time
      connection.lockRead ();
      
      try
      {
        if (!connection.isOpen ())
          continue;
        
        html.append 
          ("<tr><td rowspan='2' class='number'>${} (${})</td>" +
               "<td class='date'>${}</td>" +
               "<td>${}</td>" +
               "<td class='number'>${} / ${}</td>" +
               "<td class='number'>${}</td>" +
               "<td class='number'>${} / ${}</td></tr>\n",
           connection.serial, connection.id (),
           formatTime (connection.connectedAt), 
           connection.remoteHost ().getCanonicalHostName (),
           num (connection.subscriptionKeys.size ()),
           num (connection.notificationKeys.size ()),
           num (connection.subscriptions.size ()),
           num (connection.receivedNotificationCount), 
           num (connection.sentNotificationCount));
      
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
   
    int row = 0;
    
    for (Subscription subscription : 
         sort (connection.subscriptions (), SUBSCRIPTION_COMPARATOR))
    {
      html.append (row % 2 == 0 ? "<tr class='even'>" : "<tr class='odd'>");

      html.append ("<td class='number'>${}</td><td class='sub-exp'>", 
                   subscription.id);

      if (!subscription.acceptInsecure)
        html.appendImage ("lock.png", "Only allows secure notifications");

      if (!subscription.keys.isEmpty ())
      {
        html.appendImage ("key.png", "Security keys attached");
        html.append ("(${}) ", num (subscription.keys.size ()));
      }

      html.append ("${}</td><td class='number'>${}</td></tr>\n",
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

  private static String formatTime (long time)
  {
    return new SimpleDateFormat 
      ("yyyy-MM-dd HH:mm:ss.SSSZ").format (new Date (time));
  }
  
  private static <T> Collection<T> sort (List<T> items, 
                                         Comparator<? super T> comparator)
  {
    Collections.sort (items, comparator);
    
    return items;
  }
}
