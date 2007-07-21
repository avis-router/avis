package org.avis.federation;

import java.util.Map;

import org.avis.router.Router;

public class FederationConnector extends FederationLink
{
  public FederationConnector (Router router, String federationId,
                              Map<EwafURI, FederationClass> connectMap)
  {
    super (router, federationId);
  }
}
