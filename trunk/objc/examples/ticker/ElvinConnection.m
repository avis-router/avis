#import "ElvinConnection.h"

#import "utils.h"

NSString *ElvinConnectionOpenedNotification = @"ElvinConnectionOpened";
NSString *ElvinConnectionClosedNotification = @"ElvinConnectionClosed";

#pragma mark -

@class SubscriptionContext;

static void subscribe (Elvin *elvin, SubscriptionContext *context);

static void close_listener (Elvin *elvin, CloseReason reason,
                            const char *message,
                            ElvinConnection *self);

static void notification_listener (Subscription *sub, 
                                   Attributes *attributes, 
                                   bool secure, SubscriptionContext *context);

static void send_message (Elvin *elvin, Attributes *message);

#pragma mark -

@interface SubscriptionContext : NSObject
{
  @public
  
  NSString *     subscriptionExpr;
  Subscription * subscription;
  id             delegate;
  SEL            selector;
}

@end

@implementation SubscriptionContext

+ (id) context: (NSString *) newSubscriptionExpr 
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

@end

#pragma mark -

@interface ElvinConnection (PRIVATE)
  - (BOOL) openConnection;
  - (void) elvinEventLoopThread;
  - (void) runElvinEventLoop;
@end

@implementation ElvinConnection

@dynamic elvinUrl;

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

- (BOOL) isConnected
{
  return elvin_is_open (&elvin);
}

- (void) connect
{
  eventLoopThread = 
    [[NSThread alloc] 
       initWithTarget: self selector: @selector (elvinEventLoopThread) 
       object: nil];
                             
  [eventLoopThread start];
}

- (void) disconnect
{
  [eventLoopThread cancel];
  
  if (elvin_is_open (&elvin) && elvin_error_ok (&elvin.error))
  {
    NSLog (@"Disconnect from Elvin");
    
    elvin_invoke_close (&elvin);    
  }
  
  while (![eventLoopThread isFinished])
    usleep (100000);
  
  // nuke defunct Elvin subscription pointers
  for (SubscriptionContext *context in subscriptions)
    context->subscription = nil;
}

#pragma Elvin publish/subscribe

- (void) subscribe: (NSString *) subscriptionExpr
        withDelegate: (id) delegate usingSelector: (SEL) selector
{
  SubscriptionContext *context = 
    [SubscriptionContext context: subscriptionExpr 
                         delegate: delegate selector: selector];
  
  [subscriptions addObject: context];
  
  if (elvin_is_open (&elvin))
    elvin_invoke (&elvin, (InvokeHandler)subscribe, context);
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
  NSMutableDictionary *message = [NSMutableDictionary dictionary];
  
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
  
  [context->delegate performSelectorOnMainThread: context->selector
                                      withObject: message 
                                   waitUntilDone: YES];
}

- (void) sendTickerMessage: (NSString *) messageText 
                fromSender: (NSString *) from
                   toGroup: (NSString *) group
                 inReplyTo: (NSString *) replyToId 
               attachedURL: (NSURL *) url
                sendPublic: (BOOL) isPublic
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
  
  elvin_invoke (&elvin, (InvokeHandler)send_message, message);
}

- (void) sendPresenceRequestMessage: (NSString *) userID 
                      fromRequestor: (NSString *) requestor
                           toGroups: (NSString *) groups 
                           andUsers: (NSString *) users
                         sendPublic: (BOOL) isPublic
{
  Attributes *message = attributes_create ();
  
  attributes_set_string (message, "Presence-Request",  [userID UTF8String]);
  attributes_set_string (message, "Requestor",         [requestor UTF8String]);
  attributes_set_int32  (message, "Presence-Protocol", 1000);
  attributes_set_string (message, "Groups",            [groups UTF8String]);
  attributes_set_string (message, "Users",             [users UTF8String]);
  
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

#pragma mark Event loop

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
  if (elvin_open (&elvin, [elvinUrl UTF8String]))
  {    
    // renew any existing subscriptions
    for (SubscriptionContext *context in subscriptions)
      subscribe (&elvin, context);
        
    [[NSNotificationCenter defaultCenter] 
      postNotificationName: ElvinConnectionOpenedNotification object: self];
   
    elvin_add_close_listener (&elvin, (CloseListener)close_listener, self);
 
    return TRUE;
  } else
  {           
    return FALSE;
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

@end
