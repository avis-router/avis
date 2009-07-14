package org.avis.federation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import java.io.IOException;

import java.lang.management.ManagementFactory;

import org.avis.config.Options;
import org.avis.router.CloseListener;
import org.avis.router.Router;
import org.avis.subscription.ast.Node;
import org.avis.util.IllegalConfigOptionException;

import static java.lang.Integer.toHexString;
import static java.lang.System.identityHashCode;
import static java.util.Collections.emptySet;

import static org.avis.common.ElvinURI.defaultProtocol;
import static org.avis.common.ElvinURI.secureProtocol;
import static org.avis.io.Net.localHostName;
import static org.avis.subscription.ast.nodes.Const.CONST_TRUE;
import static org.avis.util.Text.shortException;

/**
 * Constructs the federation setup by reading Federation.*
 * configuration items from a config and setting up federation
 * classes, listeners and acceptors to match.
 * 
 * @author Matthew Phillips
 */
public class FederationManager implements CloseListener
{
  protected Router router;
  protected FederationClasses classes;
  protected Acceptor acceptor;
  protected Collection<Connector> connectors;

  public FederationManager (Router router, Options federationConfig) 
    throws IllegalConfigOptionException, IOException
  {
    this.router = router;
    
    String serverDomain = initServerDomain (federationConfig);
    
    classes = initClasses (federationConfig);
    
    initAddAttributes (federationConfig, classes);
      
    connectors = initConnectors (router, serverDomain, classes, 
                                 federationConfig);

    acceptor =
      initAcceptor (router, serverDomain, classes, federationConfig);
    
    router.addCloseListener (this);
  }
  
  public static boolean isFederationActivated (Router router)
  {
    return router.options ().getBoolean ("Federation.Activated");
  }
  /**
   * Find the federation manager for a router.
   * 
   * @param router The router.
   * @return The last federation manager created for the router.
   * 
   * @throws IllegalArgumentException if no manager found.
   */
  public static FederationManager federationManagerFor (Router router)
  {
    for (Object listener : router.closeListeners ())
    {
      if (listener instanceof FederationManager)
        return (FederationManager)listener;
    }
    
    throw new IllegalArgumentException ("No federation manager");
  }
  
  public Acceptor acceptor ()
  {
    return acceptor;
  }
  
  public Collection<Connector> connectors ()
  {
    return connectors;
  }
  
  public List<Link> links ()
  {
    ArrayList<Link> links = new ArrayList<Link> ();
    
    if (acceptor != null)
      links.addAll (acceptor.links ());
    
    for (Connector connector : connectors)
    {
      Link link = connector.link ();
      
      if (link != null)
        links.add (link);
    }
    
    return links;
  }

  public Set<EwafURI> listenURIs ()
  {
    if (acceptor == null)
      return emptySet ();
    else
      return acceptor.listenURIs ();
  }
  
  public Set<EwafURI> connectURIs ()
  {
    HashSet<EwafURI> uris = new HashSet<EwafURI> ();
    
    for (Connector connector : connectors)
      uris.add (connector.uri);
    
    return uris;
  }

  public void routerClosing (Router theRouter)
  {
    close ();
  }
  
  public void close ()
  {
    router.removeCloseListener (this);
    
    if (acceptor != null)
      acceptor.close ();
    
    for (Connector connector : connectors)
      connector.close ();
    
    acceptor = null;
    connectors = null;
  }
  
  public boolean isClosed ()
  {
    return connectors == null;
  }
  
  @SuppressWarnings ("unchecked")
  private static Collection<Connector> initConnectors
    (Router router,
     String serverDomain,
     FederationClasses classes, 
     Options config) 
     throws IllegalConfigOptionException, IOException
  {
    Map<String, Set<EwafURI>> connect = 
      (Map<String, Set<EwafURI>>)config.getParamOption ("Federation.Connect");
   
    // check federation classes and URI's used in Federation.Connect make sense
    for (Entry<String, Set<EwafURI>> entry : connect.entrySet ())
    {
      FederationClass fedClass = classes.find (entry.getKey ());
      
      if (fedClass == null)
      {
        throw new IllegalConfigOptionException
          ("Federation.Connect[" + entry.getKey () + "]",
           "Undefined federation class \"" + entry.getKey () + "\"");
      } else if (fedClass.allowsNothing ())
      {
        throw new IllegalConfigOptionException
          ("Federation.Connect[" + entry.getKey () + "]",
           "No federation subscribe/provide defined: " +
            "this connection would not import or export any notifications");
      }
      
      for (EwafURI uri : entry.getValue ())
        checkProtocol ("Federation.Connect[" + entry.getKey () + "]", uri);
    }
    
    Collection<Connector> connectors = 
      new ArrayList<Connector> (connect.size ());
    
    for (Entry<String, Set<EwafURI>> entry : connect.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      for (EwafURI uri : entry.getValue ())
      {
        connectors.add
          (new Connector (router, serverDomain, uri, fedClass, config));
      }
    }
    
    return connectors;
  }

  private String initServerDomain (Options config)
  {
    String domain = config.getString ("Federation.Router-Name");
    
    if (domain.length () == 0)
    {
      try
      {
        domain = defaultServerDomain ();
      } catch (IOException ex)
      {
        throw new IllegalConfigOptionException
          ("Federation.Router-Name", 
           "Cannot auto detect default router name, " +
           "please set this manually: " + shortException (ex));
      }
    }
    
    return domain;
  }

  /**
   * Do the best we can to guess a good server domain based on
   * identity hashcode.PID.hostname.
   */
  private String defaultServerDomain ()
    throws IOException
  {
    String instanceId = toHexString (identityHashCode (this));
    String runtimeName = ManagementFactory.getRuntimeMXBean ().getName ();
 
    /*
     * RuntimeMXBean.getName () returns pid@hostname on many VM's: if
     * it looks like this is the case use it, otherwise fall back on
     * hashcode + hostname.
     */
    if (runtimeName.matches ("\\d+@.+"))
      return instanceId + '.' + runtimeName;
    else
      return instanceId + '@' + localHostName ();
  }

  @SuppressWarnings ("unchecked")
  private static Acceptor initAcceptor (Router router, 
                                        String serverDomain,
                                        FederationClasses classes,
                                        Options config)
  {
    Set<EwafURI> uris = (Set<EwafURI>)config.get ("Federation.Listen");
    
    if (uris.isEmpty ())
    {
      return null;
    } else
    {
      try
      {
        for (EwafURI uri : uris)
          checkProtocol ("Federation.Listen", uri);
        
        return new Acceptor (router, serverDomain, classes, uris, config);
      } catch (IOException ex)
      {
        throw new IllegalConfigOptionException 
          ("Federation.Listen", shortException (ex));
      }
    }
  }

  @SuppressWarnings ("unchecked")
  private static FederationClasses initClasses (Options config)
  {
    FederationClasses classes = new FederationClasses ();
    
    Map<String, Node> provide = 
      (Map<String, Node>)config.getParamOption ("Federation.Provide");
    
    for (Entry<String, Node> entry : provide.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      fedClass.outgoingFilter = entry.getValue ();
    }
    
    Map<String, Node> subscribe = 
      (Map<String, Node>)config.getParamOption ("Federation.Subscribe");
    
    for (Entry<String, Node> entry : subscribe.entrySet ())
    {
      Node incomingFilter = entry.getValue ();

      /*
       * Cannot sub TRUE right now. When we support 1.1-level
       * federation this will be possible as CONST_TRUE will be
       * &&'d with the current consolidated subscription.
       */ 
      if (incomingFilter == CONST_TRUE)
      {
        throw new IllegalConfigOptionException 
          ("Federation.Subscribe[" + entry.getKey () + "]", 
           "Federation with TRUE is not currently supported");
      }
      
      classes.define (entry.getKey ()).incomingFilter = incomingFilter;
    }
    
    Map<String, ?> applyClass = 
      config.getParamOption ("Federation.Apply-Class");
    
    for (Entry<String, ?> entry : applyClass.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      for (String hostPatterns : (Set<String>)entry.getValue ())
      {
        // compatibility with Avis 1.1
        if (hostPatterns.contains ("@"))
          hostPatterns = hostPatterns.replaceAll ("@", "");
        
        classes.map (hostPatterns, fedClass);
      }
    }
    
    classes.setDefaultClass 
      (classes.define 
        (config.getString ("Federation.Default-Class")));
    
    return classes;
  }
  
  @SuppressWarnings ("unchecked")
  private static void initAddAttributes (Options config,
                                         FederationClasses classes)
  {
    Map<String, ?> incoming = 
      config.getParamOption ("Federation.Add-Incoming-Attribute");
    
    for (Entry<String, ?> entry : incoming.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      fedClass.incomingAttributes = (Map<String, Object>)entry.getValue ();
    }
    
    Map<String, ?> outgoing = 
      config.getParamOption ("Federation.Add-Outgoing-Attribute");
    
    for (Entry<String, ?> entry : outgoing.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      fedClass.outgoingAttributes = (Map<String, Object>)entry.getValue ();
    }
  }
  
  private static void checkProtocol (String option, EwafURI uri)
  {
    if (!uri.protocol.equals (defaultProtocol ()) && 
        !uri.protocol.equals (secureProtocol ()))
    {
      throw new IllegalConfigOptionException
        (option, "Avis only supports " + defaultProtocol () + 
         " and " + secureProtocol () + " protocols: " + uri);
    }
  }
}
