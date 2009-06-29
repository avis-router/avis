package org.avis.management.web;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.net.URL;

import java.nio.charset.Charset;

import org.apache.asyncweb.common.Cookie;
import org.apache.asyncweb.common.DefaultHttpResponse;
import org.apache.asyncweb.common.HttpRequest;
import org.apache.asyncweb.common.HttpResponseStatus;
import org.apache.asyncweb.common.MutableHttpResponse;
import org.apache.asyncweb.server.BasicServiceContainer;
import org.apache.asyncweb.server.ContainerLifecycleException;
import org.apache.asyncweb.server.HttpService;
import org.apache.asyncweb.server.HttpServiceContext;
import org.apache.asyncweb.server.HttpServiceHandler;
import org.apache.asyncweb.server.resolver.ExactMatchURIServiceResolver;
import org.apache.mina.core.buffer.IoBuffer;

import org.avis.config.Options;
import org.avis.router.Router;

import static org.avis.io.Net.addressesFor;
import static org.avis.management.web.WebManagementOptionSet.DEFAULT_PORT;

/**
 * Creates the Avis web management web server.
 * 
 * @author Matthew Phillips
 */
public class WebManagementManager implements Closeable
{
  private BasicServiceContainer container;

  @SuppressWarnings ("unchecked")
  public WebManagementManager (Router router, Options config) 
    throws IOException
  {
    this.container = new BasicServiceContainer ();
    HttpServiceHandler handler = new HttpServiceHandler ();
    
    handler.addHttpService ("clientListenerExample", new HelloWorld ());
    handler.addHttpService ("connections", new ConnectionsPage (router));
    container.addServiceFilter (handler);

    // Set up a resolver for the HttpServiceHandler
    ExactMatchURIServiceResolver resolver = new ExactMatchURIServiceResolver ();
    resolver.addURIMapping ("/", "connections");
    handler.setServiceResolver (resolver);

    // Create the mina transport and enable the container with it
    Set<URL> listenUrls = (Set<URL>)config.get ("WebManagement.Listen");
    
    AvisMinaTransport transport = 
      new AvisMinaTransport 
        (addressesFor (listenUrls, DEFAULT_PORT), router.ioManager ());

    container.addTransport (transport);

    try
    {
      container.start ();
    } catch (ContainerLifecycleException ex)
    {
      IOException ex2 = 
        new IOException ("Error starting HTTP container: " + ex.getMessage ());
      
      ex2.initCause (ex);
      
      throw ex2;
    }
  }
  
  public void close () 
    throws IOException
  {
    container.stop ();
    container = null;
  }
  
  public static class HelloWorld implements HttpService
  {

    /**
     * Sends the configured message as an HTTP response
     */
    public void handleRequest (HttpServiceContext context)
      throws Exception
    {
      MutableHttpResponse response = new DefaultHttpResponse ();

      StringWriter buf = new StringWriter ();
      PrintWriter writer = new PrintWriter (buf);
      writer
          .println ("<html><body><b>Your message of the day:</b><br/><br/>");
      writer.println ("<h2><i>Hello!</h2></i><br/><br/>");
      writeHeaders (context.getRequest (), writer);
      writer.println ("<br/>");
      writeParameters (context.getRequest (), writer);
      writer.println ("<br/>");
      writeCookies (context.getRequest (), writer);
      writer.flush ();

      IoBuffer bb = IoBuffer.allocate (1024);
      bb.setAutoExpand (true);
      bb.putString (buf.toString (), Charset.forName ("UTF-8")
          .newEncoder ());
      bb.flip ();
      response.setContent (bb);

      response.setHeader ("Pragma", "no-cache");
      response.setHeader ("Cache-Control", "no-cache");
      response.setStatus (HttpResponseStatus.OK);

      context.commitResponse (response);
    }

    /**
     * Writes headers from the request to the specified writer
     * 
     * @param request The request
     * @param writer The writer
     */
    private void writeHeaders (HttpRequest request, PrintWriter writer)
    {
      writer
          .println ("You sent these headers with your request:<br/>");
      writer.println ("<ul>");
      for (String headerName : request.getHeaders ().keySet ())
      {
        String headerValue = request.getHeader (headerName);
        writer.print ("<li>" + headerName + " = " + headerValue
            + "</li>");
      }
      writer.println ("</ul>");
    }

    /**
     * Writes cookies from the request to the specified writer
     * 
     * @param request The request
     * @param writer The writer
     */
    private void writeCookies (HttpRequest request, PrintWriter writer)
    {
      Collection<Cookie> cookies = request.getCookies ();
      if (!cookies.isEmpty ())
      {
        writer
            .println ("You sent these cookies with your request:<br/>");
        writer.println ("<ul>");
        for (Cookie cookie : cookies)
        {
          writer.println ("<li>Name = " + cookie.getName ()
              + " Value = " + cookie.getValue ());
          writer.println (" Path = " + cookie.getPath ()
              + " Version = " + cookie.getVersion () + "</li>");
        }
        writer.println ("</ul>");
      }
    }

    /**
     * Writes request parameters to the specified writer
     * 
     * @param request The request
     * @param writer The writer
     */
    private void writeParameters (HttpRequest request,
                                  PrintWriter writer)
    {
      if (request.getParameters ().size () > 0)
      {
        writer
            .println ("You sent these parameters with your request:<br/><br/>");
        writer.println ("<ul>");

        for (Map.Entry<String, List<String>> entry : request
            .getParameters ().entrySet ())
        {
          writer.println ("<li>");
          writer.print ("'" + entry.getKey () + "' =  ");
          for (Iterator<String> i = entry.getValue ().iterator (); i
              .hasNext ();)
          {
            String value = i.next ();
            writer.print ("'" + value + "'");
            if (i.hasNext ())
            {
              writer.print (", ");
            }
          }
          writer.println ("</li/>");
        }

        writer.println ("</ul>");
      }
    }

    public void start ()
    {
      // Dont care
    }

    public void stop ()
    {
      // Dont care
    }

  }
}
