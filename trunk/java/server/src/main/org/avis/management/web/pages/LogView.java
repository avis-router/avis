package org.avis.management.web.pages;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.avis.logging.Log;
import org.avis.logging.LogEvent;
import org.avis.logging.LogEventBuffer;
import org.avis.management.web.HTML;
import org.avis.management.web.HtmlView;

import static org.avis.logging.Log.eventTypeToString;
import static org.avis.management.web.HTML.formatTime;

public class LogView implements HtmlView
{
  private LogEventBuffer events;

  public LogView ()
  {
    this.events = new LogEventBuffer (1000);
    
    Log.addLogListener (events);
  }
  
  public void dispose ()
  {
    Log.removeLogListener (events);
  }
  
  public void render (HTML html)
  {
    html.append ("<h2>Log</h2>\n");
    
    html.append ("<table class='log' width='100%'>\n").indent ();

    html.append 
      ("<thead><tr>" +
       "<th width='0*'>Date</th>" +
       "<th width='0*'>Type</th>\n" + 
       "<th width='1*'>Message</th>" +
       "</tr></thead>\n");
    
    for (LogEvent event : events)
    {
      html.append 
        ("<tr>" +
         "<td class='date'>${}</td>" +
         "<td>${}</td>" +
         "<td>${}</td>" +
         "</tr>\n", 
         formatTime (event.time),
         eventTypeToString (event.type),
         event.message);
      
      if (event.exception != null)
      {
        html.append 
        ("<tr>" +
         "<td />" +
         "<td colspan='2'>Exception trace:" +
           "<pre class='exception-trace'>${}</pre></td>" +
         "</tr>\n", formatException (event.exception));
      }
    }
    
    html.outdent ().append ("</table>\n");
  }

  private static String formatException (Throwable exception)
  {
    StringWriter str = new StringWriter ();
    PrintWriter writer = new PrintWriter (str);
    
    Log.printExceptionTrace (writer, exception);
    
    writer.flush ();
    
    return str.toString ();
  }
}
