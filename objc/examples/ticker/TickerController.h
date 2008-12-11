#import <Cocoa/Cocoa.h>

@interface TickerController : NSObject
{
  IBOutlet id tickerMessagesScroller;
  IBOutlet id tickerMessagesTextView;
  IBOutlet id messageGroup;
  IBOutlet id messageText;
  IBOutlet id appController;
  
  NSString *replyToMessageId;
}

- (IBAction) sendMessage: (id) sender;

@end

