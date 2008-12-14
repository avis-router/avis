#import "ElvinConnection.h"

#include <avis/values.h>

#define UUID_STRING_LENGTH 100
#define USER_NAME_LENGTH   80

typedef struct
{
  id  object;
  SEL selector;
} Callback;

typedef struct
{
  NSString * subscription;
  Callback   callback;
} SubscribeContext;

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

static void getCurrentUser (char *userName)
{
  CFStringRef cfUserName = CSCopyUserName (false);
  
  CFStringGetCString (cfUserName, userName, USER_NAME_LENGTH, 
                      kCFStringEncodingUTF8);

  CFRelease (cfUserName);
}

@implementation ElvinConnection

- (id) initWithUrl: (NSString *) url lifecycleDelegate: (id) delegate
{
  if (![super init])
    return nil;
  
  elvinUrl = [url retain];
  lifecycleDelegate = delegate;
  
  [NSThread detachNewThreadSelector: @selector (elvinEventLoopThread) 
                           toTarget: self withObject: nil];
                           
  return self;
}

- (void) dealloc
{
  [self close];
  
  [super dealloc];
}

- (void) close
{
  if (elvin_is_open (&elvin) && elvin_error_ok (&elvin.error))
  {
    elvin_invoke_close (&elvin);
    
    while (elvin_is_open (&elvin) && elvin_error_ok (&elvin.error))
      usleep (100000);
  }
}

- (void) elvinEventLoopThread
{
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  
  elvin_open (&elvin, [elvinUrl UTF8String]);
  
  if (elvin_error_occurred (&elvin.error))
  {
    elvin_perror ("connect", &elvin.error);
    
    return;
  }
  
  NSLog (@"Opened!");

  if ([lifecycleDelegate 
        respondsToSelector: @selector (elvinConnectionDidOpen:)])
  {
    [lifecycleDelegate 
      performSelectorOnMainThread: @selector (elvinConnectionDidOpen:)
                       withObject: self waitUntilDone: YES];
  }
  
  [pool release];
  
  while (elvin_is_open (&elvin) && elvin_error_ok (&elvin.error))
  {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    
    elvin_poll (&elvin);
    
    [pool release]; 
  }
  
  NSLog (@"Exit elvin event loop");
}

static void notificationListener (Subscription *sub, 
                                  Attributes *attributes, 
                                  bool secure, Callback *callback)
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
  
  [callback->object performSelectorOnMainThread: callback->selector
                                     withObject: message 
                                  waitUntilDone: YES];
}

static void subscribe (Elvin *elvin, SubscribeContext *context)
{
  Subscription *sub = 
    elvin_subscribe (elvin, [context->subscription UTF8String]);
    
  Callback *callback = malloc (sizeof (Callback));
  *callback = context->callback;
  
  elvin_subscription_add_listener 
    (sub, (SubscriptionListener)notificationListener, callback);
    
  free (context);
}

- (void) subscribe: (NSString *) subscription
        withObject: (id) object usingHandler: (SEL) handler
{
  SubscribeContext *context = malloc (sizeof (SubscribeContext));
  
  context->subscription = subscription;
  context->callback.object = object;
  context->callback.selector = handler;
  
  elvin_invoke (&elvin, (InvokeHandler)subscribe, context);
}

static void sendMessage (Elvin *elvin, Attributes *message)
{
  elvin_send (elvin, message);
  
  attributes_destroy (message);
}

- (void) sendTickerMessage: (NSString *) messageText toGroup: (NSString *) group
           inReplyTo: (NSString *) replyToId
{
  NSAssert (group != nil && messageText != nil, @"IB connection failure");
  
  char messageID [UUID_STRING_LENGTH];
  char userName [USER_NAME_LENGTH];
  
  createUUID (messageID);
  getCurrentUser (userName);
  
  Attributes *message = attributes_create ();
  
  attributes_set_string (message, "Group", [group UTF8String]);
  attributes_set_string (message, "Message", [messageText UTF8String]);
  attributes_set_string (message, "From", userName);
  attributes_set_string (message, "Message-Id", messageID);
  attributes_set_string (message, "User-Agent", "Blue Sticker");
  attributes_set_int32  (message, "org.tickertape.message", 3001);
  
  if (replyToId != nil)
    attributes_set_string (message, "In-Reply-To", [replyToId UTF8String]);

  elvin_invoke (&elvin, (InvokeHandler)sendMessage, message);
}

@end
