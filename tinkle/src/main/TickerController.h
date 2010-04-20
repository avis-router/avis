#import <Cocoa/Cocoa.h>

#import "TextViewWithLinks.h"

extern NSString * TickerMessageReceivedNotification;
extern NSString * TickerMessageStartedEditingNotification;
extern NSString * TickerMessageStoppedEditingNotification;

@class ElvinConnection;
@class RolloverButton;
@class TickerMessage;

@interface TickerController : NSWindowController <LinkCallbacks>
{
  IBOutlet id     tickerMessagesTextView;
  IBOutlet id     messageGroup;
  IBOutlet id     messageText;
  IBOutlet id     publicCheckbox;
  IBOutlet id     attachedUrlLabel;
  IBOutlet id     attachedUrlPanel;
  IBOutlet id     sendButton;
  IBOutlet id     dragTarget;
  
  IBOutlet NSArrayController * tickerGroupsController;
  IBOutlet RolloverButton    * replyButton;
  
  ElvinConnection * elvin;
  NSString *        subscription;
  id                subscriptionContext;
  TickerMessage   * inReplyTo;
  NSRange           inReplyToHighlightedRange;
  NSMutableArray  * recentMessages;
  BOOL              allowPublic;
  BOOL              allowInsecure;
  BOOL              canSend;
  BOOL              tickerIsEditing;
}

@property (readwrite, retain) IBOutlet NSString      * subscription;
@property (readwrite, retain) IBOutlet NSURL         * attachedURL;
@property (readwrite, retain) IBOutlet TickerMessage * inReplyTo;
@property (readwrite, assign) IBOutlet BOOL            allowPublic;
@property (readwrite, assign) IBOutlet BOOL            allowInsecure;
@property (readwrite, assign) IBOutlet BOOL            canSend;
@property (readwrite, assign) IBOutlet BOOL            highlightReplyTo;
@property (readonly) NSString *inReplyToTooltip;

- (id) initWithElvin: (ElvinConnection *) theElvinConnection 
        subscription: (NSString *) theSubscription;

- (IBAction) sendMessage: (id) sender;
- (IBAction) clearAttachedURL: (id) sender;
- (IBAction) pasteURL: (id) sender;
- (IBAction) replyToLastMessage: (id) sender;
- (IBAction) clearReply: (id) sender;
- (IBAction) togglePublic: (id) sender;
- (IBAction) toggleSecure: (id) sender;

@end