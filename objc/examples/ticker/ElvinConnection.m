#import "ElvinConnection.h"

#define UUID_STRING_LENGTH 100

NSString *ElvinConnectionOpenedNotification = @"ElvinConnectionOpened";
NSString *ElvinConnectionClosedNotification = @"ElvinConnectionClosed";

#pragma mark PRIVATE Utility functions

static inline NSString *attr_string (Attributes *attrs, const char *name)
{
  return [NSString stringWithUTF8String: attributes_get_string (attrs, name)];
}

static void createUUID (char *uuid)
{
  CFUUIDRef cfUUID = CFUUIDCreate (kCFAllocatorDefault);
  CFStringRef cfUUIDString = CFUUIDCreateString (kCFAllocatorDefault, cfUUID);
  
  CFStringGetCString (cfUUIDString, uuid, UUID_STRING_LENGTH, 
                      kCFStringEncodingASCII);
  
  CFRelease (cfUUIDString);
  CFRelease (cfUUID);
}

static void copy_string_attr (Attributes *attributes, 
                              NSMutableDictionary *message,
                              NSString *name)
{
  const char *attrValue = attributes_get_string (attributes, [name UTF8String]);
  
  if (attrValue)
    [message setValue: [NSString stringWithUTF8String: attrValue] forKey: name];
}

#pragma mark PRIVATE SubscriptionContext internal class

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

#pragma mark PRIVATE ElvinConnection

static void subscribe (Elvin *elvin, SubscriptionContext *context);

static void close_listener (Elvin *elvin, CloseReason reason,
                            const char *message,
                            ElvinConnection *self);

static void notification_listener (Subscription *sub, 
                                   Attributes *attributes, 
                                   bool secure, SubscriptionContext *context);

static void send_message (Elvin *elvin, Attributes *message);

@interface ElvinConnection ()
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

#pragma mark PUBLIC

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

#pragma PUBLIC Elvin publish/subscribe

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

static void subscribe (Elvin *elvin, SubscriptionContext *context)
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
  NSArray *keys = 
    [NSArray arrayWithObjects: @"Message", @"Group", @"From", nil];

  NSArray *objects = 
    [NSArray arrayWithObjects: 
      attr_string (attributes, "Message"), 
      attr_string (attributes, "Group"),
      attr_string (attributes, "From"), nil];

  NSMutableDictionary *message = 
    [NSMutableDictionary dictionaryWithObjects: objects forKeys: keys];

  copy_string_attr (attributes, message, @"Message-Id");
  copy_string_attr (attributes, message, @"Distribution");
  copy_string_attr (attributes, message, @"MIME_TYPE");
  copy_string_attr (attributes, message, @"MIME_ARGS");
  copy_string_attr (attributes, message, @"Attachment");
  copy_string_attr (attributes, message, @"User-Agent");
  
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
  // TODO error on closed connection
  NSAssert (group != nil && messageText != nil, @"IB connection failure");
  
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

#pragma mark PRIVATE Elvin event loop

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
