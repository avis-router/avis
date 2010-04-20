package org.avis.management.pages;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.avis.logging.Log;
import org.avis.logging.LogEvent;
import org.avis.logging.LogEventBuffer;
import org.avis.management.HTML;
import org.avis.management.HtmlView;
import org.avis.management.HtmlViewWithCustomHeaders;

import static org.avis.logging.Log.eventTypeToString;

public class LogView implements HtmlView, HtmlViewWithCustomHeaders
{
  private static final ThreadLocal<DateFormat> dateFormat =
    new ThreadLocal<DateFormat> ()
  {
    @Override
    protected DateFormat initialValue ()
    {
      return new SimpleDateFormat ("MMM dd HH:mm:ss.SSS");
    }
  };

  private static final String [] EVENT_TYPE_CSS_CLASS = 
    {"log-trace", "log-diagnostic", "log-info",
     "log-warning", "log-alarm", "log-internalerror"};
  
  private LogEventBuffer events;

  public LogView ()
  {
    this.events = new LogEventBuffer (1000);
  }
  
  public void dispose ()
  {
    events.dispose ();
  }
  
  public void addHeaders (HTML html)
  {
    html.append 
      ("<script src='jquery-1.3.js'></script>\n" + 
       "<script src='expander.js'></script>");
  }
  
  public void render (HTML html)
  {
    html.append ("<h2>Log</h2>\n");
    
    html.append ("<table class='log'>\n").indent ();

    html.append 
      ("<thead><tr>" +
       "<th width='1*'>Date</th>" +
       "<th width='0*'>Type</th>\n" + 
       "<th width='3*'>Message</th>" +
       "</tr></thead>\n");
    
    int row = 0;
    
    for (LogEvent event : events)
    {
      String rowClass = row++ % 2 == 0 ? "even" : "odd";
      String from = 
        event.source instanceof Class<?> ? 
          ((Class<?>)event.source).getName () : 
            event.source.getClass ().getName ();
          
      html.append 
        ("<tr class='${}'>" +
         "<td class='date'>${}</td>" +
         "<td class='${}'>${}</td>" +
         "<td>${} (<span class='fixed'>${}</span>)</td>" +
         "</tr>\n", 
         rowClass,
         dateFormat.get ().format (event.time),
         EVENT_TYPE_CSS_CLASS [event.type],
         eventTypeToString (event.type),
         event.message,
         from);
      
      if (event.exception != null)
      {
        html.append 
          ("<tr class='${}'>" +
           "<td colspan='3'>" +
           "<div class='exception'>" +
           "<span class='expand-header'>Exception trace:</span>" +
             "<pre class='exception-trace expand-body'>${}</pre></div></td>" +
           "</tr>\n", rowClass, formatException (event.exception));
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
