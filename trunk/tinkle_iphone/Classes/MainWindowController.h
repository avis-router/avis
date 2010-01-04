#import <UIKit/UIKit.h>

@class PresenceViewController;
@class MessagesViewController;

@interface MainWindowController : UIViewController
{
  IBOutlet PresenceViewController *presenceController;
  IBOutlet MessagesViewController *messagesController;
  IBOutlet UISegmentedControl *panelSelector;
  IBOutlet UIView *contentView;
}

- (IBAction) panelSelectorItemChanged: (id) sender;

- (IBAction) showPreferences: (id) sender;

@end
