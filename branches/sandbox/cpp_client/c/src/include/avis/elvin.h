/** \file
 * The main Avis client library definitions.
 */
#ifndef AVIS_ELVIN_H
#define AVIS_ELVIN_H

#include <avis/keys.h>
#include <avis/attributes.h>
#include <avis/stdtypes.h>
#include <avis/elvin_uri.h>
#include <avis/errors.h>
#include <avis/arrays.h>

#ifdef WIN32
  #include <winsock2.h>

  typedef SOCKET socket_t;
#else
  typedef int socket_t;
#endif

/** The default port for Elvin client connections. */
#define DEFAULT_ELVIN_PORT 2917

/** The default client protocol major version supported by this library. */
#define DEFAULT_CLIENT_PROTOCOL_MAJOR 4

/** The default client protocol minor version supported by this library. */
#define DEFAULT_CLIENT_PROTOCOL_MINOR 0

/**
 * A client connection to an Elvin router. Typically a client creates a
 * connection (elvin_open()) and then subscribes to notifications 
 * (elvin_subscribe()) and/or sends them (elvin_send()).
 * 
 * See elvin_open() and elvin_open_uri().
 */
typedef struct
{
  socket_t  socket;
  ArrayList subscriptions;
  Keys *    notification_keys;
  Keys *    subscription_keys;
} Elvin;

/**
 * Selects the secure delivery/receipt mode for notifications.
 */
typedef enum 
{
  REQUIRE_SECURE_DELIVERY = 0,
  ALLOW_INSECURE_DELIVERY = 1
} SecureMode;

/**
 * A subscription to notifications on an Elvin router.
 * 
 * @see elvin_subscribe()
 */
typedef struct
{
  Elvin *      elvin;
  char *       subscription_expr;
  uint64_t     id;
  SecureMode   security;
  ArrayList    listeners;
  Keys *       keys;
} Subscription;

/**
 * A listener for notifications received via a subscription.
 * 
 * @param subscription The subscription that matched the notification.
 * @param attributes The notification from the router. Note that these 
 * attributes are only valid for the duration of the callback -- they will be 
 * freed by the connection after the callback returns. If you want to refer to 
 * any part of the notification  outside callback scope, you will need to copy 
 * the relevant parts before returning.
 * @param secure True if the notification was received securely from a client
 * with a matching set of security keys (see elvin_subscribe_with_keys() and
 * elvin_open_with_keys()).
 * @param user_data The user data pointer passed into 
 * elvin_subscription_add_listener().
 * 
 * @see elvin_subscription_add_listener()
 */
typedef void (*SubscriptionListener) (Subscription *subscription, 
                                      Attributes *attributes, bool secure,
                                      void *user_data);

/**
 * Open a connection to an Elvin router.
 * 
 * @param elvin The Elvin connection instance.
 * @param router_uri The URI for the router endpoint.
 * @param error The error info.
 * 
 * @return true if the connection succeeded.
 * 
 * Example:
 * <pre>
 * Elvin elvin;
 * ElvinError error = elvin_error_create ();
 * 
 * if (!elvin_open (&elvin, "elvin://public.elvin.org", &error))
 * {
 *   elvin_perror ("open", &error);
 *   exit (1);
 * }
 * 
 * elvin_close (&elvin);
 * elvin_error_free (&error);
 * </pre>
 * 
 * @see elvin_open_uri()
 * @see elvin_open_with_keys()
 * @see elvin_close()
 * @see elvin_is_open()
 */
bool elvin_open (Elvin *elvin, const char *router_uri, ElvinError *error);

/**
 * Open a connection to an Elvin router.
 * 
 * @param elvin The Elvin connection instance.
 * @param uri The URI for the router endpoint.
 * @param error The error info.
 * 
 * @return true if the connection succeeded.
 * 
 * @see elvin_open()
 */
bool elvin_open_uri (Elvin *elvin, ElvinURI *uri, ElvinError *error);

/**
 * Open a connection to an Elvin router with optional security constraints.
 * Ownership of the keys used in this call passes to the connection,
 * which will free them when no longer needed.
 * 
 * @param elvin The Elvin connection instance.
 * @param uri The URI for the router endpoint.
 * @param notification_keys These keys automatically apply to all
 *          notifications, exactly as if they were added to the keys
 *          in the elvin_send_with_keys() call.
 * @param subscription_keys These keys automatically apply to all
 *          subscriptions, exactly as if they were added to the keys
 *          in the elvin_subscribe_with_keys() call.
 * @param error The error info.
 * 
 * @return true if the connection succeeded.
 * 
 * @see elvin_open_uri()
 */
bool elvin_open_with_keys (Elvin *elvin, ElvinURI *uri,
                           Keys *notification_keys, Keys *subscription_keys, 
                           ElvinError *error);

/**
 * Send a notification to an Elvin router.
 * 
 * @param elvin The Elvin connection instance.
 * @param notification The notification attributes.
 * @param error The error info.
 * 
 * @return True if the send succeeded.
 * 
 * Example:
 * <pre>
 * Elvin *elvin = ...
 * ElvinError *error = ...
 * Attributes *notification = attributes_create ();
 * 
 * attributes_set_int32 (notification, "favourite number", 42);
 * attributes_set_string (notification, "message", "hello world");
 *  
 * elvin_send (elvin, notification, error);
 *
 * attributes_destroy (notification);
 * </pre>
 * 
 * @see elvin_subscribe()
 */
bool elvin_send (Elvin *elvin, Attributes *notification, ElvinError *error);

/**
 * Subscribe to notifications from an Elvin router.
 * 
 * @param elvin The Elvin connection instance.
 * @param subscription_expr The 
 * <a href="http://avis.sourceforge.net/subscription_language.html">subscription expression</a>.
 * This expression is copied and freed when the subscription is disposed.
 * @param error The error info.
 *
 * @return The new subscription, or NULL on error.
 *
 * Example:
 * <pre>
 * Elvin *elvin = ...
 * ElvinError *error = ...
 * Subscription *subscription = 
 *   elvin_subscribe (elvin, "string (message)", error);
 *
 * if (!subscription)
 * {
 *   elvin_perror ("subscribe", error);
 *   exit (1);
 * }
 * 
 * elvin_unsubscribe (elvin, subscription, error);
 * </pre>
 *  
 * @see elvin_subscribe_with_keys()
 * @see elvin_subscription_add_listener()
 */
Subscription *elvin_subscribe (Elvin *elvin, const char *subscription_expr, 
                               ElvinError *error);

/**
 * Subscribe to notifications from an Elvin router with optional security
 * constraints.
 * 
 * @param elvin The Elvin connection instance.
 * @param subscription_expr The 
 * <a href="http://avis.sourceforge.net/subscription_language.html">subscription expression</a>.
 * This expression is copied and freed when the subscription is disposed.
 * @param keys The keys that must match notification keys for
 *          secure delivery. Ownership of the keys passes to the connection,
 * which will free them when no longer needed.
 * @param security The security mode: specifying
 *          REQUIRE_SECURE_DELIVERY means the subscription will only
 *          receive notifications that are sent by clients with keys
 *          matching the set supplied here or the global
 *          subscription key set.
 * @param error The error info.
 *
 * @return The new subscription, or NULL on error.
 * 
 * Example:
 * <pre>
 * Elvin *elvin = ...
 * Keys *sub_keys = ...
 * ElvinError *error = ...
 * Subscription *subscription = 
 *   elvin_subscribe (elvin, "string (message)", keys, 
 *                    REQUIRE_SECURE_DELIVERY, error);
 *
 * if (!subscription)
 * {
 *   elvin_perror ("subscribe", error);
 *   exit (1);
 * }
 * 
 * elvin_unsubscribe (elvin, subscription, error);
 * </pre>
 * 
 * @see elvin_send()
 * @see elvin_unsubscribe()
 * @see elvin_subscription_add_listener()
 * @see elvin_poll()
 */
Subscription *elvin_subscribe_with_keys (Elvin *elvin, 
                                         const char *subscription_expr, 
                                         Keys *keys,
                                         SecureMode security, 
                                         ElvinError *error);

/**
 * Unsubscribe from a subscription created on an Elvin router.
 * 
 * @param elvin The Elvin connection instance.
 *
 * @param subscription The subscription to remove. This will be automatically
 * freed by the connection.
 *
 * @param error The error info.
 * 
 * @return True if the unsubscribe succeeded.
 * 
 * @see elvin_subscribe()
 */
bool elvin_unsubscribe (Elvin *elvin, Subscription *subscription, 
                        ElvinError *error);

/**
 * Add a listener that will be called when notifications matching
 * a subscription are received.
 * 
 * @param subscription The subscription to add the listener to.
 * @param listener The listener to be called for matching notifications.
 * @param user_data An optional pointer to user data to be passed into
 * the listener when called. This can be used to provide context information 
 * to the listener function.
 * 
 * @see elvin_subscription_remove_listener()
 */
void elvin_subscription_add_listener (Subscription *subscription, 
                                      SubscriptionListener listener,
                                      void *user_data);

/**
 * Remove a previously added subscription listener.
 * 
 * @param subscription The subscription to remove the listener from.
 * @param listener The listener to be removed.
 * @return True if the listener was removed, false if it was not in the 
 * subscription list.
 * 
 * @see elvin_subscription_add_listener()
 */
bool elvin_subscription_remove_listener (Subscription *subscription, 
                                         SubscriptionListener listener);
/**
 * Test if the Elvin connection is open.
 * 
 * @param elvin The Elvin connection instance.
 * 
 * @see elvin_close()
 */
bool elvin_is_open (Elvin *elvin);

/**
 * Close the elvin connection.
 * 
 * @param elvin The Elvin connection instance.
 * 
 * @return True if the connection was closed, false if it was already closed.
 */
bool elvin_close (Elvin *elvin);

/**
 * Poll an Elvin connection for an incoming message from the router. This will 
 * block until a notification or disconnect message is received from the 
 * router. On receipt of a notification, any listeners to the notification
 * will be called from this function. On receipt of a disconnect or socket
 * close, the connection will be shut down.
 * 
 * @return True if no error occurred.
 * 
 * This method should be called in an event loop by clients that
 * subscribe to notifications. For example:
 * 
 * <pre>
 * Elvin *elvin = ...
 * ElvinError *error = ...
 * 
 * while (elvin_is_open (elvin) && elvin_error_ok (error))
 * {
 *   elvin_poll (elvin, error);
 * }
 * </pre>
 */
bool elvin_poll (Elvin *elvin, ElvinError *error);

#endif /* AVIS_ELVIN_H */