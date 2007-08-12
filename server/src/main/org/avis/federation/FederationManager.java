package org.avis.federation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.IOException;

import org.avis.common.InvalidURIException;
import org.avis.router.CloseListener;
import org.avis.router.Router;
import org.avis.subscription.parser.ParseException;
import org.avis.util.IllegalOptionException;
import org.avis.util.Options;
import org.avis.util.Pair;

import static org.avis.common.ElvinURI.defaultProtocol;
import static org.avis.federation.FederationClass.parse;
import static org.avis.federation.FederationOptions.splitOptionParam;
import static org.avis.io.Net.addressesFor;
import static org.avis.io.Net.localHostName;
import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;
import static org.avis.util.Text.shortException;
import static org.avis.util.Text.split;

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
    /*
     * Generate (class, URI) pairs first avoid having to dispose of
     * partially-created connectors on exception.
     */
    List<Pair<EwafURI, FederationClass>> items = 
      new ArrayList<Pair<EwafURI, FederationClass>> ();
    
    for (Map.Entry<String, Object> entry : config)
    {
      if (!entry.getKey ().startsWith ("Federation.Connect:"))
        continue;
      
      String [] optionParam = splitOptionParam (entry.getKey ());
      
      FederationClass fedClass = classMap.findOrCreate (optionParam [1]);
      EwafURI uri = uri (entry.getKey (), (String)entry.getValue ());

      if (fedClass.isNull ())
      {
        throw new IllegalOptionException
          (entry.getKey (),
           "No federation subscribe/provide defined: " +
            "this connection cannot import or export any notifications");
      }
      
      items.add (new Pair<EwafURI, FederationClass> (uri, fedClass));
    }
    
    List<FederationConnector> connectors = 
      new ArrayList<FederationConnector> (items.size ());
    
    for (Pair<EwafURI, FederationClass> item : items)
    {
      connectors.add
        (new FederationConnector 
          (router, serverDomain, item.item1, item.item2, config));
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

  private static FederationListener initListener (Router router, 
                                                  String serverDomain,
                                                  FederationClassMap classMap,
                                                  Options config)
  {
    Set<EwafURI> uris = new HashSet<EwafURI> ();
    
    for (String item : split (config.getString ("Federation.Listen"), "\\s+"))
      uris.add (uri ("Federation.Listen", item));
    
    try
    {
      return new FederationListener (router, serverDomain, classMap, 
                                     addressesFor (uris));
    } catch (IOException ex)
    {
      throw new IllegalOptionException ("Federation.Listen", 
                                        shortException (ex));
    }
  }

  private static FederationClassMap initClassMap (Options federationConfig)
  {
    FederationClassMap classMap = 
      new FederationClassMap (new FederationClass (CONST_FALSE, CONST_FALSE));
    
    // federation classes
    for (Map.Entry<String, Object> entry : federationConfig)
    {
      if (!entry.getKey ().startsWith ("Federation."))
        continue;
      
      String [] optionParam = splitOptionParam (entry.getKey ());
      
      if (optionParam [1] == null)
        continue;
      
      String value = (String)entry.getValue ();
      String option = optionParam [0];
      FederationClass fedClass = classMap.findOrCreate (optionParam [1]);
      
      try
      {
        if (option.equals ("Federation.Provide:"))
        {
          fedClass.outgoingFilter = parse (value);
        } else if (option.equals ("Federation.Subscribe:"))
        {
          fedClass.incomingFilter = parse (value);
        } else if (option.equals ("Federation.Listen:"))
        {
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
      } catch (ParseException ex)
      {
        throw new IllegalOptionException
          (entry.getKey (), "Parse error: " + ex.getMessage ());
      }
    }
    
    return classMap;
  }
  
  private static EwafURI uri (String option, String uriString)
    throws IllegalOptionException
  {
    EwafURI uri;
    
    try
    {
      uri = new EwafURI (uriString);
    } catch (InvalidURIException ex)
    {
      throw new IllegalOptionException (option, ex.getMessage ());
    }
    
    if (!uri.protocol.equals (defaultProtocol ()))
    {
      throw new IllegalOptionException
        (option, "Avis only supports " + defaultProtocol () +" protocol: " + 
         uriString);
    }
    
    return uri;
  }
}
