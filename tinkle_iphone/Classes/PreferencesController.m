#import "PreferencesController.h"
#import "Preferences.h"

#import "utils.h"

@implementation PreferencesController

/*
 // The designated initializer.  Override if you create the controller programmatically and want to perform customization that is not appropriate for viewDidLoad.
- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    if (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) {
        // Custom initialization
    }
    return self;
}
*/

/*
// Implement loadView to create a view hierarchy programmatically, without using a nib.
- (void)loadView {
}
*/

- (void) viewDidLoad
{
  [super viewDidLoad];
  
  prefsInfo = 
    [[NSArray arrayWithObjects:
      [NSDictionary dictionaryWithObjectsAndKeys:
        @"Account", @"Title",
        [NSArray arrayWithObjects:
          [NSDictionary dictionaryWithObjectsAndKeys:
            userNameCell, @"Cell",
          nil],
        nil],
        @"Views",
      nil],
    nil] retain];
    
  userNameTextField.text = prefString (PrefOnlineUserName);
}

- (BOOL) shouldAutorotateToInterfaceOrientation: 
         (UIInterfaceOrientation)interfaceOrientation
{
  return YES;
}

- (void) viewDidUnload
{
  // TODO
}

- (void) dealloc
{
  [super dealloc];
  // TODO
}

- (IBAction) doneClicked: (id) sender
{
  [userNameTextField resignFirstResponder];
  
  NSString *newUserName = trim (userNameTextField.text);
  
  BOOL presenceChanged = ![prefString (PrefOnlineUserName) isEqual: newUserName];
  
  if (presenceChanged)
  {
    [[NSNotificationCenter defaultCenter] 
      postNotificationName: PresenceDefaultsWillChangeNotification object: self];
      
    setPref (PrefOnlineUserName, userNameTextField.text);
  
    [[NSNotificationCenter defaultCenter] 
      postNotificationName: PresenceDefaultsChangedNotification object: self];
  }

  [self.parentViewController dismissModalViewControllerAnimated: YES];
}

#pragma mark -
#pragma mark UITableViewDataSource

- (NSInteger) numberOfSectionsInTableView: (UITableView *) tableView
{
  return [prefsInfo count];
}

- (NSInteger) tableView: (UITableView *) tableView 
              numberOfRowsInSection: (NSInteger) section 
{
  return [[[prefsInfo objectAtIndex: section] objectForKey: @"Views"] count];
}

- (NSString *) tableView: (UITableView *) tableView 
               titleForHeaderInSection: (NSInteger) section
{
	return [[prefsInfo objectAtIndex: section] objectForKey: @"Title"];
}

- (UITableViewCell *) tableView: (UITableView *) tableView
                      cellForRowAtIndexPath: (NSIndexPath *) indexPath 
{
  NSDictionary *section = [prefsInfo objectAtIndex: indexPath.section];
  
  NSDictionary *info = 
    [[section objectForKey: @"Views"] objectAtIndex: indexPath.row];
  
  return [info valueForKey: @"Cell"];
}

@end
