package org.avis.client;

import java.util.EventListener;

public interface CloseListener extends EventListener
{
  public void connectionClosed (CloseEvent e);
}
