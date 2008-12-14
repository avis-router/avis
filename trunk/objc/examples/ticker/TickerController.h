#import <Cocoa/Cocoa.h>

#import "AppController.h"

@interface TickerController : NSObject
{
  IBOutlet id              tickerMessagesScroller;
  IBOutlet id              tickerMessagesTextView;
  IBOutlet id              messageGroup;
  IBOutlet id              messageText;
  IBOutlet AppController * appController;
  
  NSString *               replyToMessageId;
}

- (IBAction) sendMessage: (id) sender;

@end

