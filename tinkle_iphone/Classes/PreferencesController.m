#import "PreferencesController.h"
#import "Preferences.h"

#import "utils.h"

@implementation PreferencesController

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
          [NSDictionary dictionaryWithObjectsAndKeys:
            elvinUrlCell, @"Cell",
          nil],
        nil],
        @"Views",
      nil],
    nil] retain];
    
  userNameTextField.text = prefString (PrefOnlineUserName);
  elvinUrlTextField.text = prefString (PrefElvinURL);
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
  NSString *newElvinUrl = trim (elvinUrlTextField.text);
  
  BOOL presenceChanged = ![prefString (PrefOnlineUserName) isEqual: newUserName];
  BOOL elvinChanged = ![prefString (PrefElvinURL) isEqual: newElvinUrl];
  
  if (presenceChanged)
  {
    [[NSNotificationCenter defaultCenter] 
      postNotificationName: PresenceDefaultsWillChangeNotification object: self];
      
    setPref (PrefOnlineUserName, newUserName);
  
    [[NSNotificationCenter defaultCenter] 
      postNotificationName: PresenceDefaultsDidChangeNotification object: self];
  }
  
  if (elvinChanged)
  {
    [[NSNotificationCenter defaultCenter] 
      postNotificationName: ElvinDefaultsWillChangeNotification object: self];
      
    setPref (PrefElvinURL, newElvinUrl);
  
    [[NSNotificationCenter defaultCenter] 
      postNotificationName: ElvinDefaultsDidChangeNotification object: self];
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
