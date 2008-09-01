#import "TickerController.h"

@implementation TickerController

- (void) applicationWillTerminate: (NSNotification *)notification
{
  elvin_invoke_close (&elvin);
  
  while (elvin_is_open (&elvin))
    usleep (200000);
}

#define attr_string(attrs, name) \
 [NSString stringWithUTF8String: attributes_get_string (attrs, name)]

static void elvinNotificationListener (Elvin *elvin, Attributes *attributes, 
                                       bool secure, id self)
{
  NSArray *objects = 
    [NSArray arrayWithObjects: 
      attr_string (attributes, "Message"), 
      attr_string (attributes, "Group"),
      attr_string (attributes, "From"), nil];
     
  NSArray *keys = 
    [NSArray arrayWithObjects: @"Message", @"Group", @"From", nil];

  NSDictionary *message = 
   [NSDictionary dictionaryWithObjects: objects forKeys: keys];
  
  [message retain];
  
  [self performSelectorOnMainThread: @selector (handleNotify:) 
        withObject:message 
        waitUntilDone:NO];
}

- (void) handleNotify: (NSDictionary *)attributes
{
  NSTextView *textView = [text documentView];
  NSRange endRange;
  NSString *messageText = 
    [NSString stringWithFormat: @">>> %@: %@: %@\n",
     [attributes objectForKey: @"Group"],
     [attributes objectForKey: @"From"],
     [attributes objectForKey: @"Message"]];
  
  endRange.location = [[textView textStorage] length];
  endRange.length = 0;
  
  [textView replaceCharactersInRange: endRange 
                          withString: messageText];

  endRange.location = [[textView textStorage] length];
  [textView scrollRangeToVisible: endRange];
  
  [attributes release];
}

- (void) elvinEventLoopThread: (NSObject *)object
{
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  
  elvin_open (&elvin, "elvin://elvin");
  
  if (elvin_error_occurred (&elvin.error))
  {
    elvin_perror ("connect", &elvin.error);
    
    return;
  }
  
  NSLog (@"Opened!");
  
  elvin_subscribe (&elvin, 
                   "string (Message) && string (Group) && string (From)");
  
  elvin_add_notification_listener 
    (&elvin, (GeneralNotificationListener)elvinNotificationListener, 
     self);

  [pool release];
  
  while (elvin_is_open (&elvin) && elvin_error_ok (&elvin.error))
  {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    
    elvin_poll (&elvin);
   
    NSLog (@"Release!");
    
    [pool release]; 
  }
  
  NSLog (@"Exit elvin event loop");
}

- (void) awakeFromNib
{      
  [NSThread detachNewThreadSelector: @selector (elvinEventLoopThread:) 
                           toTarget: self withObject: nil];
}

@end
