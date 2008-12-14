#import "ElvinConnection.h"

#define UUID_STRING_LENGTH 100
#define USER_NAME_LENGTH   80

#define attr_string(attrs, name) \
  [NSString stringWithUTF8String: attributes_get_string (attrs, name)]

static void createUUID (char *uuid)
{
  CFUUIDRef cfUUID;
  CFStringRef cfUUIDString;
  
  cfUUID = CFUUIDCreate (kCFAllocatorDefault);
  cfUUIDString = CFUUIDCreateString (kCFAllocatorDefault, cfUUID);
  
  CFStringGetCString (cfUUIDString, uuid, UUID_STRING_LENGTH, 
                      kCFStringEncodingASCII);
  
  CFRelease (cfUUIDString);
  CFRelease (cfUUID);
}

@interface SubscriptionContext : NSObject
{
  @public
  
  NSString *subscriptionExpr;
  Subscription *subscription;
  id  delegate;
  SEL selector;
}

@end

@implementation SubscriptionContext

- (id) initWithSubscription: (NSString *) newSubscriptionExpr 
      delegate: (id) newDelegate selector: (SEL) newSelector
{
  if (![super init])
    return nil;
  
  subscriptionExpr = [newSubscriptionExpr retain];
  delegate = newDelegate;
  selector = newSelector;
  
  return self;
}

- (void) dealloc
{
  [super dealloc];
  
  [subscriptionExpr release];
}

@end

@implementation ElvinConnection

static void subscribe (Elvin *elvin, SubscriptionContext *context);

- (id) initWithUrl: (NSString *) url lifecycleDelegate: (id) delegate
{
  if (![super init])
    return nil;
  
  elvinUrl = [url retain];
  lifecycleDelegate = delegate;
  subscriptions = [[NSMutableArray arrayWithCapacity: 5] retain];

  [self connect];
    
  return self;
}

- (void) dealloc
{
  [self disconnect];
    
  [subscriptions release];

  [super dealloc];
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
    
    while (elvin_is_open (&elvin) && elvin_error_ok (&elvin.error))
      usleep (100000);
  }
  
  // nuke defunct Elvin subscription pointers
  for (SubscriptionContext *context in subscriptions)
    context->subscription = nil;
}

- (void) elvinEventLoopThread
{
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

  NSLog (@"Start connect to Elvin");
  
  // loop until connected or cancelled
  for (;;) 
  {  
    elvin_open (&elvin, [elvinUrl UTF8String]);

    if (elvin_error_occurred (&elvin.error))
    {
      NSLog (@"Failed to connect to elvin %@: %s (%i)", elvinUrl, 
             elvin.error.message, elvin.error.code);
             
      [NSThread sleepForTimeInterval: 5];
    } else if ([eventLoopThread isCancelled])
    {
      return;
    } else
    {
      break;
    }
  }
  
  NSLog (@"Connected to Elvin at %@", elvinUrl);

  // renew any existing subscriptions
  for (SubscriptionContext *context in subscriptions)
    subscribe (&elvin, context);
      
  // let delegate know we're ready
  if ([lifecycleDelegate 
        respondsToSelector: @selector (elvinConnectionDidOpen:)])
  {
    [lifecycleDelegate 
      performSelectorOnMainThread: @selector (elvinConnectionDidOpen:)
                       withObject: self waitUntilDone: YES];
  }
  
  [pool release];
  
  // start event loop
  while (elvin_is_open (&elvin) && elvin_error_ok (&elvin.error))
  {
    pool = [[NSAutoreleasePool alloc] init];
    
    elvin_poll (&elvin);
   
    NSLog (@"Polled");
    
    [pool release]; 
  }
  
  elvin_close (&elvin);
  
  NSLog (@"Exit elvin event loop");
}

static void notificationListener (Subscription *sub, 
                                  Attributes *attributes, 
                                  bool secure, SubscriptionContext *context)
{
  // TODO copy all attrs
  NSArray *keys = 
    [NSArray arrayWithObjects: @"Message", @"Group", @"From", nil];

  NSArray *objects = 
    [NSArray arrayWithObjects: 
      attr_string (attributes, "Message"), 
      attr_string (attributes, "Group"),
      attr_string (attributes, "From"), nil];

  NSMutableDictionary *message = 
    [NSMutableDictionary dictionaryWithObjects: objects forKeys: keys];

  const char *messageIdAttr = attributes_get_string (attributes, "Message-Id");
  
  if (messageIdAttr)
  {
    [message setValue: [NSString stringWithUTF8String: messageIdAttr]
                                               forKey: @"Message-Id"];
  }
  
  [context->delegate performSelectorOnMainThread: context->selector
                                     withObject: message 
                                  waitUntilDone: YES];
}

static void subscribe (Elvin *elvin, SubscriptionContext *context)
{
  context->subscription = 
    elvin_subscribe (elvin, [context->subscriptionExpr UTF8String]);
  
  if (elvin_error_ok (&elvin->error))
  {
    elvin_subscription_add_listener 
      (context->subscription, (SubscriptionListener)notificationListener, 
       context);
  } else
  {
    NSLog (@"Error while trying to subscribe: %s (%i)", 
           elvin->error.message, elvin->error.code);
  }
}

- (void) subscribe: (NSString *) subscriptionExpr
        withObject: (id) object usingHandler: (SEL) handler
{
  SubscriptionContext *context = 
    [[[SubscriptionContext alloc] initWithSubscription: subscriptionExpr 
                                  delegate: object selector: handler] retain];

  [subscriptions addObject: context];
  
  elvin_invoke (&elvin, (InvokeHandler)subscribe, context);
}

static void sendMessage (Elvin *elvin, Attributes *message)
{
  elvin_send (elvin, message);
  
  if (elvin_error_occurred (&elvin->error))
  {
    NSLog (@"Error while trying to send message: %s (%i)", 
           elvin->error.message, elvin->error.code);
  }
  
  attributes_destroy (message);
}

- (void) sendTickerMessage: (NSString *) messageText toGroup: (NSString *) group
           inReplyTo: (NSString *) replyToId
{
  NSAssert (group != nil && messageText != nil, @"IB connection failure");
  
  char messageID [UUID_STRING_LENGTH];
  
  createUUID (messageID);
  
  Attributes *message = attributes_create ();
  
  attributes_set_string (message, "Group", [group UTF8String]);
  attributes_set_string (message, "Message", [messageText UTF8String]);
  attributes_set_string (message, "From", [NSFullUserName () UTF8String]);
  attributes_set_string (message, "Message-Id", messageID);
  attributes_set_string (message, "User-Agent", "Blue Sticker");
  attributes_set_int32  (message, "org.tickertape.message", 3001);
  
  if (replyToId != nil)
    attributes_set_string (message, "In-Reply-To", [replyToId UTF8String]);

  elvin_invoke (&elvin, (InvokeHandler)sendMessage, message);
}

@end
