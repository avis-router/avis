package org.avis.management.web.pages;

import java.util.Comparator;

import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.service.IoService;

import org.avis.common.ElvinURI;
import org.avis.federation.FederationManager;
import org.avis.federation.Link;
import org.avis.management.web.HTML;
import org.avis.management.web.HtmlView;
import org.avis.router.Router;

import static org.avis.federation.FederationClass.unparse;
import static org.avis.federation.FederationManager.federationManagerFor;
import static org.avis.federation.FederationManager.isFederationActivated;
import static org.avis.management.web.HTML.formatBandwidth;
import static org.avis.management.web.HTML.formatBytes;
import static org.avis.management.web.HTML.formatTime;
import static org.avis.management.web.HTML.formatHost;
import static org.avis.management.web.HTML.formatNum;
import static org.avis.util.Collections.sort;

public class FederationView implements HtmlView
{
  private static final Comparator<Link> LINK_COMPARATOR = 
    new Comparator<Link> ()
    {
      public int compare (Link link1, Link link2)
      {
        return compareLongs (link1.session.getCreationTime (), 
                             link2.session.getCreationTime ());
      }
    };

  private Router router;
  private OptionsView optionsView;

  public FederationView (Router router)
  {
    this.router = router;
    this.optionsView = new OptionsView (router.options (), "Federation\\..*");
  }

  public void render (HTML html)
  {    
    if (!isFederationActivated (router))
    {
      html.append ("<p>Federation is not activated</p>");
      
      return;
    }
    
    FederationManager manager = federationManagerFor (router);

    // endpoints
    
    html.append ("<h2>Endpoints</h2>\n");
    
    html.append ("<ul>\n").indent ();
    
    for (ElvinURI uri : manager.listenURIs ())
      html.append ("<li><span  class='net-address'>${}</span> (listen)</li>\n", 
                   uri);
    
    for (ElvinURI uri : manager.connectURIs ())
      html.append ("<li><span class='net-address'>${}</span> (connect)</li>\n", 
                   uri);

    html.outdent ().append ("</ul>\n");
    
    // config
    
    html.append ("<h2>Configuration</h2>\n");
    
    html.append ("<div class='option-list small-scrolling'>\n");
    optionsView.render (html);
    html.append ("</div>\n");
    
    // connections
    
    html.append ("<h2>Connections</h2>\n");
    
    html.append ("<table class='client-list'>\n");
    html.indent ();
    
    html.append ("<thead><tr>" +
    		"<th class='numeric'>Federator</th>" +
    		"<th>Remote Host</th>" +
    		"<th>Class</th>" +
    		"<th>Connected</th>" +
    		"<th class='numeric'>Bytes (In&nbsp;/&nbsp;Out)</th>\n" +
    		"<th class='numeric'>Bandwidth (In&nbsp;/&nbsp;Out&nbsp;B/s)</th>\n" +
    		"<th class='numeric title'>Notifications " +
                  "(In&nbsp;/&nbsp;Out)</th></tr></thead>\n");
    
    for (Link link : sort (manager.links (), LINK_COMPARATOR))
    {
      html.append ("<tr><td class='number' rowspan='2'>${}</td>" +
                   "<td class='net-address'>${}</td>" +
                   "<td>${}</td>" +
                   "<td class='date'>${}</td>" +
                   "<td class='number'>${} /&nbsp;${}</td>" +
                   "<td class='number'>${} /&nbsp;${}</td>" +
                   "<td class='number'>${} / ${}</td></tr>\n",
      		   link.session.getId (), 
      		   formatHost (link.remoteHostAddress), 
      		   link.federationClass.name,
      		   formatTime (link.createdAt),
                   formatBytes (link.session.getReadBytes ()),
                   formatBytes (link.session.getWrittenBytes ()),
                   formatBandwidth (link.session.getReadBytesThroughput ()),
                   formatBandwidth (link.session.getWrittenBytesThroughput ()),
      		   formatNum (link.receivedNotificationCount), 
      		   formatNum (link.sentNotificationCount));
      
      html.append ("<tr><td class='sub-table' colspan='6'>\n");
      
      // detail
      
      html.append ("<table class='prop-list' border='0' cellspacing='0'>\n");
      html.indent ();
      
      IoService service = link.session.getService ();
      ElvinURI uri = router.ioManager ().elvinUriFor (service);
      
      html.append 
        ("<tr><td>${}</td>" +
         "<td>${} <span class='net-address'>${}</span></td></tr>\n", 
         "Initiated by:", service instanceof AbstractIoAcceptor ? 
           "Listener on " : "Connector to ", 
         uri);
      
      addPropListRow (html, "Remote domain:", link.remoteServerDomain);
      addPropListRow (html, "Remote filter:", unparse (link.remotePullFilter));
      
      html.outdent ().append ("</table>\n");
      
      html.append ("</td></tr>\n");
    }
    
    html.outdent ().append ("</table>");
  }

  private static void addPropListRow (HTML html, String title, Object value)
  {
    html.append ("<tr><td>${}</td><td>${}</td></tr>\n", 
                 title, value);
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
