package org.avis.management.web;

import org.apache.asyncweb.common.HttpRequest;
import org.apache.asyncweb.server.resolver.ServiceResolver;

public class PageResolver implements ServiceResolver
{
  public String resolveService (HttpRequest request)
  {
    String service = request.getRequestUri ().getPath ().substring (1);
    
    if (StandardPage.URIs.contains (service))
      return service;
    else     
      return null;
  }
}
