package org.avis.management.web;

import org.avis.router.Router;

public class ConnectionsPage extends WebPage
{
  private Router router;

  public ConnectionsPage (Router router)
  {
    this.router = router;
  }

  @Override
  protected CharSequence htmlText ()
  {
    return "Hello!";
  }
}
