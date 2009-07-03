package org.avis.management.web;

import java.io.Closeable;
import java.io.IOException;

import java.net.URL;

import org.apache.asyncweb.fileservice.FileHttpService;
import org.apache.asyncweb.server.BasicServiceContainer;
import org.apache.asyncweb.server.ContainerLifecycleException;
import org.apache.asyncweb.server.HttpServiceHandler;
import org.apache.asyncweb.server.resolver.CompositeResolver;
import org.apache.asyncweb.server.resolver.ExactMatchURIServiceResolver;
import org.apache.asyncweb.server.resolver.PatternMatchResolver;

import org.avis.config.Options;
import org.avis.management.web.pages.ConnectionsPage;
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
       new FileHttpService 
         ("/", getClass ().getResource ("resources").getPath ()));
    
    container.addServiceFilter (handler);

    // pages
    ExactMatchURIServiceResolver pageResolver = 
      new ExactMatchURIServiceResolver ();
    pageResolver.addURIMapping ("/", "connections");
    
    // resources
    // TODO need to serve from URL's to support JAR'ing
    PatternMatchResolver resourceResolver = new PatternMatchResolver();
    resourceResolver.addPatternMapping ("/.*\\.(css|png)", "resources");
        
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
  }
}
