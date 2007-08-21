package org.avis.federation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import java.io.IOException;

import org.avis.config.IllegalOptionException;
import org.avis.config.Options;
import org.avis.router.CloseListener;
import org.avis.router.Router;
import org.avis.subscription.ast.Node;

import static org.avis.common.ElvinURI.defaultProtocol;
import static org.avis.federation.FederationOptions.getParamOption;
import static org.avis.io.Net.addressesFor;
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
  protected List<Connector> connectors;

  public FederationManager (Router router, Options federationConfig) 
    throws IllegalOptionException
  {
    this.router = router;
    
    String serverDomain = initServerDomain (federationConfig);
    
    classes = initClasses (federationConfig);
      
    connectors = initConnectors (router, serverDomain, classes, 
                                 federationConfig);

    acceptor =
      initAcceptor (router, serverDomain, classes, federationConfig);
    
    router.addCloseListener (this);
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
  
  private static List<Connector> initConnectors
    (Router router,
     String serverDomain,
     FederationClasses classes, 
     Options config)
  {
    Map<String, Object> connect = 
      getParamOption (config, "Federation.Connect");
   
    // check federation classes and URI's make sense
    for (Entry<String, Object> entry : connect.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      if (fedClass.allowsNothing ())
      {
        throw new IllegalOptionException
          ("Federation.Connect[" + entry.getKey () + "]",
           "No federation subscribe/provide defined: " +
            "this connection cannot import or export any notifications");
      }
      
      checkUri ("Federation.Connect[" + entry.getKey () + "]",
                (EwafURI)entry.getValue ());
    }
    
    List<Connector> connectors = 
      new ArrayList<Connector> (connect.size ());
    
    for (Entry<String, Object> entry : connect.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      connectors.add
        (new Connector 
          (router, serverDomain, (EwafURI)entry.getValue (), 
           fedClass, config));
    }
    
    return connectors;
  }

  private static String initServerDomain (Options federationConfig)
  {
    String domain = federationConfig.getString ("Federation.Router-Name");
    
    if (domain.length () == 0)
    {
      try
      {
        domain = localHostName ();
      } catch (IOException ex)
      {
        throw new IllegalOptionException
          ("Federation.Router-Name", 
           "Cannot auto detect default router name, " +
           "please set this manually: " + shortException (ex));
      }
    }
    
    return domain;
  }

  @SuppressWarnings("unchecked")
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
          checkUri ("Federation.Listen", uri);
        
        return new Acceptor (router, serverDomain, classes, 
                             addressesFor (uris));
      } catch (IOException ex)
      {
        throw new IllegalOptionException ("Federation.Listen", 
                                          shortException (ex));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static FederationClasses initClasses (Options federationConfig)
  {
    FederationClasses classes = new FederationClasses ();
    
    Map<String, Object> provide = 
      getParamOption (federationConfig, "Federation.Provide");
    
    for (Entry<String, Object> entry : provide.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      fedClass.outgoingFilter = (Node)entry.getValue ();
    }
    
    Map<String, Object> subscribe = 
      getParamOption (federationConfig, "Federation.Subscribe");
    
    for (Entry<String, Object> entry : subscribe.entrySet ())
    {
      Node incomingFilter = (Node)entry.getValue ();

      /*
       * Cannot sub TRUE right now. When we support 1.1-level
       * federation this will be possible as CONST_TRUE will be
       * &&'d with the current consolidated subscription.
       */ 
      if (incomingFilter == CONST_TRUE)
      {
        throw new IllegalOptionException 
          ("Federation.Subscribe[" + entry.getKey () + "]", 
           "Federation with \"TRUE\" is not currently supported");
      }
      
      classes.define (entry.getKey ()).incomingFilter = incomingFilter;
    }
    
    Map<String, Object> applyClass = 
      getParamOption (federationConfig, "Federation.Apply-Class");
    
    for (Entry<String, Object> entry : applyClass.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      for (String value : (Set<String>)entry.getValue ())
      {
        if (value.startsWith ("@"))
        {
          value = value.substring (1);
          
          if (value.startsWith ("."))
            classes.mapDnsDomain (value.substring (1), fedClass);
          else
            classes.mapHost (value, fedClass);
        } else
        {
          classes.mapServerDomain (value, fedClass);
        }
      }
    }
    
    return classes;
  }
  
  private static void checkUri (String option, EwafURI uri)
  {
    if (!uri.protocol.equals (defaultProtocol ()))
    {
      throw new IllegalOptionException
        (option, "Avis only supports " + defaultProtocol () +" protocol: " + 
         uri);
    }
  }
}
