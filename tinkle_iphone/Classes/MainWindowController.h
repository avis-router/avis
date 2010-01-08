#import <UIKit/UIKit.h>

#import <Three20/Three20.h>

@class PresenceViewController;
@class MessagesViewController;

@interface MainWindowController : UIViewController <TTScrollViewDataSource>
{
//  TTScrollView *scrollView;
  
  IBOutlet PresenceViewController *presenceController;
  IBOutlet MessagesViewController *messagesController;
  IBOutlet UISegmentedControl *panelSelector;
  IBOutlet UIView *contentView;
}

- (IBAction) panelSelectorItemChanged: (id) sender;

- (IBAction) showPreferences: (id) sender;

@end
