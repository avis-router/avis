package org.avis.net.client;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.URISyntaxException;

import org.avis.Common;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;

import static org.avis.Common.CLIENT_VERSION_MAJOR;
import static org.avis.Common.CLIENT_VERSION_MINOR;
import static org.avis.Common.DEFAULT_PORT;
import static org.avis.util.Collections.join;
import static org.avis.util.Collections.list;

/**
 * An Elvin URI identifying an Elvin router as described in the "Elvin
 * URI Scheme" specification at
 * http://elvin.org/specs/draft-elvin-uri-prelim-02.txt. The most
 * common Elvin URI for a TCP endpoint is of the form:
 * 
 * <pre>
 *   elvin:[version]/[protocol]/hostname[:port];[options]
 *   
 *   protocol: transport,security,marshalling
 *   options:  name1=value1[;name2=value2]*
 * </pre>
 * 
 * <p>
 * 
 * Example URI 1: <code>elvin://localhost:2917</code>.
 * <p>
 * Example URI 2: <code>elvin:4.0/tcp,ssl,xdr/localhost:29170</code>.
 * 
 * @author Matthew Phillips
 */
public final class ElvinURI
{
  private static final List<String> DEFAULT_PROTOCOL =
    list ("tcp", "none", "xdr");
  
  // todo: Mantara Elvin uses "ssl,none,xdr" for secure stack - resolve
  private static final List<String> SECURE_PROTOCOL =
    list ("tcp", "ssl", "xdr");
  
  /**
   * Basic matcher for URI's. Detail parsing is done as a separate pass.
   */
  private static final Pattern URL_PATTERN =
    Pattern.compile ("(\\w+):(?:([^/]+))?/([^/]+)?/([^/].*?)(;.*)?");

  /**
   * The URI string as passed into the constructor.
   */
  public String uriString;
  
  /**
   * Major protocol version. Default {@link Common#CLIENT_VERSION_MAJOR}.
   */
  public int versionMajor;
  
  /**
   * Minor protocol version. Default {@link Common#CLIENT_VERSION_MINOR}.
   */
  public int versionMinor;
  
  /**
   * The stack of protocol modules in (transport,security,marshalling)
   * order. e.g. "tcp", "none", "xdr". See also
   * {@link #defaultProtocol()}
   */
  public List<String> protocol;
  
  /**
   * Host name.
   */
  public String host;
  
  /**
   * Port. Default {@link Common#DEFAULT_PORT}.
   */
  public int port;

  /**
   * URI options: e.g. elvin://host:port;option1=value1;option2=value2
   */
  public Map<String, String> options;

  private int hash;

  /**
   * Create a new instance.
   * 
   * @param uriString The URI.
   * 
   * @throws URISyntaxException if the URI is not valid.
   */
  public ElvinURI (String uriString)
    throws URISyntaxException
  {
    this.uriString = uriString;
    this.versionMajor = CLIENT_VERSION_MAJOR;
    this.versionMinor = CLIENT_VERSION_MINOR;
    this.protocol = DEFAULT_PROTOCOL;
    this.host = null;
    this.port = DEFAULT_PORT;
    this.options = emptyMap ();
    
    parseUrl ();
    
    this.hash = computeHash ();
  }

  @Override
  public String toString ()
  {
    return uriString;
  }
  
  public String toCanonicalString ()
  {
    StringBuilder str = new StringBuilder ();
    
    str.append ("elvin:");
    
    str.append (versionMajor).append ('.').append (versionMinor);
    
    str.append ('/');
    
    join (str, protocol, ',');
    
    str.append ('/').append (host).append (':').append (port);

    // NB: options is a sorted map, canonical order is automatic
    for (Entry<String, String> option : options.entrySet ())
    {
      str.append (';');
      str.append (option.getKey ()).append ('=').append (option.getValue ());
    }
    
    return str.toString ();
  }
  
  @Override
  public int hashCode ()
  {
    return hash;
  }
  
  @Override
  public boolean equals (Object obj)
  {
    return obj instanceof ElvinURI && equals ((ElvinURI)obj);
  }

  public boolean equals (ElvinURI uri)
  {
    return hash == uri.hash &&
           host.equals (uri.host) &&
           port == uri.port &&
           versionMajor == uri.versionMajor &&
           versionMinor == uri.versionMinor && 
           options.equals (uri.options) &&
           protocol.equals (uri.protocol);
  }
  
  private int computeHash ()
  {
    return host.hashCode () ^ port ^ protocol.hashCode ();
  }
  
  private void parseUrl ()
    throws URISyntaxException
  { 
    Matcher matcher = URL_PATTERN.matcher (uriString);
    
    if (!matcher.matches ())
    {
      throw new URISyntaxException (uriString, "Not a valid Elvin URI");
    } else if (!matcher.group (1).equals ("elvin"))
    {
      throw new URISyntaxException (uriString,
                                    "Elvin URI scheme must be \"elvin:\"");
    }
    
    // version
    if (matcher.group (2) != null)
      parseVersion (matcher.group (2));
    
    // protocol
    if (matcher.group (3) != null)
      parseProtocol (matcher.group (3));
    
    // endpoint (host/port)
    parseEndpoint (matcher.group (4));
    
    // options
    if (matcher.group (5) != null)
      parseOptions (matcher.group (5));
  }

  private void parseVersion (String versionExpr)
    throws URISyntaxException
  {
    Matcher versionMatch =
      Pattern.compile ("(\\d+)(?:\\.(\\d+))?").matcher (versionExpr);
    
    if (versionMatch.matches ())
    {
      versionMajor = parseInt (versionMatch.group (1));
      
      if (versionMatch.group (2) != null)
        versionMinor = parseInt (versionMatch.group (2));
    } else
    {
      throw new URISyntaxException (uriString,
                                    "Invalid version string: \"" +
                                    versionExpr + "\"");
    }
  }
  
  private void parseProtocol (String protocolExpr)
    throws URISyntaxException
  {
    Matcher protocolMatch =
      Pattern.compile ("(?:(\\w+),(\\w+),(\\w+))|secure").matcher (protocolExpr);
    
    if (protocolMatch.matches ())
    {
      if (protocolMatch.group (1) != null)
        protocol = asList (protocolExpr.split (","));
      else
        protocol = SECURE_PROTOCOL;
    } else
    {
      throw new URISyntaxException (uriString,
                                    "Invalid protocol: \"" +
                                    protocolExpr + "\"");
    }
  }
  
  private void parseEndpoint (String endpoint)
    throws URISyntaxException
  {
    Matcher endpointMatch =
      Pattern.compile ("([^:]+)(?::(\\d+))?").matcher (endpoint);
    
    if (endpointMatch.matches ())
    {
      host = endpointMatch.group (1);
      
      if (endpointMatch.group (2) != null)
        port = parseInt (endpointMatch.group (2));
    } else
    {
      throw new URISyntaxException (uriString, "Invalid port number");
    }
  }
  
  private void parseOptions (String optionsExpr)
    throws URISyntaxException
  {
    Matcher optionMatch =
      Pattern.compile (";([^=;]+)=([^=;]*)").matcher (optionsExpr);
    
    options = new TreeMap<String, String> ();
    
    int index = 0;
    
    while (optionMatch.lookingAt ())
    {
      options.put (optionMatch.group (1), optionMatch.group (2));
      
      index = optionMatch.end ();
      optionMatch.region (index, optionsExpr.length ());
    }
    
    if (index != optionsExpr.length ())
      throw new URISyntaxException
        (uriString, "Invalid options: \"" + optionsExpr + "\"");
  }

  /**
   * The default URI protocol stack: "tcp", "none", "xdr"
   */
  public static List<String> defaultProtocol ()
  {
    return DEFAULT_PROTOCOL;
  }

  /**
   * The secure URI protocol stack: "tcp", "ssl", "xdr"
   */
  public static List<String> secureProtocol ()
  {
    return SECURE_PROTOCOL;
  }
}
