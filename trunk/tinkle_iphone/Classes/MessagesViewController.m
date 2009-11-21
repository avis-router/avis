#import "MessagesViewController.h"

@implementation MessagesViewController

/*
// The designated initializer. Override to perform setup that is required before the view is loaded.
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
  
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];
  
  [notifications addObserver: self selector: @selector (handleKeyboardShowOrHide:)
                        name: UIKeyboardWillShowNotification object: nil]; 
  [notifications addObserver: self selector: @selector (handleKeyboardShowOrHide:)
                        name: UIKeyboardWillHideNotification object: nil]; 
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


- (void)dealloc {
    [super dealloc];
}

- (void) handleKeyboardShowOrHide: (NSNotification *) ntfn
{
  CGPoint centreStart;
  CGPoint centreEnd;
  
  [[[ntfn userInfo] valueForKey: UIKeyboardCenterBeginUserInfoKey] 
    getValue: &centreStart];
  [[[ntfn userInfo] valueForKey: UIKeyboardCenterEndUserInfoKey] 
    getValue: &centreEnd];
  
  CGRect windowFrame = self.view.window.frame;
  CGRect myBounds = [self.view convertRect: self.view.bounds toView: nil];
  
  // adjustment for my frame's vertical offset from bottom
  CGFloat vertOffset = 
    (windowFrame.origin.y + windowFrame.size.height) - 
    (myBounds.origin.y + myBounds.size.height);
                 
  // insert offset depending on movement direction
  if (centreStart.y < centreEnd.y)
    vertOffset = -vertOffset;

  // extract keyboard's animation params
  double duration;
  UIViewAnimationCurve curve;
  
  [[[ntfn userInfo] valueForKey: UIKeyboardAnimationDurationUserInfoKey]
    getValue: &duration];
  [[[ntfn userInfo] valueForKey: UIKeyboardAnimationCurveUserInfoKey] 
    getValue: &curve];
  
  [UIView beginAnimations: @"showKeyboard" context: nil];
  [UIView setAnimationDuration: duration];
  [UIView setAnimationCurve: curve];
    
  self.view.bounds = 
    CGRectOffset (self.view.bounds, centreStart.x - centreEnd.x, 
                  (centreStart.y - centreEnd.y) - vertOffset);

  [UIView commitAnimations];
}

- (void) handleKeyboardHide: (NSNotification *) ntfn
{
  NSLog (@"Hide keyboard");
}

@end
