package org.avis.management;

import org.apache.asyncweb.common.DefaultHttpResponse;
import org.apache.asyncweb.common.HttpRequest;
import org.apache.asyncweb.common.MutableHttpResponse;
import org.apache.asyncweb.server.HttpService;
import org.apache.asyncweb.server.HttpServiceContext;
import org.apache.asyncweb.server.resolver.ServiceResolver;

import static org.apache.asyncweb.common.HttpResponseStatus.TEMPORARY_REDIRECT;

/**
 * Redirects any requests for "/" to a given default URI.
 * 
 * @author Matthew Phillips
 */
public class RedirectRoot implements ServiceResolver, HttpService
{
  public static final String SERVICE_NAME = "redirect-default";
  
  private String defaultUri;

  public RedirectRoot (String defaultUri)
  {
    this.defaultUri = defaultUri;
  }

  public String resolveService (HttpRequest request)
  {
    // redirect "/" to the default service
    if (request.getRequestUri ().getPath ().equals ("/"))
      return SERVICE_NAME;
    else
      return null;
  }

  public void handleRequest (HttpServiceContext context)
    throws Exception
  {
    MutableHttpResponse response = new DefaultHttpResponse ();
    
    response.setHeader ("Location", "/" + defaultUri);
    response.setStatus (TEMPORARY_REDIRECT);
    
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
}
