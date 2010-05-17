#import "MessageSelectGroupController.h"

#import "Preferences.h"
#import "utils.h"

@implementation MessageSelectGroupController

- (void) viewWillAppear: (BOOL) animated
{
  [super viewWillAppear: animated];
  
  groups = 
    [[NSMutableArray arrayWithArray: 
      [prefArray (PrefTickerGroups) sortedArrayUsingSelector: 
        @selector (localizedCaseInsensitiveCompare:)]] retain];
}

- (BOOL) shouldAutorotateToInterfaceOrientation: 
         (UIInterfaceOrientation) interfaceOrientation
{
  return YES;
}

#pragma mark -
#pragma mark Event handlers

- (IBAction) cancel: (id) sender
{
  [groupTextField resignFirstResponder];
  [self.parentViewController dismissModalViewControllerAnimated: YES];
}

- (void) selectGroup: (NSString *) groupName
{
  setPref (PrefDefaultSendGroup, groupName);

  [groupTextField resignFirstResponder];

  [self.parentViewController dismissModalViewControllerAnimated: YES];
}

- (IBAction) addButtonClicked: (id) sender
{
  NSString *addedGroup = trim (groupTextField.text);
  
  if ([addedGroup length] == 0)
    return;
    
  if (![groups containsObject: addedGroup])
    setPref (PrefTickerGroups, [groups arrayByAddingObject: addedGroup]);
  
  [self selectGroup: addedGroup];
}

#pragma mark -
#pragma mark UITableViewDelegate

- (void) tableView: (UITableView *) tableView 
         didSelectRowAtIndexPath: (NSIndexPath *) indexPath
{ 
  [self selectGroup: [groups objectAtIndex: indexPath.row]];
}

- (void) tableView: (UITableView *) tableView 
         commitEditingStyle: (UITableViewCellEditingStyle) editingStyle 
         forRowAtIndexPath: (NSIndexPath *) indexPath
{
  [groups removeObjectAtIndex: indexPath.row];
  
  [tableView deleteRowsAtIndexPaths: 
    [NSArray arrayWithObject: indexPath] withRowAnimation: YES];
    
  setPref (PrefTickerGroups, [NSArray arrayWithArray: groups]);
}

#pragma mark -
#pragma mark UITableViewDataSource

- (NSInteger) numberOfSectionsInTableView: (UITableView *) tableView
{
  return 1;
}

- (NSInteger) tableView: (UITableView *) tableView 
  numberOfRowsInSection: (NSInteger) section 
{
  return [groups count];
}

- (UITableViewCell *) tableView: (UITableView *) tableView
    cellForRowAtIndexPath: (NSIndexPath *) indexPath 
{
  static NSString *CellIdentifier = @"GroupCell";
    
  UITableViewCell *cell = 
    [tableView dequeueReusableCellWithIdentifier: CellIdentifier];

  if (cell == nil)
  {
    cell = [[[UITableViewCell alloc]
             initWithStyle: UITableViewCellStyleDefault
               reuseIdentifier: CellIdentifier] autorelease];
  } 
  
  cell.textLabel.text = [groups objectAtIndex: indexPath.row];

  return cell;
}

@end