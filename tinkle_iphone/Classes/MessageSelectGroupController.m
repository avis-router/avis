#import "MessageSelectGroupController.h"

#import "utils.h"

@implementation MessageSelectGroupController

@synthesize group, groups, delegate;

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
  
//  picker.autoresizingMask = UIViewAutoresizingFlexibleWidth;
//  picker.showsSelectionIndicator = YES;
}

- (void) viewWillAppear: (BOOL) animated
{
  [super viewWillAppear: animated];
  
  [groupsList selectRowAtIndexPath: 
    [NSIndexPath indexPathWithIndex: [groups indexOfObject: group]] 
    animated: NO scrollPosition: UITableViewScrollPositionNone];
}

- (BOOL) shouldAutorotateToInterfaceOrientation: (UIInterfaceOrientation) interfaceOrientation
{
  return YES;
}

- (void) didReceiveMemoryWarning
{
	// Releases the view if it doesn't have a superview.
  [super didReceiveMemoryWarning];
	
	// Release any cached data, images, etc that aren't in use.
}

- (void) viewDidUnload 
{
	// Release any retained subviews of the main view.
	// e.g. self.myOutlet = nil;
}

- (void) dealloc 
{
  [super dealloc];
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
  [self.delegate setGroup: groupName];

  [groupTextField resignFirstResponder];

  [self.parentViewController dismissModalViewControllerAnimated: YES];
}

- (IBAction) addButtonClicked: (id) sender
{
  NSString *addedGroup = trim (groupTextField.text);
  
  if ([addedGroup length] > 0 && ![groups containsObject: addedGroup])
    [delegate groupsChanged: [groups arrayByAddingObject: addedGroup]];
    
  [self selectGroup: addedGroup];
}

- (void) setGroup: (NSString *) newGroup
{
  [group release];
  group = [newGroup retain];
  
  [groupsList selectRowAtIndexPath: 
    [NSIndexPath indexPathWithIndex: [groups indexOfObject: group]] 
    animated: NO scrollPosition: UITableViewScrollPositionNone];
}

- (void) setGroups: (NSArray *) newGroups
{
  [groups release];
  groups = 
    [[newGroups sortedArrayUsingSelector: 
      @selector (localizedCaseInsensitiveCompare:)] retain];
}

#pragma mark -
#pragma mark UITableViewDelegate

- (void) tableView: (UITableView *) tableView 
         didSelectRowAtIndexPath: (NSIndexPath *) indexPath
{ 
  [self selectGroup: [groups objectAtIndex: indexPath.row]];
}

#pragma mark -
#pragma mark UITableViewDataSource

- (NSInteger) numberOfSectionsInTableView: (UITableView *) tableView
{
  return 1;
}

// Customize the number of rows in the table view.
- (NSInteger) tableView: (UITableView *) tableView 
  numberOfRowsInSection: (NSInteger) section 
{
  return [groups count];
}

// Customize the appearance of table view cells.
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
