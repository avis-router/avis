package org.avis.net.client;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.URISyntaxException;

import org.avis.Common;

import static java.lang.Integer.parseInt;

import static org.avis.Common.CLIENT_VERSION_MAJOR;
import static org.avis.Common.CLIENT_VERSION_MINOR;
import static org.avis.Common.DEFAULT_PORT;
import static org.avis.util.Collections.set;

/**
 * An Elvin URI identifying an Elvin router as described in the "Elvin
 * URI Scheme" specification at
 * http://elvin.org/specs/draft-elvin-uri-prelim-02.txt. The most
 * common Elvin URI for a TCP endpoint is of the form:
 * 
 * <pre>
 *   elvin:[version]/[protocol]/hostname[:port] [options]
 * </pre>
 * 
 * <p>
 * 
 * Example URI 1: <code>elvin://localhost:2917</code>.
 * <p>
 * Example URI 2: <code>elvin:4.0/tcp,xdr,ssl/localhost:29170</code>.
 * 
 * @author Matthew Phillips
 */
public class ElvinURI
{
  private static final Set<String> DEFAULT_PROTOCOL =
    set ("tcp", "none", "xdr");
  
  /**
   * Basic matcher for URI's. Detail parsing is done as a separate pass.
   */
  private static final Pattern URL_PATTERN =
    Pattern.compile ("(\\w+):(?:([^/]+))?/([^/]+)?/([^/].*)");
  
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
   * The set of protocol modules. e.g. "tcp", "xdr", "ssl". See also
   * {@link #defaultProtocol()}
   */
  public Set<String> protocol;
  
  /**
   * Host name.
   */
  public String host;
  
  /**
   * Port. Default {@link Common#DEFAULT_PORT}.
   */
  public int port;

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
    
    parseUrl ();
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
      Pattern.compile ("(\\w+)(\\s*,\\s*(\\w+))*").matcher (protocolExpr);
    
    if (protocolMatch.matches ())
    {
      protocol = new HashSet<String> ();
      
      String [] items = protocolExpr.split ("\\s*,\\s*");
      
      for (int i = 0; i < items.length; i++)
        protocol.add (items [i]);
    } else
    {
      throw new URISyntaxException (uriString,
                                    "Invalid protocol: \"" +
                                    protocolExpr + "\"");
    }
  }

  /**
   * The default URI protocol: "tcp", "xdr", "none"
   */
  public static Set<String> defaultProtocol ()
  {
    return DEFAULT_PROTOCOL;
  }
}
