#import <UIKit/UIKit.h>

@class PresenceTableViewController;
@class MessagesViewController;

@interface MainWindowController : UIViewController
{
  IBOutlet PresenceTableViewController *presenceController;
  IBOutlet MessagesViewController *messagesController;
  IBOutlet UISegmentedControl *panelSelector;
  IBOutlet UIView *contentView;
}

- (IBAction) panelSelectorItemChanged: (id) sender;

@end
