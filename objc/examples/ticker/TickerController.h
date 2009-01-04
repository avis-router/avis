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
  IBOutlet id     replyButton;
  IBOutlet id     dragTarget;
  
  AppController * appController;
  NSString *      inReplyTo;
  BOOL            allowPublic;
}

@property (readwrite, retain) IBOutlet NSURL *    attachedURL;
@property (readwrite, retain) IBOutlet NSString * inReplyTo;
@property (readwrite, assign) IBOutlet BOOL       allowPublic;

- (id) initWithAppController: (AppController *) appController;

- (IBAction) sendMessage: (id) sender;

- (IBAction) clearAttachedURL: (id) sender;

- (IBAction) clearReply: (id) sender;

@end
