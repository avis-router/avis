#import "AppController.h"
#import "TickerController.h"

@implementation AppController

#define UUID_STRING_LENGTH 100
#define USER_NAME_LENGTH   80

typedef struct
{
  id  object;
  SEL selector;
} Callback;

#define attr_string(attrs, name) \
  [NSString stringWithUTF8String: attributes_get_string (attrs, name)]

static void createUUID (char *uuid)
{
  CFUUIDRef     cfUUID;
  CFStringRef   cfUUIDString;
  
  cfUUID = CFUUIDCreate (kCFAllocatorDefault);
  cfUUIDString = CFUUIDCreateString (kCFAllocatorDefault, cfUUID);
  
  CFStringGetCString (cfUUIDString, uuid, UUID_STRING_LENGTH, 
                      kCFStringEncodingASCII);
  
  CFRelease (cfUUIDString);
  CFRelease (cfUUID);
}

static void getCurrentUser (char *userName)
{
  CFStringRef cfUserName = CSCopyUserName (FALSE);
  
  CFStringGetCString (cfUserName, userName, USER_NAME_LENGTH, 
                      kCFStringEncodingUTF8);
  
  CFRelease (cfUserName);
}

- (void) elvinEventLoopThread
{
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  
  elvin_open (&elvin, "elvin://elvin");
  
  if (elvin_error_occurred (&elvin.error))
  {
    elvin_perror ("connect", &elvin.error);
    
    return;
  }
  
  NSLog (@"Opened!");
  
  [self performSelectorOnMainThread: @selector (openTickerWindow)
                         withObject: nil 
                      waitUntilDone: YES];
  
  [pool release];
  
  while (elvin_is_open (&elvin) && elvin_error_ok (&elvin.error))
  {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    
    elvin_poll (&elvin);
    
    [pool release]; 
  }
  
  NSLog (@"Exit elvin event loop");
}

- (void) applicationDidFinishLaunching: (NSNotification *) notification 
{
  NSLog (@"Start Elvin Thread");
  
  [NSThread detachNewThreadSelector: @selector (elvinEventLoopThread) 
                           toTarget: self withObject: nil];  
}

- (void) applicationWillTerminate: (NSNotification *) notification
{
  elvin_invoke_close (&elvin);
  
  while (elvin_is_open (&elvin))
    usleep (200000);
}

- (void) createNewMessage: (id) sender
{
  if (![NSBundle loadNibNamed: @"MessageWindow" owner: self])
  {
    NSLog (@"No nib file for message?");
    return;
  }
  
  // TODO this is leaking the MessageWindowController object
  // possibly due to circular ref?
  
  [messageWindow makeKeyAndOrderFront: nil];
}

- (void) openTickerWindow
{
  NSLog (@"Open Ticker Window");
  
  if (![NSBundle loadNibNamed: @"TickerWindow" owner: self])
  {
    NSLog (@"No nib file for ticker?");
    return;
  }
  
  [tickerWindow makeKeyAndOrderFront: nil];
}

static void elvinNotificationListener (Subscription *sub, 
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

  NSDictionary *message = 
    [NSDictionary dictionaryWithObjects: objects forKeys: keys];

  [callback->object performSelectorOnMainThread: callback->selector
                                     withObject: message 
                                  waitUntilDone: YES];
}
  
- (void) subscribe: (NSString *) subscription
        withObject: (id) object usingHandler: (SEL) handler
{
  // TODO not thread safe
  Subscription *sub = elvin_subscribe (&elvin, [subscription UTF8String]);
  Callback *callback = malloc (sizeof (Callback));
  
  callback->object = object;
  callback->selector = handler;
  
  elvin_subscription_add_listener 
    (sub, (SubscriptionListener)elvinNotificationListener, callback);
}

static void doSendMessage (Elvin *elvin, Attributes *message)
{
  elvin_send (elvin, message);
  
  attributes_destroy (message);
}

- (void) sendMessage: (NSString *) messageText toGroup: (NSString *) group
{
  char messageID [UUID_STRING_LENGTH];
  char userName [USER_NAME_LENGTH];
  
  createUUID (messageID);
  getCurrentUser (userName);
  
  NSLog (@"Send message %@ to %@", messageText, group);
  
  Attributes *message = attributes_create ();
  
  attributes_set_string (message, "Group", [group UTF8String]);
  attributes_set_string (message, "Message", [messageText UTF8String]);
  attributes_set_string (message, "From", userName);
  attributes_set_string (message, "Message-Id", messageID);
  attributes_set_int32  (message, "org.tickertape.message", 3001);

  elvin_invoke (&elvin, (InvokeHandler)doSendMessage, message);
}

@end
