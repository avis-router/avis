#import "PreferencesController.h"

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
    [NSArray arrayWithObjects:
      [NSDictionary dictionaryWithObjectsAndKeys:
        @"User Name", @"Title",
        [NSArray arrayWithObjects:
          [NSDictionary dictionaryWithObjectsAndKeys:
            [self userNameField], @"View",
            @"UserName", @"CellID",
          nil],
        nil],
        @"Views",
      nil],
    nil];
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

- (UITextField *) userNameField
{
  CGRect frame = CGRectMake (10, 8.0, 260, 30);
  UITextField *textField = [[UITextField alloc] initWithFrame: frame];
  
  textField.borderStyle = UITextBorderStyleBezel;
  textField.textColor = [UIColor blackColor];
  textField.font = [UIFont systemFontOfSize:17.0];
  textField.placeholder = @"<enter text>";
  textField.backgroundColor = [UIColor whiteColor];
  textField.autocorrectionType = UITextAutocorrectionTypeNo;	// no auto correction support
  
  textField.keyboardType = UIKeyboardTypeDefault;	// use the default type input method (entire keyboard)
  textField.returnKeyType = UIReturnKeyDone;
  
  textField.clearButtonMode = UITextFieldViewModeWhileEditing;	// has a clear 'x' button to the right
  
//  textFieldNormal.tag = kViewTag;		// tag this control so we can remove it later for recycled cells
  
//  textFieldNormal.delegate = self;	// let us be the delegate so we know when the keyboard's "Done" button is pressed
  
  // Add an accessibility label that describes what the text field is for.
//  [textFieldNormal setAccessibilityLabel:NSLocalizedString(@"NormalTextField", @"")];

  return textField;
}

- (IBAction) doneClicked: (id) sender
{
  [self.parentViewController dismissModalViewControllerAnimated: YES];
}

#pragma mark -
#pragma mark UITableViewDataSource

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
	return 50;
}

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
  
  UITableViewCell *cell;
    [tableView dequeueReusableCellWithIdentifier: [info objectForKey: @"CellID"]];

  if (cell == nil)
  {
    cell =
      [[[UITableViewCell alloc] initWithStyle: UITableViewCellStyleDefault 
        reuseIdentifier: [info objectForKey: @"CellID"]] autorelease];
    
    cell.selectionStyle = UITableViewCellSelectionStyleNone;
    [cell.contentView addSubview: [info objectForKey: @"View"]];
  }
  
  return cell;
}

@end
