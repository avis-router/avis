package org.avis.client;

/**
 * Specifies the secure delivery mode for notifications.
 *  
 * @author Matthew Phillips
 */
public enum SecureMode
{
  /**
   * Require secure key match between notification and subscription
   * before delivering to a client.
   */
  REQUIRE_SECURE_DELIVERY,
  
  /**
   * Allow clients without matching keys to receive the message. Those
   * with matching keys will still receive securely.
   */
  ALLOW_INSECURE_DELIVERY;
}
