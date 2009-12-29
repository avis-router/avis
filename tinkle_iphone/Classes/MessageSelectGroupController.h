#import <UIKit/UIKit.h>

@interface MessageSelectGroupController : 
  UIViewController <UITableViewDelegate, UITableViewDataSource>
{
  NSMutableArray *groups;
  
  IBOutlet UITableView *groupsList;
  IBOutlet UITextField *groupTextField;
}

- (IBAction) cancel: (id) sender;

- (void) selectGroup: (NSString *) groupName;

- (IBAction) addButtonClicked: (id) sender;

@end
