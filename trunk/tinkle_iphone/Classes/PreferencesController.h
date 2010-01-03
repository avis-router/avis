#import <UIKit/UIKit.h>

@interface PreferencesController : 
  UIViewController <UITableViewDelegate, UITableViewDataSource>
{
  NSArray *prefsInfo;
  
  IBOutlet UITableViewCell *userNameCell;
  IBOutlet UITextField *userNameTextField;
}

- (IBAction) doneClicked: (id) sender;

@end
