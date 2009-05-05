#import <Cocoa/Cocoa.h>

@class ElvinConnection;
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
  
  ElvinConnection * elvin;
  NSString *        subscription;
  id                subscriptionContext;
  NSString *        inReplyTo;
  BOOL              allowPublic;
  BOOL              allowInsecure;
  BOOL              canSend;
}

@property (readwrite, retain) IBOutlet NSString * subscription;
@property (readwrite, retain) IBOutlet NSURL *    attachedURL;
@property (readwrite, retain) IBOutlet NSString * inReplyTo;
@property (readwrite, assign) IBOutlet BOOL       allowPublic;
@property (readwrite, assign) IBOutlet BOOL       allowInsecure;
@property (readwrite, assign) IBOutlet BOOL       canSend;

- (id) initWithElvin: (ElvinConnection *) theElvinConnection 
        subscription: (NSString *) theSubscription;

- (IBAction) sendMessage: (id) sender;

- (IBAction) clearAttachedURL: (id) sender;

- (IBAction) clearReply: (id) sender;

- (IBAction) togglePublic: (id) sender;

- (IBAction) toggleSecure: (id) sender;

@end