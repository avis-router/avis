/*
 *  Avis Elvin client library for C.
 *
 *  Copyright (C) 2008 Matthew Phillips <avis@mattp.name>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of version 3 of the GNU Lesser General
 *  Public License as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
/** \file
 * The main Avis client library definitions.
 */
#ifndef AVIS_ELVIN_H
#define AVIS_ELVIN_H

#include <avis/defs.h>
#include <avis/keys.h>
#include <avis/attributes.h>
#include <avis/stdtypes.h>
#include <avis/elvin_uri.h>
#include <avis/errors.h>
#include <avis/arrays.h>
#include <avis/net.h>

#define AVIS_LISTENERS_TYPE

typedef ArrayList * Listeners;

/**
 * A client connection to an Elvin router. Typically a client creates
 * a connection (elvin_open()) and then subscribes to notifications
 * (elvin_subscribe()) and/or sends them (elvin_send()). For
 * applications that create subscriptions, the usual model is to
 * connect, subscribe and then call elvin_event_loop() to dispatch
 * incoming notifications.
 *
 * <h2>Threading Model</h2>
 *
 * Elvin client connections are single-threaded, and are designed to
 * be driven by a thread calling elvin_event_loop(). Multi-threaded
 * applications should use elvin_invoke() to safely invoke operations in
 * the event loop thread.
 *
 * @see elvin_open()
 * @see elvin_open_uri()
 * @see elvin_close()
 * @see elvin_event_loop()
 * @see elvin_invoke()
 */
typedef struct
{
  socket_t    router_socket;
  socket_t    control_socket_read;
  socket_t    control_socket_write;
  ArrayList   subscriptions;
  Listeners   close_listeners;
  Listeners   notification_listeners;
  Keys *      notification_keys;
  Keys *      subscription_keys;
  bool        polling;
  ElvinError  error;
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
  Listeners    listeners;
  Keys *       keys;
} Subscription;

typedef enum
{
  /** The client was shut down normally with a call to elvin_close(). */
  REASON_CLIENT_SHUTDOWN,

  /** The router was shut down normally. */
  REASON_ROUTER_SHUTDOWN,

  /**
   * Either the client or the router decided that the protocol rules have
   * been violated. This would only happen in the case of a serious bug in
   * the client or router.
   */
  REASON_PROTOCOL_VIOLATION
} CloseReason;

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
 * A listener for elvin connection close events.
 *
 * @param elvin The elvin connection.
 * @param reason The reason for the shutdown.
 * @param message The router's shutdown message (when REASON_ROUTER_SHUTDOWN),
 * or the client's description of the reason otherwise.
 * @param user_data The user data passed in when adding the listener.
 *
 * @see elvin_add_close_listener()
 */
typedef void (*CloseListener) (Elvin *elvin, CloseReason reason,
                               const char *message,
                               void *user_data);

/**
 * A listener for connection-wide notification events.
 *
 * @param elvin The elvin connection.
 * @param attributes The notification from the router. Note that these
 * attributes are only valid for the duration of the callback -- they will be
 * freed by the connection after the callback returns. If you want to refer to
 * any part of the notification  outside callback scope, you will need to copy
 * the relevant parts before returning.
 * @param secure True if the notification was received securely from a client
 * with a matching set of security keys (see elvin_subscribe_with_keys() and
 * elvin_open_with_keys()). This will be true if at least one subscription
 * securely matched the notification.
 * @param user_data The user data passed in when adding the listener.
 *
 * @see elvin_add_notification_listener()
 */
typedef void (*GeneralNotificationListener)
  (Elvin *elvin, Attributes *attributes, bool secure, void *user_data);

/**
 * A handler function schedued to be called by elvin_invoke().
 *
 * @param elvin The Elvin connection that is calling the function.
 * @param parameter The parameter passed into elvin_invoke ().
 */
typedef void (*InvokeHandler) (Elvin *elvin, void *parameter);

/**
 * Open a connection to an Elvin router.
 *
 * @param elvin The Elvin connection instance.
 * @param router_uri The URI for the router endpoint.
 *
 * @return true if the connection succeeded.
 *
 * Example:
 * <pre>
 * Elvin elvin;
 *
 * if (!elvin_open (&elvin, "elvin://public.elvin.org"))
 * {
 *   elvin_perror ("open", &elvin.error);
 *   exit (1);
 * }
 *
 * elvin_close (&elvin);
 * </pre>
 *
 * @see elvin_open_uri()
 * @see elvin_open_with_keys()
 * @see elvin_close()
 * @see elvin_is_open()
 */
bool elvin_open (Elvin *elvin, const char *router_uri);

/**
 * Open a connection to an Elvin router.
 *
 * @param elvin The Elvin connection instance.
 * @param uri The URI for the router endpoint.
 *
 * @return true if the connection succeeded.
 *
 * @see elvin_open()
 */
bool elvin_open_uri (Elvin *elvin, ElvinURI *uri);

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
 *
 * @return true if the connection succeeded.
 *
 * @see elvin_open_uri()
 * @see elvin_set_keys()
 */
bool elvin_open_with_keys (Elvin *elvin, ElvinURI *uri,
                           Keys *notification_keys, Keys *subscription_keys);

/**
 * Change the connection-wide keys used to secure the receipt and
 * delivery of notifications.
 *
 * @param elvin The Elvin connection.
 * @param notification_keys The new notification keys. These
 *          automatically apply to all notifications, exactly as if
 *          they were added to the keys in the elvin_send_with_keys() call.
 * @param subscription_keys The new subscription keys. These
 *          automatically apply to all subscriptions, exactly as if
 *          they were added to the keys in the elvin_subscribe_with_keys(),
 *          call. This applies to all existing and future
 *          subscriptions.
 *
 * @see elvin_open_with_keys()
 * @see elvin_subscription_set_keys()
 */
bool elvin_set_keys (Elvin *elvin,
                     Keys *notification_keys, Keys *subscription_keys);

/**
 * Send a notification to an Elvin router.
 *
 * @param elvin The Elvin connection instance.
 * @param notification The notification attributes.
 *
 * @return True if the send succeeded.
 *
 * Example:
 * <pre>
 * Elvin *elvin = ...
 * Attributes *notification = attributes_create ();
 *
 * attributes_set_int32 (notification, "favourite number", 42);
 * attributes_set_string (notification, "message", "hello world");
 *
 * elvin_send (elvin, notification);
 *
 * attributes_destroy (notification);
 * </pre>
 *
 * @see elvin_subscribe()
 */
bool elvin_send (Elvin *elvin, Attributes *notification);

/**
 * Send a notification to an Elvin router with security constraints.
 *
 * @param elvin The Elvin connection instance.
 * @param notification The notification to send.
 * @param notification_keys The keys that must match for secure delivery.
 * @param security The security requirement.
 *          REQUIRE_SECURE_DELIVERY means the notification can only
 *          be received by subscriptions with keys matching the set
 *          supplied here (or the connections' global notification keys: see
 *          elvin_subscription_set_keys()).
 *
 * @see elvin_send()
 * @see elvin_subscribe_with_keys()
 */
bool elvin_send_with_keys (Elvin *elvin, Attributes *notification,
                           Keys *notification_keys, SecureMode security);

/**
 * Subscribe to notifications from an Elvin router.
 *
 * @param elvin The Elvin connection instance.
 * @param subscription_expr The
 * <a href="http://avis.sourceforge.net/subscription_language.html">subscription expression</a>.
 * This expression is copied and freed when the subscription is disposed.
 *
 * @return The new subscription, or NULL on error.
 *
 * Example:
 * <pre>
 * Elvin *elvin = ...
 * Subscription *subscription =
 *   elvin_subscribe (elvin, "string (message)");
 *
 * if (!subscription)
 * {
 *   elvin_perror ("subscribe", &elvin->error);
 *   exit (1);
 * }
 *
 * elvin_unsubscribe (elvin, subscription);
 * </pre>
 *
 * @see elvin_subscribe_with_keys()
 * @see elvin_subscription_add_listener()
 */
Subscription *elvin_subscribe (Elvin *elvin, const char *subscription_expr);

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
 *
 * @return The new subscription, or NULL on error.
 *
 * Example:
 * <pre>
 * Elvin *elvin = ...
 * Keys *sub_keys = ...
 * Subscription *subscription =
 *   elvin_subscribe_with_keys (elvin, "string (message)", keys,
 *                              REQUIRE_SECURE_DELIVERY);
 *
 * if (!subscription)
 * {
 *   elvin_perror ("subscribe", &elvin->error);
 *   exit (1);
 * }
 *
 * elvin_unsubscribe (elvin, subscription);
 * </pre>
 *
 * @see elvin_unsubscribe()
 * @see elvin_subscription_set_expr()
 * @see elvin_send_with_keys()
 * @see elvin_subscription_add_listener()
 * @see elvin_subscription_set_keys()
 * @see elvin_poll()
 */
Subscription *elvin_subscribe_with_keys (Elvin *elvin,
                                         const char *subscription_expr,
                                         Keys *keys,
                                         SecureMode security);

/**
 * Unsubscribe from a subscription created on an Elvin router.
 *
 * @param elvin The Elvin connection instance.
 *
 * @param subscription The subscription to remove. This will be automatically
 * freed by the connection.
 *
 * @return True if the unsubscribe succeeded.
 *
 * @see elvin_subscribe()
 */
bool elvin_unsubscribe (Elvin *elvin, Subscription *subscription);

/**
 * Change the subscription expression for an existing subscription.
 *
 * @param subscription The subscription to change.
 * @param subscription_expr The new subscription expression.
 *
 * @see elvin_subscribe()
*/
bool elvin_subscription_set_expr (Subscription *subscription,
                                  const char *subscription_expr);

/**
 * Change the keys used for receiving secure notifications.
 *
 * @param subscription The subscription to change.
 * @param subscription_keys The new subscription keys.
 * @param security security The security mode: specifying
 *          REQUIRE_SECURE_DELIVERY means the subscription will only
 *          receive notifications that are sent by clients with keys
 *          matching the set supplied here or the global
 *          subscription key set.
 *
 * @see elvin_subscribe_with_keys()
 * @see elvin_set_keys()
*/
bool elvin_subscription_set_keys (Subscription *subscription,
                                  Keys *subscription_keys,
                                  SecureMode security);

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
 * @see elvin_add_notification_listener()
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
 * Close the elvin connection. This may be called any number of times, calls
 * subsequent to the first have no effect.
 *
 * @param elvin The Elvin connection instance.
 *
 * @return True if the connection was closed, false if it was already closed.
 */
bool elvin_close (Elvin *elvin);

/**
 * Invoke a function call inside the Elvin event loop thread. This
 * call will return immediately, and the nominated handler will be
 * called from the event loop thread (the one calling
 * elvin_event_loop()) at the earliest opportunity. This function is
 * the only one that is safe to call threads other than the one running the
 * main event loop.
 *
 * @param elvin The Elvin connection.
 * @param handler The handler to call.
 * @param parameter The single parameter to supply to the handler
 * (which may be NULL)
 * @return True if the handler was queued for callback. False
 * indicates the connection is closed or there was an internal error.
 *
 * @see elvin_invoke_close()
 */
bool elvin_invoke (Elvin *elvin, InvokeHandler handler, void *parameter);

/**
 * Shortcut to invoke elvin_close() via elvin_invoke(). This can be used by
 * multi-threaded applications to safely shut down an Elvin connection.
 */
bool elvin_invoke_close (Elvin *elvin);

/**
 * Add a listener that will be called when the connection is closed.
 *
 * @param elvin The elvin connection.
 * @param listener The listener to be called when the connection is either
 * closed locally (elvin_close ()) or remotely by the router
 * @param user_data An optional pointer to user data to be passed into
 * the listener when called. This can be used to provide context information
 * to the listener function.
 *
 * @see elvin_remove_close_listener()
 */
void elvin_add_close_listener (Elvin *elvin, CloseListener listener,
                               void *user_data);

/**
 * Remove a listener added by elvin_add_close_listener().
 *
 * @param elvin The elvin connection.
 * @param listener The listener to be removed.
 *
 * @return True if the listener was in the list and was removed.
 *
 * @see elvin_add_close_listener()
 */
bool elvin_remove_close_listener (Elvin *elvin, CloseListener listener);

/**
 * Add a listener that will be called when a notification is received on any
 * subscription created by this connection.
 *
 * @param elvin The elvin connection.
 * @param listener The listener to be called when the a notification
 * is received.
 * @param user_data An optional pointer to user data to be passed into
 * the listener when called. This can be used to provide context information
 * to the listener function.
 *
 * @see elvin_remove_notification_listener()
 * @see elvin_subscribe()
 * @see elvin_subscription_add_listener()
 */
void elvin_add_notification_listener (Elvin *elvin,
                                      GeneralNotificationListener listener,
                                      void *user_data);

/**
 * Remove a listener added by elvin_add_notification_listener().
 *
 * @param elvin The elvin connection.
 * @param listener The listener to be removed.
 *
 * @return True if the listener was in the list and was removed.
 *
 * @see elvin_add_close_listener()
 */
bool elvin_remove_notification_listener (Elvin *elvin,
                                         GeneralNotificationListener listener);

/**
 * Continuously poll an Elvin connection for an incoming messages from
 * the router until the client connection is closed. Clients that
 * create subscriptions should call this function after subscribing to
 * receive notifications and dispatch them to their listeners.
 *
 * This function will block and only return when the connection is
 * closed by the client with elvin_close(), by the router, or due to
 * a protocol error.
 *
 * To close the connection from another thread, call elvin_invoke_close().
 *
 * Example:
 *
 * <pre>
 * Elvin *elvin = ...
 *
 * if (!elvin_event_loop (elvin))
 *   elvin_perror ("elvin", &elvin->error);
 * </pre>
 *
 * @return True if no error occurred.
 *
 * @see elvin_close()
 * @see elvin_poll()
 * @see elvin_event_loop()
 */
bool elvin_event_loop (Elvin *elvin);

/**
 * Poll an Elvin connection for a single incoming message from the
 * router. This will block until a notification or disconnect message
 * is received from the router, or until ELVIN_IO_TIMEOUT milliseconds
 * have passed (in which case the error code will be set to
 * ELVIN_ERROR_TIMEOUT).
 *
 * On receipt of a notification, any listeners to the notification
 * will be called from this function. On receipt of a disconnect or
 * socket close, the connection will be shut down.
 *
 * This method wil not normally be useful to clients: see
 * elvin_event_loop() instead.
 *
 * @return True if no error occurred.
 */
bool elvin_poll (Elvin *elvin);

#endif /* AVIS_ELVIN_H */
