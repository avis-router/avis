#import <Cocoa/Cocoa.h>

@class AppController;

@interface TickerController : NSWindowController
{
  IBOutlet id     tickerMessagesTextView;
  IBOutlet id     messageGroup;
  IBOutlet id     messageText;
  IBOutlet id     publicCheckbox;
  IBOutlet id     attachedUrlLabel;
  IBOutlet id     attachedUrlPanel;
  IBOutlet id     sendButton;
  IBOutlet id     dragTarget;
  
  AppController * appController;
  NSString *      replyToMessageId;
}

- (id) initWithAppController: (AppController *) appController;

- (IBAction) sendMessage: (id) sender;

- (IBAction) clearAttachedURL: (id) sender;

- (void) setAttachedURL: (NSURL *) url;

- (NSURL *) attachedURL;

@end

