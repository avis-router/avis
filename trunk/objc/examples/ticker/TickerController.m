#import "TickerController.h"

@implementation TickerController

typedef struct
{
  SEL method;
  void *param;
} Callback;

- (void) applicationWillTerminate: (NSNotification *)notification
{
  elvin_invoke_close (&elvin);
  
  CFMessagePortInvalidate (remoteCocoaPort); // do we need to do this?
  CFRelease (remoteCocoaPort);
  
  while (elvin_is_open (&elvin))
    usleep (200000);
}

static void runCocoaCallback (CFMessagePortRef cocoaPort, SEL method, 
                              void *param)
{
  Callback callback = {method, param};
  
  CFDataRef data = CFDataCreate (NULL, (void *)&callback, sizeof (callback));
  
  // send message async, no timeout
  if (CFMessagePortSendRequest 
      (cocoaPort, 0, data, 0, 0, NULL, NULL) != kCFMessagePortSuccess)
  {
    NSLog (@"Failed to send message into Cocoa event loop");
  }
  
  CFRelease (data);  
}

static CFDataRef cocoaEventLoopCallback (CFMessagePortRef local, SInt32 msgid,
                                         CFDataRef data, id instance) 
{
  Callback *callback = (Callback *)CFDataGetBytePtr (data);
  
  objc_msgSend (instance, callback->method, callback->param);
  
  return NULL;
}

static void elvinNotificationListener (Elvin *elvin, Attributes *attributes, 
                                       bool secure, CFMessagePortRef cocoaPort)
{
  char *message = strdup (attributes_get_string (attributes, "Message"));
  
  runCocoaCallback (cocoaPort, @selector (handleNotify:), (void *)message);
}

- (void) handleNotify: (char *)message
{
  NSString *messageString = [NSString stringWithUTF8String: message];
  NSTextView *textView = [text documentView];
  NSRange endRange;

  endRange.location = [[textView textStorage] length];
  endRange.length = 0;
  
  [textView replaceCharactersInRange: endRange withString: messageString];

  endRange.location = [[textView textStorage] length];
  [textView replaceCharactersInRange: endRange withString: @"\n"];

  endRange.length = [messageString length];
  [textView scrollRangeToVisible: endRange];
  
  free (message);
}

- (void) elvinEventLoopThread: (NSObject *)object
{
  elvin_open (&elvin, "elvin://elvin");
  
  if (elvin_error_occurred (&elvin.error))
  {
    elvin_perror ("connect", &elvin.error);
    
    return;
  } else
  {
    NSLog (@"Opened!");
    
    elvin_subscribe (&elvin, "require (Message)");
    
    elvin_add_notification_listener 
      (&elvin, (GeneralNotificationListener)elvinNotificationListener, 
       remoteCocoaPort);
    
    elvin_event_loop (&elvin);
  }
  
  NSLog (@"Exit elvin event loop");
}

- (void) awakeFromNib
{    
  CFMessagePortContext context;
  
  memset (&context, 0, sizeof (context));
  context.info = self;
  
  CFMessagePortRef localCocoaPort =
    CFMessagePortCreateLocal 
      (NULL, CFSTR ("Elvin"), (CFMessagePortCallBack)cocoaEventLoopCallback, 
       &context, false);
  
  CFRunLoopSourceRef source = 
    CFMessagePortCreateRunLoopSource (NULL, localCocoaPort, 0);
  
  CFRunLoopAddSource (CFRunLoopGetCurrent (), source, kCFRunLoopDefaultMode);

  CFRelease (localCocoaPort); // should we release this here?

  remoteCocoaPort = CFMessagePortCreateRemote (NULL, CFSTR ("Elvin"));
  
  [NSThread detachNewThreadSelector: @selector (elvinEventLoopThread:) 
                           toTarget: self withObject: nil];
}

@end
