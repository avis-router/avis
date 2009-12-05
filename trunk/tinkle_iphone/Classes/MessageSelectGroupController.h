#import <UIKit/UIKit.h>

@interface MessageSelectGroupController : 
  UIViewController <UIPickerViewDelegate, UIPickerViewDataSource>
{
  NSArray *groups;
  NSString *group;
  id delegate;
  
  IBOutlet UIPickerView *picker;
}

@property (readwrite, retain) NSArray *groups;
@property (readwrite, retain) NSString *group;
@property (readwrite, assign) id delegate;

- (IBAction) cancel: (id) sender;

- (IBAction) groupSelected: (id) sender;

@end
