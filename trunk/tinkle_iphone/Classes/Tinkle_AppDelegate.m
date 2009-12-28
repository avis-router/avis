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

