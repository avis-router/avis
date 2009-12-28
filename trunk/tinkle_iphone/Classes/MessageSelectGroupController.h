#import <UIKit/UIKit.h>

@interface MessageSelectGroupController : 
  UIViewController <UITableViewDelegate, UITableViewDataSource>
{
  NSArray *groups;
  NSString *group;
  id delegate;
  
  IBOutlet UITableView *groupsList;
  IBOutlet UITextField *groupTextField;
}

@property (readwrite, retain) NSArray *groups;
@property (readwrite, retain) NSString *group;
@property (readwrite, assign) id delegate;

- (IBAction) cancel: (id) sender;

- (IBAction) groupSelected: (id) sender;

- (IBAction) addButtonClicked: (id) sender;

@end
