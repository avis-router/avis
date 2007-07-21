package org.avis.federation;

import org.avis.router.Router;

public abstract class FederationLink
{
  protected Router router;
  protected String federationId;

  public FederationLink (Router router, String federationId)
  {
    this.router = router;
    this.federationId = federationId;
  }
}
