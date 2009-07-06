package org.avis.management.web;

import java.io.UnsupportedEncodingException;

import java.nio.charset.Charset;

import org.apache.asyncweb.common.DefaultHttpResponse;
import org.apache.asyncweb.common.HttpRequest;
import org.apache.asyncweb.common.MutableHttpResponse;
import org.apache.asyncweb.server.HttpService;
import org.apache.asyncweb.server.HttpServiceContext;
import org.apache.asyncweb.server.resolver.ServiceResolver;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.util.Base64;

import static org.apache.asyncweb.common.HttpResponseStatus.UNAUTHORIZED;

/**
 * Resolves any unauthenticated requests to a 401 basic auth page.
 * 
 * @author Matthew Phillips
 */
public class Authoriser implements ServiceResolver, HttpService
{
  public static final String SERVICE_NAME = "authorisation";
  
  private String name;
  private String password;

  public Authoriser (String name, String password)
  {
    this.name = name;
    this.password = password;
  }

  public String resolveService (HttpRequest request)
  {
    boolean authorised = false;
    String authorisation = request.getHeader ("Authorization");
    
    if (authorisation != null && authorisation.startsWith ("Basic "))
    {
      String auth = base64Decode (authorisation.substring (6));
      int sep = auth.indexOf (':');
      
      String authName = "";
      String authPassword = "";
      
      if (sep != -1)
      {
        authName = auth.substring (0, sep);
        authPassword = auth.substring (sep + 1);
      }
      
      authorised = authName.equals (name) && authPassword.equals (password);
    }
    
    return authorised ? null : SERVICE_NAME; 
  }

  public void handleRequest (HttpServiceContext context)
    throws Exception
  {
    MutableHttpResponse response = new DefaultHttpResponse ();
    response.setHeader ("WWW-Authenticate", "Basic realm=\"Avis Web Manager\"");
    response.setHeader ("Content-Type", "text/html; charset=UTF-8");
    response.setStatus (UNAUTHORIZED);

    IoBuffer out = IoBuffer.allocate (2048);
    out.setAutoExpand (true);

    out.putString 
      ("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" + 
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" + 
       "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n" + 
       "<head>\n" + 
       "  <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />\n" + 
       "  <title>Authorisation Required</title>\n" + 
       "  <link href=\"main.css\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\" />\n" + 
       "</head>\n" +
       "<body><p>Please enter the administrator password</p>" +
       "</body></html>", Charset.forName ("UTF-8").newEncoder ());
    
    out.flip ();
    
    response.setContent (out);

    context.commitResponse (response);
  }

  public void start ()
  {
    // zip
  }

  public void stop ()
  {
    // zip
  }
  
  private static String base64Decode (String base64Chars)
  {
    try
    {
      return new String 
        (new Base64 ().decode (base64Chars.getBytes ("US-ASCII")), "US-ASCII");
    } catch (UnsupportedEncodingException ex)
    {
      return "";
    }
  }
}