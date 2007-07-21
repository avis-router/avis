package org.avis.federation;

import java.util.Set;

import org.avis.router.Router;

public class FederationListener extends FederationLink
{
  public FederationListener (Router router, String federationId,
                             FederationClassMap classMap, 
                             Set<EwafURI> listenUris)
  {
    super (router, federationId);
  }
}
