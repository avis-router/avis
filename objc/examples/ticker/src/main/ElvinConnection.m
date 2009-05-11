#import "ElvinConnection.h"

#import "utils.h"

NSString *ElvinConnectionOpenedNotification = @"ElvinConnectionOpened";
NSString *ElvinConnectionClosedNotification = @"ElvinConnectionClosed";
NSString *ElvinConnectionWillCloseNotification = @"ElvinConnectionWillClose";

NSString *KEY_RECEIVED_SECURE = @"_ElvinConnection::SECURE";

#pragma mark -

typedef struct 
{
  Attributes * message;
  Keys *       keys;
} SendMessageContext;

@class SubscriptionContext;

static void subscribe (Elvin *elvin, SubscriptionContext *context);

static void resubscribe (Elvin *elvin, SubscriptionContext *context);

static void close_listener (Elvin *elvin, CloseReason reason,
                            const char *message,
                            ElvinConnection *self);

static void notification_listener (Subscription *sub, 
                                   Attributes *attributes, 
                                   bool secure, SubscriptionContext *context);

static void send_message (Elvin *elvin, Attributes *message);

void send_message_with_keys (Elvin *elvin, SendMessageContext *context);

static Keys *subscriptionKeysFor (NSArray *keys);

static Keys *notificationKeysFor (NSArray *keys);
  
#pragma mark -

@interface SubscriptionContext : NSObject
{
  @public
  
  NSString *     subscriptionExpr;
  Subscription * subscription;
  id             delegate;
  SEL            selector;
}

- (void) deliverNotification: (NSDictionary *) notification;
  
@end

@implementation SubscriptionContext

+ (id) contextFor: (NSString *) newSubscriptionExpr 
      delegate: (id) newDelegate selector: (SEL) newSelector
{
  SubscriptionContext *context = [[SubscriptionContext new] retain];
  
  if (context == nil)
    return nil;
  
  context->subscriptionExpr = [newSubscriptionExpr retain];
  context->delegate = newDelegate;
  context->selector = newSelector;
  context->subscription = nil;
  
  return context;
}

- (void) dealloc
{  
  [subscriptionExpr release];
  
  [super dealloc];
}

- (void) deliverNotification: (NSDictionary *) notification
{
  [delegate performSelector: selector withObject: notification];
  
  [notification release];
}

@end

#pragma mark -

@interface ElvinConnection (PRIVATE)
  - (BOOL) openConnection;
  - (void) elvinEventLoopThread;
  - (void) runElvinEventLoop;
@end

@implementation ElvinConnection

+ (BOOL) wasReceivedSecure: (NSDictionary *) message
{
  NSNumber *value = [message objectForKey: KEY_RECEIVED_SECURE];
  
  return [value boolValue];
}

- (id) initWithUrl: (NSString *) url
{
  if (![super init])
    return nil;
    
  elvin_reset (&elvin);
  
  elvinUrl = [url retain];
  subscriptions = [[NSMutableArray arrayWithCapacity: 5] retain];
    
  return self;
}

- (void) dealloc
{
  NSLog (@"Dealloc elvin");
  
  [self disconnect];
  
  [elvinUrl release];    
  [subscriptions release];

  [super dealloc];
}

- (NSString *) elvinUrl
{
  return elvinUrl;
}

- (void) setElvinUrl: (NSString *) newElvinUrl
{
  if (![elvinUrl isEqual: newElvinUrl])
  {
    BOOL wasConnected = [self isConnected];
    
    if (wasConnected)
      [self disconnect];
    
    [elvinUrl release];
    elvinUrl = [newElvinUrl retain];

    if (wasConnected)
      [self connect];
  }
}

- (NSArray *) keys
{
  return keys;
}

- (void) setKeys: (NSArray *) newKeys
{
  [keys release];
  
  keys = [newKeys retain];
  
  // TODO change connection keys
}

#pragma Elvin publish/subscribe

- (id) subscribe: (NSString *) subscriptionExpr
        withDelegate: (id) delegate usingSelector: (SEL) selector
{
  SubscriptionContext *context = 
    [SubscriptionContext contextFor: subscriptionExpr 
                         delegate: delegate selector: selector];
  
  [subscriptions addObject: context];
  
  if (elvin_is_open (&elvin))
    elvin_invoke (&elvin, (InvokeHandler)subscribe, context);
  
  return context;
}

- (void) resubscribe: (id) subscriptionContext 
         usingSubscription: (NSString *) newSubscription
{
  SubscriptionContext *context = subscriptionContext;
  
  [context->subscriptionExpr release];
  context->subscriptionExpr = [newSubscription retain];
  
  if (elvin_is_open (&elvin))
    elvin_invoke (&elvin, (InvokeHandler)resubscribe, context);
}

void resubscribe (Elvin *elvin, SubscriptionContext *context)
{
  elvin_subscription_set_expr 
    (context->subscription, [context->subscriptionExpr UTF8String]);

  if (!elvin_error_ok (&elvin->error))
  {
    NSLog (@"Error while trying to resubscribe: %s (%i)", 
           elvin->error.message, elvin->error.code);
  }
}

void subscribe (Elvin *elvin, SubscriptionContext *context)
{
  context->subscription = 
    elvin_subscribe (elvin, [context->subscriptionExpr UTF8String]);
  
  if (elvin_error_ok (&elvin->error))
  {
    elvin_subscription_add_listener 
      (context->subscription, (SubscriptionListener)notification_listener, 
       context);
  } else
  {
    NSLog (@"Error while trying to subscribe: %s (%i)", 
           elvin->error.message, elvin->error.code);
  }
}

void notification_listener (Subscription *sub, 
                            Attributes *attributes, 
                            bool secure, SubscriptionContext *context)
{
  NSMutableDictionary *message = [[NSMutableDictionary dictionary] retain];
  
  AttributesIter i;

  attributes_iter_init (&i, attributes);
  
  while (attributes_iter_has_next (&i))
  {
    const Value *value = attributes_iter_value (&i);

    NSObject *objcValue;
    
    switch (value->type)
    {
      case TYPE_STRING:
        objcValue = [NSString stringWithUTF8String: value->value.str];
        break;
      case TYPE_INT32:
        objcValue = [NSNumber numberWithInt: value->value.int32];
        break;
      case TYPE_INT64:
        objcValue = [NSNumber numberWithLongLong: value->value.int64];
        break;
      case TYPE_REAL64:
        objcValue = [NSNumber numberWithDouble: value->value.real64];
        break;
      case TYPE_OPAQUE:
        objcValue = [NSData dataWithBytes: value->value.bytes.items 
                                   length: value->value.bytes.item_count];
        break;
    }
    
    [message setValue: objcValue 
      forKey: [NSString stringWithUTF8String: attributes_iter_name (&i)]];
    
    attributes_iter_next (&i);
  }
  
  [message setObject: [NSNumber numberWithBool: secure] 
              forKey: KEY_RECEIVED_SECURE];
  
  [context
    performSelectorOnMainThread: @selector (deliverNotification:)
                     withObject: message waitUntilDone: NO];
}

- (void) sendTickerMessage: (NSString *) messageText 
                fromSender: (NSString *) from
                   toGroup: (NSString *) group
                 inReplyTo: (NSString *) replyToId 
               attachedURL: (NSURL *) url
                sendPublic: (BOOL) isPublic
              sendInsecure: (BOOL) isInsecure
{
  char messageID [UUID_STRING_LENGTH];
  
  createUUID (messageID);
  
  Attributes *message = attributes_create ();
  
  attributes_set_string (message, "Group", [group UTF8String]);
  attributes_set_string (message, "Message", [messageText UTF8String]);
  attributes_set_string (message, "From", [from UTF8String]);
  attributes_set_string (message, "Message-Id", messageID);
  attributes_set_string (message, "User-Agent", "Blue Sticker");
  attributes_set_int32  (message, "org.tickertape.message", 3001);
  
  if (replyToId != nil)
    attributes_set_string (message, "In-Reply-To", [replyToId UTF8String]);

  if (isPublic)
    attributes_set_string (message, "Distribution", "world");
  
  if (url)
  {
    attributes_set_string (message, "MIME_TYPE", "x-elvin/url");
    attributes_set_string (message, "MIME_ARGS", 
                           [[url absoluteString] UTF8String]);
  }

  if (isInsecure || [keys count] == 0)
  {
    elvin_invoke (&elvin, (InvokeHandler)send_message, message);
  } else
  {
    SendMessageContext *context = malloc (sizeof (SendMessageContext));
    
    context->message = message;
    context->keys = notificationKeysFor (keys);
    
    elvin_invoke (&elvin, (InvokeHandler)send_message_with_keys, context);
  }
}

- (void) sendPresenceRequestMessage: (NSString *) userID 
                      fromRequestor: (NSString *) requestor
                           toGroups: (NSString *) groups 
                           andUsers: (NSString *) users
                         sendPublic: (BOOL) isPublic
{
  Attributes *message = attributes_create ();
  
  attributes_set_int32  (message, "Presence-Protocol", 1000);  
  attributes_set_string (message, "Presence-Request",  [userID UTF8String]);
  attributes_set_string (message, "Requestor",         [requestor UTF8String]);
  attributes_set_string (message, "Groups",            [groups UTF8String]);
  attributes_set_string (message, "Users",             [users UTF8String]);
  
  if (isPublic)
    attributes_set_string (message, "Distribution", "world");
  
  elvin_invoke (&elvin, (InvokeHandler)send_message, message);
}

- (void) sendPresenceInfoMessage: (NSString *) userID
                         forUser: (NSString *) userName
                       inReplyTo: (NSString *) inReplyTo
                      withStatus: (PresenceStatus *) status
                        toGroups: (NSString *) groups
                      andBuddies: (NSString *) buddies
                   fromUserAgent: (NSString *) userAgent
                 includingFields: (PresenceFields) fields
                      sendPublic: (BOOL) isPublic
{
  Attributes *message = attributes_create ();
  
  attributes_set_int32  (message, "Presence-Protocol", 1000);  
  attributes_set_string (message, "Presence-Info",     [inReplyTo UTF8String]);
  attributes_set_string (message, "Client-Id",         [userID UTF8String]);
  attributes_set_string (message, "User",              [userName UTF8String]);
  attributes_set_string (message, "Groups",            [groups UTF8String]);
  
  if (fields & FieldStatus)
  {
    attributes_set_string (message, "Status",         
      [[status statusCodeAsString] UTF8String]);
    attributes_set_string (message, "Status-Text", 
      [[status statusText] UTF8String]);
    attributes_set_int32 (message, "Status-Duration", 
      [status statusDurationAsSecondsElapsed]);
  }
  
  if (fields & FieldBuddies)
    attributes_set_string (message, "Buddies", [buddies UTF8String]);
    
  if (fields & FieldUserAgent)
  {
    const char *agent = [userAgent UTF8String];
    
    attributes_set_string (message, "User-Agent", agent);
    attributes_set_string (message, "Ticker-Client", agent);
  }

  if (isPublic)
    attributes_set_string (message, "Distribution", "world");
  
  elvin_invoke (&elvin, (InvokeHandler)send_message, message);
}

void send_message (Elvin *elvin, Attributes *message)
{
  elvin_send (elvin, message);
  
  if (elvin_error_occurred (&elvin->error))
  {
    NSLog (@"Error while trying to send message: %s (%i)", 
           elvin->error.message, elvin->error.code);
  }
  
  attributes_destroy (message);
}

void send_message_with_keys (Elvin *elvin, SendMessageContext *context)
{
  elvin_send_with_keys
    (elvin, context->message, context->keys, REQUIRE_SECURE_DELIVERY);
  
  if (elvin_error_occurred (&elvin->error))
  {
    NSLog (@"Error while trying to send message: %s (%i)", 
           elvin->error.message, elvin->error.code);
  }
  
  attributes_destroy (context->message);
  free (context);
}

#pragma mark Event loop

- (BOOL) isConnected
{
  return elvin_is_open (&elvin);
}

- (void) connect
{
  NSAssert (eventLoopThread == nil, @"Attempt to close when still connected");
  
  eventLoopThread = 
    [[[NSThread alloc] 
       initWithTarget: self selector: @selector (elvinEventLoopThread) 
       object: nil] retain];
                             
  [eventLoopThread start];
}

- (void) disconnect
{
  [eventLoopThread cancel];
  
  if (elvin_is_open (&elvin) && elvin_error_ok (&elvin.error))
  {
    NSLog (@"Disconnect from Elvin");
    
    [[NSNotificationCenter defaultCenter] 
      postNotificationName: ElvinConnectionWillCloseNotification object: self];
      
    elvin_invoke_close (&elvin);    
    
    // TODO: this should not be potentially infinite
    while (![eventLoopThread isFinished])
      usleep (100000);
    
    // nuke defunct Elvin subscription pointers
    for (SubscriptionContext *context in subscriptions)
      context->subscription = nil;
  }
  
  [eventLoopThread release];
  eventLoopThread = nil;
}

- (void) elvinEventLoopThread
{
  while (![eventLoopThread isCancelled])
  {
    NSAutoreleasePool *pool = [NSAutoreleasePool new];
    
    NSLog (@"Start connect to Elvin");
    
    if ([self openConnection])
    {
      NSLog (@"Connected to Elvin at: %@", elvinUrl);
      
      [self runElvinEventLoop];
      
      if (elvin_error_occurred (&elvin.error))
      {
        NSLog (@"Exited Elvin event loop on error: %s (%i)", 
               elvin.error.message, elvin.error.code);
      }
    } else
    {
      NSLog (@"Failed to connect to elvin %@: %s (%i): will retry shortly...", 
             elvinUrl, elvin.error.message, elvin.error.code);
             
      if (![eventLoopThread isCancelled])
        [NSThread sleepForTimeInterval: 5];
    }
    
    elvin_close (&elvin);
 
    [pool release];
  }
  
  NSLog (@"Elvin thread is terminating");
}

- (BOOL) openConnection
{  
  ElvinURI uri;
  ElvinError error = ELVIN_EMPTY_ERROR;
  elvin_uri_from_string (&uri, [elvinUrl UTF8String], &error);
  
  if (elvin_error_occurred (&error))
    return NO;
  
  Keys *subscriptionKeys = subscriptionKeysFor (keys);
  
  if (elvin_open_with_keys (&elvin, &uri, EMPTY_KEYS, subscriptionKeys))
  {
    // renew any existing subscriptions
    for (SubscriptionContext *context in subscriptions)
      subscribe (&elvin, context);

    [[NSNotificationCenter defaultCenter] 
      postNotificationName: ElvinConnectionOpenedNotification object: self];
   
    elvin_add_close_listener (&elvin, (CloseListener)close_listener, self);
 
    return YES;
  } else
  {
    return NO;
  }
}

- (void) runElvinEventLoop
{
  while (elvin_is_open (&elvin) && elvin_error_ok (&elvin.error))
  {
    NSAutoreleasePool *pool = [NSAutoreleasePool new];
    
    elvin_poll (&elvin);
   
    if (elvin.error.code == ELVIN_ERROR_TIMEOUT)
      elvin_error_reset (&elvin.error);

    [pool release]; 
  }
}

void close_listener (Elvin *elvin, CloseReason reason,
                     const char *message, ElvinConnection *self)
{
  [[NSNotificationCenter defaultCenter] 
     postNotificationName: ElvinConnectionClosedNotification object: self];
}

Keys *subscriptionKeysFor (NSArray *keys)
{
  Keys *elvinKeys = elvin_keys_create ();
  
  for (NSDictionary *key in keys)
  {
    if ([[key valueForKey: @"Private"] isEqual: [NSNumber numberWithBool: YES]])
    {
      NSData *data = [key valueForKey: @"Data"];
      
      Key privateKey = 
        elvin_key_create_from_data ([data bytes], [data length]);
      
      elvin_keys_add_dual_consumer 
        (elvinKeys, KEY_SCHEME_SHA1_DUAL, privateKey);
      
      elvin_keys_add_dual_producer 
        (elvinKeys, KEY_SCHEME_SHA1_DUAL, 
           elvin_key_create_public (privateKey, KEY_SCHEME_SHA1_DUAL));
    } else
    {
      // TODO
    }
  }
  
  return elvinKeys;
}

Keys *notificationKeysFor (NSArray *keys)
{
  Keys *elvinKeys = elvin_keys_create ();
  
  for (NSDictionary *key in keys)
  {
    if ([[key valueForKey: @"Private"] isEqual: [NSNumber numberWithBool: YES]])
    {
      NSData *data = [key valueForKey: @"Data"];
      
      Key privateKey = 
        elvin_key_create_from_data ([data bytes], [data length]);
      
      elvin_keys_add_dual_producer 
        (elvinKeys, KEY_SCHEME_SHA1_DUAL, privateKey);
      
      elvin_keys_add_dual_consumer 
        (elvinKeys, KEY_SCHEME_SHA1_DUAL, 
           elvin_key_create_public (privateKey, KEY_SCHEME_SHA1_DUAL));
    } else
    {
      // TODO
    }
  }
  
  return elvinKeys;
}

@end
