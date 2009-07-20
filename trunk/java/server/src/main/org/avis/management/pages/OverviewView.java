package org.avis.management.pages;

import java.util.Date;
import java.util.TimeZone;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.mina.core.service.IoService;

import org.avis.federation.Connector;
import org.avis.federation.FederationManager;
import org.avis.management.HTML;
import org.avis.management.HtmlView;
import org.avis.router.Router;

import static java.lang.Runtime.getRuntime;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;

import static org.avis.federation.FederationManager.federationManagerFor;
import static org.avis.federation.FederationManager.isFederationActivated;
import static org.avis.management.HTML.formatNum;
import static org.avis.management.HTML.formatTime;

public class OverviewView implements HtmlView
{
  private static final ThreadLocal<DateFormat> offsetFormat =
    new ThreadLocal<DateFormat> ()
  {
    @Override
    protected DateFormat initialValue ()
    {
      return new SimpleDateFormat ("Z");
    }
  };
  
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
    
    html.append ("<tr><td>Version:</td>" +
    		 "<td class='fixed'>Avis ${}${} (built on ${})</td></tr>\n",
                 getProperty ("avis.router.version", "(unknown)"),
                 getProperty ("avis.release", ""),
                 getProperty ("avis.build-date", "(unknown)"));
    
    html.append 
      ("<tr><td>Running since:</td><td class='date'>${}</td></tr>\n",
       formatTime (startedAt));
    
    TimeZone timezone = TimeZone.getDefault ();
    Date now = new Date ();
    
    html.append ("<tr><td>Timezone:</td>" +
    		     "<td><span class='number'>${}</span> ${}</td></tr>\n", 
                 offsetFormat.get ().format (now),
                 timezone.getDisplayName (timezone.inDaylightTime (now), 
                                          TimeZone.LONG));
    html.append ("<tr><td>Memory:</td>" +
                 "<td><span class='number'>${}</span> free / " +
                 "<span class='number'>${}</span> total / " +
                 "<span class='number'>${}</span> max</td></tr>\n", 
                 formatNum (getRuntime ().freeMemory ()),
                 formatNum (getRuntime ().totalMemory ()),
                 formatNum (getRuntime ().maxMemory ()));
    
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
        in += connector.ioConnector ().getStatistics ().getReadBytes ();
        out += connector.ioConnector ().getStatistics ().getWrittenBytes ();
      }
    }
    
    return new long [] {in, out};
  }
}
