#import "MessagesViewController.h"

#import "Preferences.h"
#import "ElvinConnection.h"

NSString *TickerMessageReceivedNotification = @"TickerMessageReceived";

static inline float bottomY (CGRect rect) 
{
  return rect.origin.y + rect.size.height;
}

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

@synthesize elvin;
@synthesize canSend;

- (void) viewDidLoad
{
  [super viewDidLoad];
  
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];
  
  [notifications addObserver: self selector: @selector (handleKeyboardShowOrHide:)
                        name: UIKeyboardWillShowNotification object: nil]; 
  [notifications addObserver: self selector: @selector (handleKeyboardShowOrHide:)
                        name: UIKeyboardWillHideNotification object: nil]; 
  
  [notifications addObserver: self selector: @selector (handleElvinOpen:)
                        name: ElvinConnectionOpenedNotification object: nil]; 
  
  [notifications addObserver: self selector: @selector (handleElvinClose:)
                        name: ElvinConnectionClosedNotification object: nil]; 
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

- (NSString *) subscription
{
  return subscription;
}

- (void) setSubscription: (NSString *) newSubscription
{
  if (![newSubscription isEqual: subscription])
  {
    [subscription release];
    
    subscription = [newSubscription retain];
    
    if (subscriptionContext)
    {
      [elvin resubscribe: subscriptionContext 
        usingSubscription: subscription];
    } else
    {
      subscriptionContext = 
        [elvin subscribe: subscription withDelegate: self 
          onNotify: @selector (handleNotify:) 
           onError: @selector (handleSubscribeError:)];
    }
  }
}

- (IBAction) sendMessage: (id) sender
{
  NSString *message = messageCompositionField.text;

  // TODO
  NSString *group = @"Test";
//  NSString *group = [messageGroup stringValue];
  
  [elvin sendTickerMessage: message 
    fromSender: prefString (PrefOnlineUserName)
    toGroup: group 
    inReplyTo: nil
    attachedURL: nil
    sendPublic: YES
    sendInsecure: YES];

//  self.attachedURL = nil;
//  self.inReplyTo = nil;
//  self.allowPublic = NO;
//  self.allowInsecure = YES;
  
  messageCompositionField.text = @"";
  [messageCompositionField resignFirstResponder];

//  // flip focus from/to message text to fire start/end editing events
//  if ([[messageText window] firstResponder] == messageText)
//  {
//    [[messageGroup window] makeFirstResponder: messageGroup];
//    [[messageText window] makeFirstResponder: messageText];
//  }
//  
//  // add group to groups pref if not there
//  NSArray *groups = prefArray (PrefTickerGroups);
//  
//  if ([group rangeOfString: @"@"].location == NSNotFound &&
//      ![groups containsObject: group])
//  {
//    NSMutableArray *newGroups = [NSMutableArray arrayWithArray: groups];
//    
//    [newGroups addObject: group];
//    
//    [[NSUserDefaults standardUserDefaults] 
//       setObject: newGroups forKey: PrefTickerGroups];
//  }
}

- (void) handleNotify: (NSDictionary *) ntfn
{
  TickerMessage *message = [TickerMessage messageForNotification: ntfn];

//  // decide on whether we're scrolled to the end of the messages
//  CGPoint containerOrigin = [messagesTextView textContainerOrigin];
//  CGRect visibleRect = CGOffsetRect ([messagesTextView visibleRect], 
//                                     -containerOrigin.x, -containerOrigin.y);
//  CGRect tickerMessagesRect = [messagesTextView bounds];
//  
//  BOOL wasScrolledToEnd = 
//    bottomY (visibleRect) == bottomY (tickerMessagesRect);

//  NSDateFormatter *dateFormatter = [[NSDateFormatter new] autorelease];
//  [dateFormatter setDateStyle: NSDateFormatterLongStyle];
//  [dateFormatter setTimeStyle: NSDateFormatterMediumStyle];  
//
//  // define display attributes
//  NSDictionary *replyLinkAttrs = 
//    [NSDictionary dictionaryWithObject: message forKey: NSLinkAttributeName];
//     
//  NSDictionary *lowlightAttrs = 
//    [NSDictionary dictionaryWithObject: color (102, 102, 102) 
//      forKey: NSForegroundColorAttributeName];
//      
//  NSDictionary *groupAttrs = 
//    [NSDictionary dictionaryWithObject: color (48, 80, 10) 
//      forKey: NSForegroundColorAttributeName];
//
//  NSDictionary *fromAttrs = 
//    [NSDictionary dictionaryWithObject: color (86, 56, 12) 
//      forKey: NSForegroundColorAttributeName];
//
//  NSDictionary *messageAttrs = 
//    [NSDictionary dictionaryWithObject: color (0, 64, 128) 
//      forKey: NSForegroundColorAttributeName];
//
//  NSDictionary *fontAttrs = 
//    [NSDictionary dictionaryWithObject: 
//       [NSFont fontWithName: @"Lucida Grande" size: 11] 
//                     forKey: NSFontAttributeName];
  
  NSMutableString *messagesViewText = 
    [NSMutableString stringWithString: messagesTextView.text];
  NSRange range;
  
  range.location = messagesViewText.length;
  range.length = 0;
  
  // start new line for all but first message
  if (messagesViewText.length > 0)
    [messagesViewText appendString: @"\n"];
   
  [messagesViewText appendString: message->group];
  
  [messagesViewText appendString: @": "];
  
  [messagesViewText appendString: message->from];
  
  [messagesViewText appendString: @": "];
  
  [messagesViewText appendString: message->message];
  
//  if (message->url)
//  {    
//    NSDictionary *linkAttrs = 
//      [NSDictionary dictionaryWithObjectsAndKeys:
//       [NSColor blueColor], NSForegroundColorAttributeName, 
//       [NSNumber numberWithBool: NO], NSUnderlineStyleAttributeName,
//       message->url, NSLinkAttributeName, nil];
//
//    [displayedMessage 
//      appendAttributedString: attributedString (@" <", lowlightAttrs)];
//
//    if ([[message->url absoluteString] length] <= 70)
//    {
//      [displayedMessage appendAttributedString: 
//        attributedString ([message->url absoluteString], linkAttrs)];
//    } else
//    {
//      // display truncated URL
//      [displayedMessage appendAttributedString: 
//        attributedString ([message->url scheme], linkAttrs)];
//
//      [displayedMessage appendAttributedString: 
//        attributedString (@"://", linkAttrs)];
//                  
//      [displayedMessage appendAttributedString: 
//        attributedString ([message->url host], linkAttrs)];
//
//      [displayedMessage appendAttributedString: 
//        attributedString (@"/...", linkAttrs)];
//    }           
//    
//    [displayedMessage 
//      appendAttributedString: attributedString (@">", lowlightAttrs)];
//  }
  
//  // date  
//  [displayedMessage appendAttributedString: 
//    attributedString (@" (", lowlightAttrs)];
//
//  [displayedMessage appendAttributedString: 
//    attributedString ([dateFormatter stringFromDate: message->receivedAt], 
//                      lowlightAttrs)];
//  [displayedMessage appendAttributedString: 
//    attributedString (@")", lowlightAttrs)];

  messagesTextView.text = messagesViewText;
  
//  // scroll to end if that's how it was when we started
//  if (wasScrolledToEnd)
//  {
//    // TODO: bug: very rarely, this segfaults. perhaps something to do with
//    // insertion point?
//    [tickerMessagesTextView displayIfNeeded];
//    [tickerMessagesTextView scrollRangeToVisible: 
//      NSMakeRange ([[tickerMessagesTextView textStorage] length], 0)];
//  }
  
  [[NSNotificationCenter defaultCenter] 
     postNotificationName: TickerMessageReceivedNotification object: self
     userInfo: [NSDictionary dictionaryWithObject: message forKey: @"message"]];
}

- (void) handleElvinOpen: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    self.canSend = YES;
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinOpen:) 
                           withObject: nil waitUntilDone: NO];
  }
}

- (void) handleElvinClose: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    self.canSend = NO;
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinClose:) 
                           withObject: nil waitUntilDone: NO];
  }
}

- (void) handleSubscribeError: (NSError *) error
{
  // TODO
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
                 
  // invert offset depending on movement direction
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

@end
