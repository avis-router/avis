#import "MainWindowController.h"

#import "PresenceTableViewController.h"
#import "MessagesViewController.h"
#import "PreferencesController.h"

@implementation MainWindowController

- (void) viewDidLoad
{
  [super viewDidLoad];
  
  messagesController.view.hidden = YES;
  
  presenceController.view.frame = contentView.frame;
  messagesController.view.frame = contentView.frame;
  
  messagesController.parentViewController = self;
  
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

- (IBAction) showPreferences: (id) sender
{
  PreferencesController *prefsController = 
     [[PreferencesController alloc]
       initWithNibName: @"Preferences" bundle: nil];
  
   prefsController.modalTransitionStyle = UIModalTransitionStyleFlipHorizontal;

   UINavigationController *navigationController = 
     [[UINavigationController alloc] initWithRootViewController: prefsController];
    
  navigationController.toolbarHidden = YES;
  navigationController.navigationBarHidden = YES;
  
  [self presentModalViewController: navigationController animated: YES];
 
  [navigationController release];
  [prefsController release]; 
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
