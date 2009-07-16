package org.avis.management.web.pages;

import java.util.Comparator;

import org.avis.common.ElvinURI;
import org.avis.management.web.HTML;
import org.avis.management.web.HtmlView;
import org.avis.router.Connection;
import org.avis.router.Router;
import org.avis.router.Subscription;

import static org.avis.management.web.HTML.formatBandwidth;
import static org.avis.management.web.HTML.formatBytes;
import static org.avis.management.web.HTML.formatHost;
import static org.avis.management.web.HTML.formatNum;
import static org.avis.management.web.HTML.formatTime;
import static org.avis.util.Collections.sort;

public class ClientsView implements HtmlView
{
  private static final Comparator<Connection> CONNECTION_COMPARATOR = 
    new Comparator<Connection> ()
    {
      public int compare (Connection c1, Connection c2)
      {
        return compareLongs (c1.session.getCreationTime (), 
                             c2.session.getCreationTime ());
      }
    };

  private static final Comparator<Subscription> SUBSCRIPTION_COMPARATOR = 
    new Comparator<Subscription> ()
    {
      public int compare (Subscription s1, Subscription s2)
      {
        return compareLongs (s1.id, s2.id);
      }
    };
    
  private Router router;

  public ClientsView (Router router)
  {
    this.router = router;
  }

  public void render (HTML html)
  {
    // endpoints
    
    html.append ("<h2>Endpoints</h2>\n");
    
    html.append ("<ul>\n").indent ();
    
    for (ElvinURI uri : router.listenURIs ())
      html.append ("<li>${}</li>\n", uri);

    html.outdent ().append ("</ul>\n");
    
    // clients
    html.append ("<h2>Clients</h2>\n");
    
    html.append ("<table class='client-list'>\n");

    html.indent ();

    html.append 
      ("<thead><tr><th class='numeric'>Client</th>" +
       "<th>Host</th>\n" + 
       "<th>Connected</th>" +
       "<th class='numeric'>Keys (Sub&nbsp;/&nbsp;Notify)</th>\n" +
       "<th class='numeric'>Subscriptions</th>\n" +
       "<th class='numeric'>Bytes (Out&nbsp;/&nbsp;In)</th>\n" +
       "<th class='numeric'>Bandwidth (Out&nbsp;/&nbsp;In&nbsp;B/s)</th>\n" +
       "<th class='numeric'>Notifications (Out&nbsp;/&nbsp;In)</th></tr></thead>\n");
    
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
          ("<tr><td rowspan='2' class='number'>${}</td>" +
               "<td>${}</td>" +
               "<td class='date'>${}</td>" +
               "<td class='number'>${} /&nbsp;${}</td>" +
               "<td class='number'>${}</td>" +
               "<td class='number'>${} /&nbsp;${}</td>" +
               "<td class='number'>${} /&nbsp;${}</td>" +
               "<td class='number'>${} /&nbsp;${}</td></tr>\n",
           connection.id (),
           formatHost (connection.remoteHost ()),
           formatTime (connection.session.getCreationTime ()), 
           formatNum (connection.subscriptionKeys.size ()),
           formatNum (connection.notificationKeys.size ()),
           formatNum (connection.subscriptions.size ()),
           formatBytes (connection.session.getReadBytes ()),
           formatBytes (connection.session.getWrittenBytes ()),
           formatBandwidth (connection.session.getReadBytesThroughput ()),
           formatBandwidth (connection.session.getWrittenBytesThroughput ()),
           formatNum (connection.receivedNotificationCount), 
           formatNum (connection.sentNotificationCount));
      
        html.append ("<tr><td class='sub-table' colspan='7'>\n");
  
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
        html.append ("(${}) ", formatNum (subscription.keys.size ()));
      }

      html.append ("${}</td><td class='number'>${}</td></tr>\n",
                   subscription.expr,
                   formatNum (subscription.notificationCount));
      
      row++;
    }
    
    html.outdent ();
    
    html.append ("</table>\n");
  }
  
  protected static int compareLongs (long l1, long l2)
  {
    long diff = l1 - l2;
        
    if (diff < 0)
      return -1;
    else if (diff > 0)
      return 1;
    else
      return 0;
  }
}
