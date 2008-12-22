#import <Cocoa/Cocoa.h>

#import "AppController.h"

@interface TickerController : NSObject
{
  IBOutlet id              tickerMessagesTextView;
  IBOutlet id              messageGroup;
  IBOutlet id              messageText;
  IBOutlet id              publicCheckbox;
  IBOutlet id              attachedUrlLabel;
  IBOutlet id              attachedUrlPanel;
  
  IBOutlet AppController * appController;
  
  NSString *               replyToMessageId;
}

- (IBAction) sendMessage: (id) sender;

- (IBAction) clearAttachedURL: (id) sender;

- (void) setAttachedURL: (NSURL *) url;

- (NSURL *) attachedURL;

@end

