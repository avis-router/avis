package org.avis.management.web.pages;

import org.avis.management.web.HTML;
import org.avis.management.web.HtmlView;
import org.avis.router.Router;

import static java.lang.System.getProperty;

import static java.lang.System.currentTimeMillis;

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
    html.append ("<p>Router version: ${}${} (built on ${})</p>\n",
                 getProperty ("avis.router.version", "(unknown)"),
                 getProperty ("avis.release", ""),
                 getProperty ("avis.build-date", "(unknown)"));
    
    html.append 
      ("<p>Router running since: <span class='date'>${}</span></p>\n",
       formatTime (startedAt));
    
    html.append 
      ("<p>Number of clients: <span class='number'>${}</span></p>\n", 
       router.connections ().size ());
  
    html.append ("<p>Total notifications in&nbsp;/&nbsp;out: " +
                "<span class='number'>${}&nbsp;/&nbsp;${}</span></p>\n", 
                 formatNum (router.receivedNotificationCount), 
                 formatNum (router.sentNotificationCount));
 
  }
}
