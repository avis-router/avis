#import "MainWindowController.h"

#import "PresenceViewController.h"
#import "MessagesViewController.h"
#import "PreferencesController.h"
#import "Preferences.h"
#import "PresenceEntity.h"

@implementation MainWindowController

- (void) viewDidLoad
{
  [super viewDidLoad];
  
  messagesController.parentViewController = self;

  messagesController.view.hidden = YES;
  
  presenceController.view.frame = contentView.frame;
  messagesController.view.frame = contentView.frame;
  
  messagesController.parentViewController = self;
  
  [contentView addSubview: presenceController.view];
  [contentView addSubview: messagesController.view];
  
//  scrollView = [[TTScrollView alloc] initWithFrame: contentView.frame];
//  scrollView.dataSource = self;
//  scrollView.backgroundColor = [UIColor whiteColor];
//  
//  [self.view addSubview: scrollView];
  
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];
  
  [notifications addObserver: self selector: @selector (handlePresenceUserClicked:)
                        name: PresenceEntityClickedNotification object: nil]; 
}

- (void) handlePresenceUserClicked: (NSNotification *) ntfn
{
  PresenceEntity *entity = [ntfn object];
  
  setPref (PrefDefaultSendGroup, entity.name);
  
  panelSelector.selectedSegmentIndex = 1;
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

//  [scrollView moveToPageAtIndex: panelSelector.selectedSegmentIndex resetEdges: NO];
//  
//  scrollView.centerPageIndex = panelSelector.selectedSegmentIndex;
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

#pragma mark -
#pragma mark TTScrollViewDataSource

- (NSInteger) numberOfPagesInScrollView: (TTScrollView*) scrollView
{
  return 2;
}

- (UIView *) scrollView: (TTScrollView *) scrollView 
             pageAtIndex: (NSInteger) pageIndex
{
  presenceController.view.userInteractionEnabled = NO;
  messagesController.view.userInteractionEnabled = NO;
  
  switch (pageIndex)
  {
    case 0:
      return presenceController.view;
    case 1:
      return messagesController.view;
    default:
      return nil;
  }
}

- (CGSize) scrollView: (TTScrollView *) scrollView 
  sizeOfPageAtIndex: (NSInteger) pageIndex
{
  return contentView.frame.size;
}

@end
