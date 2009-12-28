#import "MessageSelectGroupController.h"

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
  
//  [picker selectRow: [groups indexOfObject: group] inComponent: 0 animated: NO];

  [groupsList selectRowAtIndexPath: 
    [NSIndexPath indexPathWithIndex: [groups indexOfObject: group]] 
    animated: NO scrollPosition: UITableViewScrollPositionNone];
}

/*
// Override to allow orientations other than the default portrait orientation.
- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    // Return YES for supported orientations
    return (interfaceOrientation == UIInterfaceOrientationPortrait);
}
*/

- (void)didReceiveMemoryWarning {
	// Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
	
	// Release any cached data, images, etc that aren't in use.
}

- (void)viewDidUnload {
	// Release any retained subviews of the main view.
	// e.g. self.myOutlet = nil;
}


- (void)dealloc 
{
  [super dealloc];
}

- (IBAction) cancel: (id) sender
{
  [groupTextField resignFirstResponder];
  [self.parentViewController dismissModalViewControllerAnimated: YES];
}

- (IBAction) groupSelected: (id) sender
{  
  [self.delegate setGroup:
    [groups objectAtIndex: [groupsList indexPathForSelectedRow].row]];

  [groupTextField resignFirstResponder];

  [self.parentViewController dismissModalViewControllerAnimated: YES];
}

- (IBAction) addButtonClicked: (id) sender
{
}

#pragma mark -
#pragma mark UITableViewDelegate

//- (void) tableView: (UITableView *) tableView 
//         didSelectRowAtIndexPath: (NSIndexPath *) indexPath
//{
//  
//}

//- (void) pickerView: (UIPickerView *) pickerView didSelectRow: (NSInteger) row 
//         inComponent: (NSInteger) component
//{
////	if (pickerView == myPickerView)	// don't show selection for the custom picker
////	{
////		// report the selection to the UI label
////		label.text = [NSString stringWithFormat:@"%@ - %d",
////						[pickerViewArray objectAtIndex:[pickerView selectedRowInComponent:0]],
////						[pickerView selectedRowInComponent:1]];
////	}
//}

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

//#pragma mark -
//#pragma mark UIPickerViewDataSource
//
//- (NSString *) pickerView: (UIPickerView *) pickerView 
//               titleForRow: (NSInteger) row forComponent: (NSInteger) component
//{
//  return [groups objectAtIndex: row];
//}
//
//- (CGFloat) pickerView: (UIPickerView *) pickerView 
//            widthForComponent: (NSInteger) component
//{
//	return 240.0;
//}
//
//- (CGFloat) pickerView: (UIPickerView *) pickerView 
//            rowHeightForComponent: (NSInteger) component
//{
//	return 40.0;
//}
//
//- (NSInteger) pickerView: (UIPickerView *) pickerView 
//              numberOfRowsInComponent: (NSInteger) component
//{
//	return [groups count];
//}
//
//- (NSInteger) numberOfComponentsInPickerView: (UIPickerView *) pickerView
//{
//	return 1;
//}

@end
