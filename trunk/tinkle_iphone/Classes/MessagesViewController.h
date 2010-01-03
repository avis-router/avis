#import <UIKit/UIKit.h>

extern NSString *TickerMessageReceivedNotification;

@class ElvinConnection;

@interface MessagesViewController : UIViewController 
{
  ElvinConnection      *elvin;
  NSString             *subscription;
  id                    subscriptionContext;
  BOOL                  canSend;
  BOOL                  keyboardShown;
  CGFloat               startViewVertOffset;
  UIViewController     *parentViewController;
  IBOutlet UITextView  *messagesTextView;
  IBOutlet UIButton    *sendButton;
  IBOutlet UITextField *messageCompositionField;
}

@property (readwrite, retain) ElvinConnection   *elvin;
@property (readwrite, assign) IBOutlet BOOL      canSend;
@property (readwrite, assign, nonatomic) UIViewController *parentViewController;

- (IBAction) sendMessage: (id) sender;

- (IBAction) selectGroup: (id) sender;

- (void) subscribe;

- (NSString *) subscription;

- (void) updateSendGroup;

@end