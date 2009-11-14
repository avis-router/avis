#import "Tinkle_AppDelegate.h"

#import "ElvinConnection.h"

@implementation Tinkle_AppDelegate

@synthesize window;
@synthesize tabBarController;


- (void) applicationDidFinishLaunching: (UIApplication *) application 
{
  // Add the tab bar controller's current view as a subview of the window
  [window addSubview: tabBarController.view];
  
  // connect to elvin
  NSLog (@"Connect to elvin");
  
  ElvinConnection *elvin = 
    [[ElvinConnection alloc] initWithUrl: @"elvin://pearl.local:29170"];
    
  [elvin connect];
  
  sleep (2);
  
  if ([elvin isConnected])
    NSLog (@"Connected!");
  else   
    NSLog (@"Failed :(");
    
  [elvin disconnect];
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


- (void)dealloc 
{
  [tabBarController release];
  [window release];
  [super dealloc];
}

@end

