#import <Cocoa/Cocoa.h>

@class AppController;
@class RolloverButton;

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
  
  IBOutlet RolloverButton *replyButton;
  
  AppController * appController;
  NSString *      inReplyTo;
  BOOL            allowPublic;
  BOOL            canSend;
}

@property (readwrite, retain) IBOutlet NSURL *    attachedURL;
@property (readwrite, retain) IBOutlet NSString * inReplyTo;
@property (readwrite, assign) IBOutlet BOOL       allowPublic;
@property (readwrite, assign) IBOutlet BOOL       canSend;

- (id) initWithAppController: (AppController *) appController;

- (IBAction) sendMessage: (id) sender;

- (IBAction) clearAttachedURL: (id) sender;

- (IBAction) clearReply: (id) sender;

- (IBAction) togglePublic: (id) sender;

@end
