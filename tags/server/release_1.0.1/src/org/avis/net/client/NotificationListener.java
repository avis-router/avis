package org.avis.net.client;

import java.util.EventListener;

public interface NotificationListener extends EventListener
{
  public void notificationReceived (NotificationEvent e);
}
