#import <UIKit/UIKit.h>

@interface PreferencesController : 
  UIViewController <UITableViewDelegate, UITableViewDataSource>
{
  NSArray *prefsInfo;
}

- (UITextField *) userNameField;

- (IBAction) doneClicked: (id) sender;

@end
