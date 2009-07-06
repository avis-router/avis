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
  
  private URL baseUrl;
  private SimpleDateFormat httpDateFormatter;

  public UrlHttpService (URL baseUrl)
  {
    this.baseUrl = baseUrl;
    // TODO this should be thread-local
    this.httpDateFormatter = 
      new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    httpDateFormatter.setTimeZone (getTimeZone ("GMT"));
  }

  public void handleRequest (HttpServiceContext context)
    throws Exception
  {
    System.out.println ("URL = " + context.getRequest ().getRequestUri ());
    
    MutableHttpResponse response = new DefaultHttpResponse ();
    
    String path = 
      context.getRequest ().getRequestUri ().getPath ().substring (1);
    
    // NOTE "new URL (baseUrl, path)" does not work for "jar:file:" URL's
    URL url = 
      new URL (baseUrl.getProtocol (), baseUrl.getHost (), baseUrl.getPort (), 
               baseUrl.getPath () + path);
    
    System.out.println ("base URL = " + baseUrl);
    System.out.println ("resolved URL = " + url);

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
        response.setHeader 
          ("Last-Modified", 
           httpDateFormatter.format (new Date (lastModified)));
        response.setHeader 
          ("Expires", 
           httpDateFormatter.format 
             (new Date (currentTimeMillis () + MAX_CACHE_AGE)));
      }
  
      String contentType = guessContentType (path);
  
      if (contentType != null)
        response.setHeader ("Content-Type", contentType);
      
      System.out.println ("content type = " + contentType);
        
      try
      {
        IoBuffer urlContent = loadUrl (connection);
        
        response.setContent (urlContent);
        response.setStatus (OK);
      } catch (FileNotFoundException ex)
      {
        response.setStatus (NOT_FOUND);
      }
    }
    
    context.commitResponse (response);
  }

  private Date getIfModifiedDate (HttpRequest request)
  {
    String ifModifiedSince = request.getHeader ("If-Modified-Since");
    
    if (ifModifiedSince != null)
    {
      try
      {
        return httpDateFormatter.parse (ifModifiedSince);
      } catch (ParseException ex)
      {
        diagnostic ("Invalid HTTP date from client", this, ex);
      }      
    }
    
    return null;
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

  private IoBuffer loadUrl (URLConnection connection) 
    throws IOException
  {
    InputStream in = connection.getInputStream ();
    
    System.out.println ("last modified = " + connection.getLastModified ());
    
    try
    {
      IoBuffer buffer = IoBuffer.allocate (connection.getContentLength ());
      
      byte [] bytes = new byte [8192];
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
