#import <UIKit/UIKit.h>

@interface PreferencesController : 
  UIViewController <UITableViewDelegate, UITableViewDataSource>
{
  NSArray *prefsInfo;
  
  IBOutlet UITableViewCell *userNameCell;
  IBOutlet UITextField *userNameTextField;
  
  IBOutlet UITableViewCell *elvinUrlCell;
  IBOutlet UITextField *elvinUrlTextField;
}

- (IBAction) doneClicked: (id) sender;

@end
