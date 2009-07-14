package org.avis.management.web.pages;

import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceStatistics;

import org.avis.federation.Connector;
import org.avis.federation.FederationManager;
import org.avis.management.web.HTML;
import org.avis.management.web.HtmlView;
import org.avis.router.Router;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;

import static org.avis.federation.FederationManager.federationManagerFor;
import static org.avis.federation.FederationManager.isFederationActivated;
import static org.avis.management.web.HTML.formatNum;
import static org.avis.management.web.HTML.formatTime;

public class OverviewView implements HtmlView
{
  private Router router;
  private long startedAt;

  public OverviewView (Router router)
  {
    this.router = router;
    this.startedAt = currentTimeMillis ();
  }

  public void render (HTML html)
  {
    html.append ("<h2>Router</h2>\n");
    
    html.append ("<table>\n").indent ();
    
    html.append ("<tr><td>Router version:</td>" +
    		"<td class='number'>Avis ${}${} (built on ${})</td></tr>\n",
                 getProperty ("avis.router.version", "(unknown)"),
                 getProperty ("avis.release", ""),
                 getProperty ("avis.build-date", "(unknown)"));
    
    html.append 
      ("<tr><td>Router running since:</td><td class='date'>${}</td></tr>\n",
       formatTime (startedAt));
    
    html.outdent ().append ("</table>\n");
    
    html.append ("<h2>Statistics</h2>\n");
    
    html.append ("<table>\n").indent ();
    html.append 
      ("<tr><td>Number of clients:</td><td class='number'>${}</td></tr>\n", 
       router.connections ().size ());
  
    html.append ("<tr><td>Total notifications in&nbsp;/&nbsp;out:</td>" +
                "<td class='number'>${}&nbsp;/&nbsp;${}</td></tr>\n", 
                 formatNum (router.receivedNotificationCount), 
                 formatNum (router.sentNotificationCount));
    
    long [] bytesInOut = calcBytesInOut ();
    
    html.append ("<tr><td>Total bytes in&nbsp;/&nbsp;out:</td>" +
                "<td class='number'>${}&nbsp;/&nbsp;${}</td></tr>\n", 
                 formatNum (bytesInOut [0]), 
                 formatNum (bytesInOut [1]));
    
    html.outdent ().append ("</table>\n");
  }

  private long [] calcBytesInOut ()
  {
    long in = 0;
    long out = 0;
    
    for (IoService service : router.ioAcceptors ())
    {
      in += service.getStatistics ().getReadBytes ();
      out += service.getStatistics ().getWrittenBytes ();
    }
    
    if (isFederationActivated (router))
    {
      FederationManager manager = federationManagerFor (router);
      
      if (manager.acceptor () != null)
      {
        for (IoService service : manager.acceptor ().ioAcceptors ())
        {
          in += service.getStatistics ().getReadBytes ();
          out += service.getStatistics ().getWrittenBytes ();
        }
      }

      for (Connector connector : manager.connectors ())
      {
        IoServiceStatistics stats = connector.ioConnector ().getStatistics ();
        
        in += stats.getReadBytes ();
        out += stats.getWrittenBytes ();
      }
    }
    
    return new long [] {in ,out};
  }
}
