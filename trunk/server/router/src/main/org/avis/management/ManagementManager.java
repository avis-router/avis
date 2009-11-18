package org.avis.management;

import java.util.Collection;

import java.io.Closeable;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.asyncweb.server.BasicServiceContainer;
import org.apache.asyncweb.server.ContainerLifecycleException;
import org.apache.asyncweb.server.HttpServiceHandler;
import org.apache.asyncweb.server.resolver.CompositeResolver;
import org.apache.asyncweb.server.resolver.PatternMatchResolver;

import org.avis.config.Options;
import org.avis.management.pages.ClientsView;
import org.avis.management.pages.ConfigurationView;
import org.avis.management.pages.FederationView;
import org.avis.management.pages.LogView;
import org.avis.management.pages.OverviewView;
import org.avis.router.CloseListener;
import org.avis.router.Router;

import static java.util.Collections.singleton;

import static org.avis.io.Net.addressesFor;
import static org.avis.logging.Log.warn;
import static org.avis.management.ManagementOptionSet.DEFAULT_PORT;

/**
 * Creates the Avis web management web server.
 * 
 * @author Matthew Phillips
 */
public class ManagementManager implements Closeable, CloseListener
{
  private Options config;
  private BasicServiceContainer container;

  public ManagementManager (Router router, Options config) 
    throws IOException
  {
    this.config = config;
    
    String adminName = config.getString ("Management.Admin-Name");
    String adminPassword = config.getString ("Management.Admin-Password");
    
    if (adminName.length () == 0 || adminPassword.length () == 0)
    {
      throw new IllegalArgumentException 
        ("Management.Admin-Name and Management.Admin-Password options must " +
         "be set in order to enable Avis remote management");
    }
    
    this.container = new BasicServiceContainer ();
    HttpServiceHandler handler = new HttpServiceHandler ();
    
    Authoriser authoriser = new Authoriser (adminName, adminPassword);
    RedirectRootToDefault redirectDefault = new RedirectRootToDefault ("overview");
    
    handler.addHttpService (Authoriser.SERVICE_NAME, authoriser);
    handler.addHttpService (RedirectRootToDefault.SERVICE_NAME, redirectDefault);
    
    handler.addHttpService 
      ("overview", 
       new StandardPage ("Overview", new OverviewView (router)));
    handler.addHttpService 
      ("clients", 
       new StandardPage ("Clients", new ClientsView (router)));
    handler.addHttpService 
      ("federation", 
       new StandardPage ("Federation", new FederationView (router)));
    handler.addHttpService 
      ("configuration", 
       new StandardPage ("Configuration", 
                         new ConfigurationView (router.options ())));
    handler.addHttpService 
      ("log", new StandardPage ("Log", new LogView ()));

    handler.addHttpService 
      ("resources", 
       new ResourceHttpService 
         (parentUrl (getClass ().getResource ("resources/screen.css"))));
    
    container.addServiceFilter (handler);

    // resolve resources
    PatternMatchResolver resourceResolver = new PatternMatchResolver ();
    resourceResolver.addPatternMapping ("/.*\\.(css|png|ico|js)$", "resources");
        
    CompositeResolver mainResolver = new CompositeResolver ();

    // allow authorisation resolver first crack at everything
    mainResolver.addResolver (authoriser);
    mainResolver.addResolver (StandardPage.RESOLVER);
    mainResolver.addResolver (resourceResolver);
    mainResolver.addResolver (redirectDefault);
    
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
  
  public void routerClosing (Router router)
  {
    try
    {
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
  
  public Collection<URL> listenURLs ()
  {
    return singleton ((URL)config.get ("Management.Listen"));
  }

  /**
   * Find the web management manager for a router.
   * 
   * @param router The router.
   * @return The last manager created for the router.
   * 
   * @throws IllegalArgumentException if no manager found.
   */
  public static ManagementManager webManagementManagerFor (Router router)
  {
    for (Object listener : router.closeListeners ())
    {
      if (listener instanceof ManagementManager)
        return (ManagementManager)listener;
    }
    
    throw new IllegalArgumentException ("No web management manager");
  }


  private static URL parentUrl (URL baseUrl)
  {
    String path = baseUrl.getPath ();
    
    int slash = path.lastIndexOf ('/');
    
    if (slash != -1)
      path = path.substring (0, slash + 1);
    
    try
    {
      return new URL (baseUrl.getProtocol (), baseUrl.getHost (), 
                      baseUrl.getPort (), path);
    } catch (MalformedURLException ex)
    {
      throw new IllegalArgumentException ("Invalid URL " + path);
    }
  }
}