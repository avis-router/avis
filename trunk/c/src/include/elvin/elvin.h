/*
 * The main Avis client library definitions.
 */
#ifndef ELVIN_H
#define ELVIN_H

#include <elvin/keys.h>
#include <elvin/attributes.h>
#include <elvin/stdtypes.h>
#include <elvin/elvin_uri.h>
#include <elvin/errors.h>
#include <elvin/array_list.h>

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
 * @see elvin_subscription_init()
 * @see elvin_subscribe()
 * @see Notification
 */
typedef struct
{
  Elvin *      elvin;
  const char * subscription_expr;
  uint64_t     id;
  Keys         keys;
  SecureMode   security;
  ArrayList    listeners;
} Subscription;

/**
 * A notification received via an Elvin router.
 * 
 * @see elvin_subscription_add_listener() 
 */
typedef struct
{
  /** The attribute values of the notification. */
  Attributes attributes;
  
  /** True if the notification was received securely. */
  bool       secure;
} Notification;

/**
 * A listener for notifications received via a subscription.
 * 
 * @param subscription The subscription that matched the notification.
 * @param notification The notification from the router. Note that this 
 * notification and its associated attributes is only valid for the duration
 * of the callback -- it will be freed by the connection after the callback 
 * returns. If you want to refer to any part of the notification 
 * outside callback scope, you will need to copy the relevant parts before
 * returning.
 * 
 * @see elvin_subscription_add_listener()
 */
typedef void (*SubscriptionListener) (Subscription *subscription, 
                                      Notification *notification); 
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
 * ElvinError error;
 * 
 * if (!elvin_open (&elvin, "elvin://public.elvin.org", &error))
 * {
 *   elvin_perror ("open", &error);
 *   exit (1);
 * }
 * 
 * elvin_close (&elvin);
 * </pre>
 * 
 * @see elvin_open_uri()
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
 * attributes_destroy (ntfn);
 * </pre>
 * 
 * @see elvin_subscribe()
 */
bool elvin_send (Elvin *elvin, Attributes *notification, ElvinError *error);

/**
 * Subscribe to notifications from an Elvin router.
 * 
 * @param elvin The Elvin connection instance.
 *
 * @param subscription The subscription to add. The subscription_expr field
 * must be initialised to the expression used to select notifications (see
 * elvin_subscription_init()). The keys and security fields may also be set
 * to the desired initial values.
 *
 * @param error The error info.
 * 
 * @return True if the subscription succeeded.
 * 
 * Example:
 * <pre>
 * Elvin *elvin = ...
 * ElvinError *error = ...
 * Subscription subscription;
 * 
 * elvin_subscription_init (&subscription, "string (message)");
 *
 * if (!elvin_subscribe (elvin, &sub, error))
 * {
 *   elvin_perror ("subscribe", error);
 *   exit (1);
 * }
 * 
 * elvin_unsubscribe (elvin, &subscription, error);
 * </pre>
 * 
 * @see elvin_send()
 * @see elvin_unsubscribe()
 * @see elvin_subscription_add_listener()
 */
bool elvin_subscribe (Elvin *elvin, Subscription *subscription, 
                      ElvinError *error);

/**
 * Unsubscribe from a subscription created on an Elvin router.
 * 
 * @param elvin The Elvin connection instance.
 *
 * @param subscription The subscription to remove.
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
 */
void elvin_subscription_add_listener (Subscription *subscription, 
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
 * Poll an Elvin connection for incoming messages from the router. This will 
 * block until a notification or disconnect signal is received from the 
 * router. On receipt of a notification, any listeners to the notification
 * will be called from this function.
 * 
 * This method should be called in an event loop by clients that
 * subscribe to notifications.
 * 
 * @return True if no error occurred.
 */
bool elvin_poll (Elvin *elvin, ElvinError *error);

/**
 * Initialise a subscription instance. After initialisation of the required
 * subscription_expr field, the optional keys and security fields may also be
 * set before calling elvin_subscribe() to activate the subscription.
 * 
 * @param subscription The instance to initialise.
 * @param subscription_expr The 
 * <a href="http://avis.sourceforge.net/subscription_language.html">subscription expression</a>
 * used to select incoming notifications.
 * 
 * @see elvin_unsubscribe()
 */
void elvin_subscription_init (Subscription *subscription, 
                              const char *subscription_expr);
#endif /* ELVIN_H */
