/*
 *  elvin.h
 *
 *  Elvin Client Library
 *  Main Public Definitions.
 */

#ifndef ELVIN_H
#define ELVIN_H

#include <elvin/keys.h>
#include <elvin/named_values.h>
#include <elvin/stdtypes.h>
#include <elvin/elvin_uri.h>
#include <elvin/errors.h>

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
 * A client's connection to an Elvin router.
 * 
 * See elvin_open() and elvin_open_uri().
 */
typedef struct
{
  socket_t socket;
} Elvin;

typedef enum 
{
  REQUIRE_SECURE_DELIVERY = 0,
  ALLOW_INSECURE_DELIVERY = 1
} SecureMode;

struct ArrayList;

typedef struct
{
  Elvin *elvin;
  const char *subscription_expr;
  uint64_t id;
  Keys keys;
  SecureMode security;
  struct ArrayList *listeners;
} Subscription;

typedef struct
{
  NamedValues attributes;
} Notification;

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
 * @see elvin_open_uri().
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
 */
bool elvin_open_uri (Elvin *elvin, ElvinURI *uri, ElvinError *error);

bool elvin_send (Elvin *elvin, NamedValues *notification, ElvinError *error);

bool elvin_subscribe (Elvin *elvin, Subscription *subscription, 
                      ElvinError *error);

void elvin_add_subscription_listener (Subscription *subscription, 
                                      SubscriptionListener listener);

bool elvin_is_open (Elvin *elvin);

bool elvin_close (Elvin *elvin);

void elvin_poll (Elvin *elvin, ElvinError *error);

void elvin_subscription_init (Subscription *subscription, 
                              const char *subscription_expr);
#endif /* ELVIN_H */
