#import <UIKit/UIKit.h>

@interface MessageSelectGroupController : 
  UIViewController <UITableViewDelegate, UITableViewDataSource>
{
  NSArray *groups;
  
  IBOutlet UITableView *groupsList;
  IBOutlet UITextField *groupTextField;
}

- (IBAction) cancel: (id) sender;

- (void) selectGroup: (NSString *) groupName;

- (IBAction) addButtonClicked: (id) sender;

@end
