#import "MainWindowController.h"

#import "PresenceTableViewController.h"
#import "MessagesViewController.h"

@implementation MainWindowController

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
  
  messagesController.view.hidden = YES;
  
  presenceController.view.frame = contentView.frame;
  messagesController.view.frame = contentView.frame;
  
  [contentView addSubview: presenceController.view];
  [contentView addSubview: messagesController.view];
}

- (IBAction) panelSelectorItemChanged: (id) sender
{
  switch (panelSelector.selectedSegmentIndex)
  {
    case 0:
      presenceController.view.hidden = NO;
      messagesController.view.hidden = YES;
      break;
   case 1:
      presenceController.view.hidden = YES;
      messagesController.view.hidden = NO;
      break;
  }
}

- (BOOL) shouldAutorotateToInterfaceOrientation: 
         (UIInterfaceOrientation) interfaceOrientation 
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

@end
