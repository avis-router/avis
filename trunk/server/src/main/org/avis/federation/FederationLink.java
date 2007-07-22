package org.avis.federation;

import org.avis.router.Router;

public abstract class FederationLink
{
  protected Router router;
  protected String serverDomain;

  public FederationLink (Router router, String serverDomain)
  {
    this.router = router;
    this.serverDomain = serverDomain;
  }
}
