#import <UIKit/UIKit.h>

extern NSString *TickerMessageReceivedNotification;

@class ElvinConnection;

@interface MessagesViewController : UIViewController 
{
  ElvinConnection      *elvin;
  NSString             *subscription;
  NSString             *group;
  id                    subscriptionContext;
  BOOL                  canSend;
  BOOL                  keyboardShown;
  
  IBOutlet UITextView  *messagesTextView;
  IBOutlet UIButton    *sendButton;
  IBOutlet UITextField *messageCompositionField;
}

@property (readwrite, retain) ElvinConnection   *elvin;
@property (readwrite, retain) NSString          *group;
@property (readwrite, retain) IBOutlet NSString *subscription;
@property (readwrite, assign) IBOutlet BOOL      canSend;

- (IBAction) sendMessage: (id) sender;

- (IBAction) selectGroup: (id) sender;

@end
