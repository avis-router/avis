#import "TickerController.h"
#import "AppController.h"

@implementation TickerController

- (void) handleNotify: (NSDictionary *) message
{
  NSRange endRange;
  NSString *messageText = 
    [NSString stringWithFormat: @">>> %@: %@: %@\n",
      [message objectForKey: @"Group"],
      [message objectForKey: @"From"],
      [message objectForKey: @"Message"]];
  
  endRange.location = [[text textStorage] length];
  endRange.length = 0;
  
  [text replaceCharactersInRange: endRange withString: messageText];

  endRange.location = [[text textStorage] length];
  [text scrollRangeToVisible: endRange];
  
  [message release];
}

- (void) awakeFromNib
{ 
  [appController 
    subscribe: @"string (Message) && string (Group) && string (From)" 
    withObject: self
    usingHandler: @selector (handleNotify:)];
}

@end
