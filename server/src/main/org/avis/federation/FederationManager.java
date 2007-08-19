package org.avis.federation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import java.io.IOException;

import org.avis.router.CloseListener;
import org.avis.router.Router;
import org.avis.subscription.ast.Node;
import org.avis.util.IllegalOptionException;
import org.avis.util.Options;

import static org.avis.common.ElvinURI.defaultProtocol;
import static org.avis.federation.FederationOptions.getParamOption;
import static org.avis.io.Net.addressesFor;
import static org.avis.io.Net.localHostName;
import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;
import static org.avis.subscription.ast.nodes.Const.CONST_TRUE;
import static org.avis.util.Text.shortException;

/**
 * Constructs the federation setup by reading Federation.*
 * configuration items from a config and setting up federation
 * classes, listeners and connectors to match.
 * 
 * @author Matthew Phillips
 */
public class FederationManager implements CloseListener
{
  protected Router router;
  protected FederationClassMap classMap;
  protected FederationListener listener;
  protected List<FederationConnector> connectors;

  public FederationManager (Router router, Options federationConfig) 
    throws IllegalOptionException
  {
    this.router = router;
    
    String serverDomain = initServerDomain (federationConfig);
    
    classMap = initClassMap (federationConfig);
      
    // connectors
    connectors = initConnectors (router, serverDomain, classMap, 
                                 federationConfig);

    // listener
    listener =
      initListener (router, serverDomain, classMap, federationConfig);
    
    router.addCloseListener (this);
  }
  
  public void routerClosing (Router theRouter)
  {
    close ();
  }
  
  public void close ()
  {
    router.removeCloseListener (this);
    
    if (listener != null)
      listener.close ();
    
    for (FederationConnector connector : connectors)
      connector.close ();
    
    listener = null;
    connectors = null;
  }
  
  public boolean isClosed ()
  {
    return connectors == null;
  }
  
  private static List<FederationConnector> initConnectors
    (Router router,
     String serverDomain,
     FederationClassMap classMap, 
     Options config)
  {
    Map<String, Object> connect = 
      getParamOption (config, "Federation.Connect");
   
    // check federation classes make sense
    for (Entry<String, Object> entry : connect.entrySet ())
    {
      FederationClass fedClass = classMap.findOrCreate (entry.getKey ());
      
      if (fedClass.isNull ())
      {
        throw new IllegalOptionException
          ("Federation.Connect[" + entry.getKey () + "]",
           "No federation subscribe/provide defined: " +
            "this connection cannot import or export any notifications");
      }
      
      checkUri ("Federation.Connect[" + entry.getKey () + "]",
                (EwafURI)entry.getValue ());
    }
    
    List<FederationConnector> connectors = 
      new ArrayList<FederationConnector> (connect.size ());
    
    for (Entry<String, Object> entry : connect.entrySet ())
    {
      FederationClass fedClass = classMap.findOrCreate (entry.getKey ());
      
      connectors.add
        (new FederationConnector 
          (router, serverDomain, (EwafURI)entry.getValue (), fedClass, config));
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
  private static FederationListener initListener (Router router, 
                                                  String serverDomain,
                                                  FederationClassMap classMap,
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
        
        return new FederationListener (router, serverDomain, classMap, 
                                       addressesFor (uris));
      } catch (IOException ex)
      {
        throw new IllegalOptionException ("Federation.Listen", 
                                          shortException (ex));
      }
    }
  }

  private static FederationClassMap initClassMap (Options federationConfig)
  {
    FederationClassMap classMap = 
      new FederationClassMap (new FederationClass (CONST_FALSE, CONST_FALSE));
    
    Map<String, Object> provide = 
      getParamOption (federationConfig, "Federation.Provide");
    
    for (Entry<String, Object> entry : provide.entrySet ())
    {
      FederationClass fedClass = classMap.findOrCreate (entry.getKey ());
      
      fedClass.outgoingFilter = (Node)entry.getValue ();
    }
    
    Map<String, Object> subscribe = 
      getParamOption (federationConfig, "Federation.Subscribe");
    
    for (Entry<String, Object> entry : subscribe.entrySet ())
    {
      Node node = (Node)entry.getValue ();

      /*
       * Cannot sub TRUE right now. When we support 1.1-level
       * federation this will be possible as CONST_TRUE will be
       * &&'d with the current consolidated subscription.
       */ 
      if (node == CONST_TRUE)
      {
        throw new IllegalOptionException 
          ("Federation.Subscribe[" + entry.getKey () + "]", 
           "Federation with \"TRUE\" is not currently supported");
      }
      
      FederationClass fedClass = classMap.findOrCreate (entry.getKey ());
      
      fedClass.incomingFilter = node;
    }
    
    Map<String, Object> listen = 
      getParamOption (federationConfig, "Federation.Apply-Class");
    
    for (Entry<String, Object> entry : listen.entrySet ())
    {
      FederationClass fedClass = classMap.findOrCreate (entry.getKey ());
      String value = (String)entry.getValue ();
      
      if (value.startsWith ("@"))
      {
        value = value.substring (1);
        
        if (value.startsWith ("."))
          classMap.mapDnsDomain (value.substring (1), fedClass);
        else
          classMap.mapHost (value, fedClass);
      } else
      {
        classMap.mapServerDomain (value, fedClass);
      }
    }
    
    return classMap;
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
