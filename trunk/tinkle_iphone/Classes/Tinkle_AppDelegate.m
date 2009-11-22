#import "Tinkle_AppDelegate.h"

#import "ElvinConnection.h"
#import "PresenceConnection.h"
#import "PresenceTableViewController.h"
#import "MessagesViewController.h"
#import "Preferences.h"

@implementation Tinkle_AppDelegate

@synthesize window;
@synthesize tabBarController;
@synthesize presence;

- (id) init
{
  self = [super init];

  if (!self)
    return nil;

  registerUserDefaults ();
  
//  elvin = 
//    [[ElvinConnection alloc] initWithUrl: @"elvin://pearl.local:29170"];
  elvin = 
    [[ElvinConnection alloc] initWithUrl: @"elvin://public.elvin.org"];
    
  presence = [[[PresenceConnection alloc] initWithElvin: elvin] retain];
    
  NSString *userAgent = 
    [NSString stringWithFormat: @"%@ for iPhone (%@)",
      [[NSBundle mainBundle] objectForInfoDictionaryKey: @"CFBundleDisplayName"],
      [[NSBundle mainBundle] objectForInfoDictionaryKey: @"CFBundleShortVersionString"]];

  elvin.userAgent = userAgent;
  
  return self;
}

- (void) dealloc 
{
  [tabBarController release];
  [window release];
  
  [self disconnect];
  
  [presence release];
  [elvin release];
  
  [super dealloc];
}

- (void) applicationDidFinishLaunching: (UIApplication *) application 
{
  // Add the tab bar controller's current view as a subview of the window
  [window addSubview: tabBarController.view];

  presenceController.presence = presence;
  messagesController.elvin = elvin;
  messagesController.subscription = [self tickerSubscription];
  
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];
  
  [notifications addObserver: self selector: @selector (handleElvinOpen:)
                        name: ElvinConnectionOpenedNotification object: nil]; 
  
  [notifications addObserver: self selector: @selector (handleElvinClose:)
                        name: ElvinConnectionClosedNotification object: nil]; 

  [elvin connect];
}

- (void) applicationWillTerminate: (UIApplication *) application
{
  [self disconnect];
}

- (void) disconnect
{
  [elvin disconnect];
}

- (void) handleElvinOpen: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    // TODO
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
    // TODO
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinClose:) 
                           withObject: nil waitUntilDone: NO];
  }
}

- (NSString *) tickerSubscription
{
  NSArray *groups = prefArray (PrefTickerGroups);
  NSString *subscription = prefString (PrefTickerSubscription);
  NSString *user = 
    [ElvinConnection escapedSubscriptionString: prefString (PrefOnlineUserName)];
  
  NSMutableString *fullSubscription = 
    [NSMutableString stringWithString: 
       @"(string (Message) && string (From) && string (Group))"];
  
  // user's messages
  
  [fullSubscription appendFormat: 
     @" && ((From == '%@' || Group == '%@' || Thread-Id == '%@')",
     user, user, user];
  
  // groups
  
  if ([groups count] > 0 || [subscription length] > 0)
    [fullSubscription appendString: @" || ("];
  
  if ([groups count] > 0)
  {
    [fullSubscription appendString: @"equals (Group"];
    
    for (NSString *group in groups)
      [fullSubscription appendFormat: @", '%@'",
         [ElvinConnection escapedSubscriptionString: group]];
    
    [fullSubscription appendString: @")"];
  }
  
  // extra subscription
  
  if ([subscription length] > 0)
  {
    if ([groups count] > 0)
      [fullSubscription appendString: @" || "];
  
    [fullSubscription appendFormat: @"(%@)", subscription];
  }
  
  if ([groups count] > 0 || [subscription length] > 0)
    [fullSubscription appendString: @")"];
  
  [fullSubscription appendString: @")"];
  
  return fullSubscription;
}

/*
// Optional UITabBarControllerDelegate method
- (void)tabBarController:(UITabBarController *)tabBarController didSelectViewController:(UIViewController *)viewController {
}
*/

/*
// Optional UITabBarControllerDelegate method
- (void)tabBarController:(UITabBarController *)tabBarController didEndCustomizingViewControllers:(NSArray *)viewControllers changed:(BOOL)changed {
}
*/

@end

