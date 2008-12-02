#import "MessageWindowController.h"
#import "AppController.h"

@implementation MessageWindowController

- (void) sendMessage: (id) sender
{
  [appController sendMessage: [[messageText textStorage] string] 
                     toGroup: [group stringValue]];
  
  [[sender window] close];
}

@end
