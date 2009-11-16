#import <UIKit/UIKit.h>

@class ElvinConnection;
@class PresenceConnection;
@class PresenceTableViewController;

@interface Tinkle_AppDelegate : 
  NSObject <UIApplicationDelegate, UITabBarControllerDelegate> 
{
  UIWindow *window;
  UITabBarController *tabBarController;
  
  IBOutlet PresenceTableViewController *presenceController;
  
  ElvinConnection *elvin;
  PresenceConnection *presence;
}

@property (nonatomic, retain) IBOutlet UIWindow *window;
@property (nonatomic, retain) IBOutlet UITabBarController *tabBarController;
@property (nonatomic, readonly) PresenceConnection *presence;

- (void) disconnect;

@end
