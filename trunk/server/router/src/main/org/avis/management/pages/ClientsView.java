package org.avis.management.pages;

import java.util.Comparator;

import org.avis.common.ElvinURI;
import org.avis.management.HTML;
import org.avis.management.HtmlView;
import org.avis.management.HtmlViewWithCustomHeaders;
import org.avis.router.Connection;
import org.avis.router.Router;
import org.avis.router.Subscription;

import static org.avis.management.HTML.formatBandwidth;
import static org.avis.management.HTML.formatBytes;
import static org.avis.management.HTML.formatHost;
import static org.avis.management.HTML.formatNum;
import static org.avis.management.HTML.formatTime;
import static org.avis.router.StatsFilter.readBytesThroughput;
import static org.avis.router.StatsFilter.updateThroughput;
import static org.avis.router.StatsFilter.writtenBytesThroughput;
import static org.avis.util.Collections.sort;

public class ClientsView implements HtmlView, HtmlViewWithCustomHeaders
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

  public void addHeaders (HTML html)
  {
    html.append 
      ("<script src='jquery-1.3.js'></script>\n" + 
       "<script src='expander.js'></script>");
  }
  
  public void render (HTML html)
  {
    // endpoints
    
    html.append ("<h2>Endpoints</h2>\n");
    
    html.append ("<ul>\n").indent ();
    
    for (ElvinURI uri : router.listenURIs ())
      html.append ("<li class='net-address'>${}</li>\n", uri);

    html.outdent ().append ("</ul>\n");
    
    // clients
    html.append ("<h2>Clients</h2>\n");
    
    html.append ("<table class='client-list'>\n");

    html.indent ();

    html.append 
      ("<thead><tr><th class='numeric'>Client</th>" +
       "<th>Remote Host</th>\n" + 
       "<th>Local Endpoint</th>\n" + 
       "<th>Date</th>" +
       "<th class='numeric'>Keys<br/>" +
         "<span class='sub-title'>Sub&nbsp;/&nbsp;Notify</span></th>\n" +
       "<th class='numeric'>Subs</th>\n" +
       "<th class='numeric'>Bytes<br/>" +
         "<span class='sub-title'>Out&nbsp;/&nbsp;In</span></th>\n" +
       "<th class='numeric'>Bytes/Sec<br/>" +
         "<span class='sub-title'>Out&nbsp;/&nbsp;In</span></th>\n" +
       "<th class='numeric'>Notifications<br/>" +
         "<span class='sub-title'>Out&nbsp;/&nbsp;In</span></th></tr></thead>\n");
    
    for (Connection connection : 
         sort (router.connections (), CONNECTION_COMPARATOR))
    {
      // TODO add relative connection time
      connection.lockRead ();
      
      try
      {
        if (!connection.isOpen ())
          continue;
        
        updateThroughput (connection.session);
        
        html.append 
          ("<tr><td rowspan='2' class='number'>${}</td>" +
               "<td class='net-address'>${}</td>" +
               "<td class='net-address'>${}</td>" +
               "<td class='date'>${}</td>" +
               "<td class='number'>${} /&nbsp;${}</td>" +
               "<td class='number'>${}</td>" +
               "<td class='number'>${} /&nbsp;${}</td>" +
               "<td class='number'>${} /&nbsp;${}</td>" +
               "<td class='number'>${} /&nbsp;${}</td></tr>\n",
           connection.id (),
           formatHost (connection.remoteHost ()),
           router.ioManager ().elvinUriFor (connection.session.getService ()),
           formatTime (connection.session.getCreationTime ()), 
           formatNum (connection.subscriptionKeys.size ()),
           formatNum (connection.notificationKeys.size ()),
           formatNum (connection.subscriptions.size ()),
           formatBytes (connection.session.getReadBytes ()),
           formatBytes (connection.session.getWrittenBytes ()),
           formatBandwidth (readBytesThroughput (connection.session)),
           formatBandwidth (writtenBytesThroughput (connection.session)),
           formatNum (connection.receivedNotificationCount), 
           formatNum (connection.sentNotificationCount));

        html.append ("<tr><td class='sub-table' colspan='8'>\n");
        
        if (!connection.options.options ().isEmpty ())
        {
          html.append ("<h2 class='expand-header'>Options</h2>");
          html.append ("<div class='expand-body'>\n");
          html.indent ();
          new OptionsView (connection.options, false).render (html);
          html.outdent ();
          html.append ("</div>\n");
        }
        
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
    html.append ("<h2 class='expand-header'>Subscriptions</h2>");
    html.append ("<table class='sub-list expand-body'>\n");

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
