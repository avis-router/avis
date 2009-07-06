package org.avis.management.web;

import java.util.Date;
import java.util.Locale;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.URLConnection;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.asyncweb.common.DefaultHttpResponse;
import org.apache.asyncweb.common.HttpRequest;
import org.apache.asyncweb.common.MutableHttpResponse;
import org.apache.asyncweb.server.HttpService;
import org.apache.asyncweb.server.HttpServiceContext;
import org.apache.mina.core.buffer.IoBuffer;

import static java.lang.Math.min;

import static java.lang.System.currentTimeMillis;
import static java.util.TimeZone.getTimeZone;

import static org.apache.asyncweb.common.HttpResponseStatus.NOT_FOUND;
import static org.apache.asyncweb.common.HttpResponseStatus.NOT_MODIFIED;
import static org.apache.asyncweb.common.HttpResponseStatus.OK;

import static org.avis.logging.Log.diagnostic;

/**
 * Serves resources under a given URL to clients.
 * 
 * @author Matthew Phillips
 */
public class UrlHttpService implements HttpService
{
  private static final int MAX_CACHE_AGE = 60 * 60 * 24 * 1000;

  private static final ThreadLocal<SimpleDateFormat> HTTP_DATE_FORMAT = 
    new ThreadLocal<SimpleDateFormat> ()
  {
    protected SimpleDateFormat initialValue ()
    {
      SimpleDateFormat format = 
        new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
      
      format.setTimeZone (getTimeZone ("GMT"));
      
      return format;
    }
  };
  
  private URL baseUrl;
  
  public UrlHttpService (URL baseUrl)
  {
    this.baseUrl = baseUrl;
  }

  public void handleRequest (HttpServiceContext context)
    throws Exception
  {
    MutableHttpResponse response = new DefaultHttpResponse ();
    
    String path = 
      context.getRequest ().getRequestUri ().getPath ().substring (1);
    
    // NOTE "new URL (baseUrl, path)" does not work for "jar:file:" URL's
    URL url = 
      new URL (baseUrl.getProtocol (), baseUrl.getHost (), baseUrl.getPort (), 
               baseUrl.getPath () + path);
    
    // bounce any requests that resolve outside of base URL 
    if (!url.getPath ().startsWith (baseUrl.getPath ()))
    {
      response.setStatus (NOT_FOUND);
      context.commitResponse (response);
      
      return;
    }
      
    URLConnection connection = url.openConnection ();

    long lastModified = connection.getLastModified ();
    
    Date ifModifedSince = getIfModifiedDate (context.getRequest ());

    if (ifModifedSince != null && lastModified <= ifModifedSince.getTime ())
    {
      response.setStatus (NOT_MODIFIED);
    } else
    {
      if (lastModified != 0)
      {
        response.setHeader ("Last-Modified", formatDate (lastModified));
        response.setHeader 
          ("Expires", formatDate (currentTimeMillis () + MAX_CACHE_AGE));
      }
  
      String contentType = guessContentType (path);
  
      if (contentType != null)
        response.setHeader ("Content-Type", contentType);
      
      try
      {
        response.setContent (contentsOf (connection));
        response.setStatus (OK);
      } catch (FileNotFoundException ex)
      {
        response.setStatus (NOT_FOUND);
      }
    }
    
    context.commitResponse (response);
  }

  private static Date getIfModifiedDate (HttpRequest request)
  {
    String ifModifiedSince = request.getHeader ("If-Modified-Since");
    
    if (ifModifiedSince != null)
    {
      try
      {
        return parseDate (ifModifiedSince);
      } catch (ParseException ex)
      {
        diagnostic ("Invalid HTTP date from client", UrlHttpService.class, ex);
      }      
    }
    
    return null;
  }
  
  private static String formatDate (long date)
  {
    return HTTP_DATE_FORMAT.get ().format (new Date (date));
  }

  private static Date parseDate (String date) 
    throws ParseException
  {
    return HTTP_DATE_FORMAT.get ().parse (date);
  }

  private static String guessContentType (String path)
  {
    String type = URLConnection.guessContentTypeFromName (path);
    
    if (type == null)
    {
      if (path.endsWith (".css"))
        return "text/css";
      else if (path.endsWith (".ico"))
        return "image/vnd.microsoft.icon";
    }
    
    return type;
  }

  private static IoBuffer contentsOf (URLConnection connection) 
    throws IOException
  {
    InputStream in = connection.getInputStream ();
    
    try
    {
      IoBuffer buffer = IoBuffer.allocate (connection.getContentLength ());
      
      byte [] bytes = new byte [min (connection.getContentLength (), 8192)];
      int bytesRead;
      
      while ((bytesRead = in.read (bytes)) != -1)
        buffer.put (bytes, 0, bytesRead);
      
      buffer.flip ();
      
      return buffer;
    } finally
    {
      in.close ();
    }
  }

  public void start ()
  {
    // zip
  }

  public void stop ()
  {
    // zip
  }
}
