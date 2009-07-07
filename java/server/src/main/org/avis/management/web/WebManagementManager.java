package org.avis.management.web;

import java.io.Closeable;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.asyncweb.server.BasicServiceContainer;
import org.apache.asyncweb.server.ContainerLifecycleException;
import org.apache.asyncweb.server.HttpServiceHandler;
import org.apache.asyncweb.server.resolver.CompositeResolver;
import org.apache.asyncweb.server.resolver.ExactMatchURIServiceResolver;
import org.apache.asyncweb.server.resolver.PatternMatchResolver;

import org.avis.config.Options;
import org.avis.management.web.pages.ConnectionsPage;
import org.avis.router.CloseListener;
import org.avis.router.Router;

import static org.avis.io.Net.addressesFor;
import static org.avis.logging.Log.warn;
import static org.avis.management.web.WebManagementOptionSet.DEFAULT_PORT;

/**
 * Creates the Avis web management web server.
 * 
 * @author Matthew Phillips
 */
public class WebManagementManager implements Closeable, CloseListener
{
  private BasicServiceContainer container;

  public WebManagementManager (Router router, Options config) 
    throws IOException
  {
    if (config.getString ("Management.Admin-Name").length () == 0 ||
        config.getString ("Management.Admin-Password").length () == 0)
    {
      throw new IllegalArgumentException 
        ("Management.Admin-Name and Management.Admin-Password options must " +
         "be set in order to enable Avis remote management");
    }
    
    this.container = new BasicServiceContainer ();
    HttpServiceHandler handler = new HttpServiceHandler ();
    
    Authoriser authoriser = 
      new Authoriser (config.getString ("Management.Admin-Name"),
                      config.getString ("Management.Admin-Password"));
    
    handler.addHttpService (Authoriser.SERVICE_NAME, authoriser);
    handler.addHttpService ("connections", new ConnectionsPage (router));
    handler.addHttpService 
      ("resources", 
       new UrlHttpService 
         (parentUrl (getClass ().getResource ("resources/main.css"))));
    
    container.addServiceFilter (handler);

    // pages
    ExactMatchURIServiceResolver pageResolver = 
      new ExactMatchURIServiceResolver ();
    pageResolver.addURIMapping ("/", "connections");
    
    // resources
    PatternMatchResolver resourceResolver = new PatternMatchResolver();
    resourceResolver.addPatternMapping ("/.*\\.(css|png|ico)", "resources");
        
    CompositeResolver mainResolver = new CompositeResolver ();

    // allow authorisation resolver first crack at everything
    mainResolver.addResolver (authoriser);
    mainResolver.addResolver (resourceResolver);
    mainResolver.addResolver (pageResolver);
    
    handler.setServiceResolver (mainResolver);

    URL listenUrl = (URL)config.get ("Management.Listen");
    
    AvisMinaTransport transport = 
      new AvisMinaTransport 
        (addressesFor (listenUrl, DEFAULT_PORT), router.ioManager (), 
         listenUrl.getProtocol ().equals ("https"));

    container.addTransport (transport);

    router.addCloseListener (this);
    
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
  
  private URL parentUrl (URL baseUrl)
  {
    String path = baseUrl.getPath ();
    
    int slash = path.lastIndexOf ('/');
    
    if (slash != -1)
      path = path.substring (0, slash + 1);
    
    try
    {
      return new URL (baseUrl.getProtocol (), baseUrl.getHost (), baseUrl.getPort (), path);
    } catch (MalformedURLException ex)
    {
      throw new IllegalArgumentException ("Invalid URL " + path);
    }
  }
  
  public void routerClosing (Router router)
  {
    try
    {
      System.out.println ("web management closing");
      close ();
    } catch (IOException ex)
    {
      warn ("Error while shutting down web management", this, ex);
    }
  }

  public void close () 
    throws IOException
  {
    container.stop ();
  }
}
