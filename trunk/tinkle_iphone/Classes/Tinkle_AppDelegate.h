#import <UIKit/UIKit.h>

@class ElvinConnection;
@class PresenceConnection;
@class PresenceTableViewController;
@class MessagesViewController;
@class MainWindowController;

@interface Tinkle_AppDelegate : NSObject <UIApplicationDelegate> 
{
  UIWindow *window;
  MainWindowController *mainWindowController;
  
  IBOutlet PresenceTableViewController *presenceController;
  IBOutlet MessagesViewController      *messagesController;
  
  ElvinConnection    *elvin;
  PresenceConnection *presence;
}

@property (nonatomic, retain) IBOutlet UIWindow *window;
@property (nonatomic, retain) IBOutlet MainWindowController *mainWindowController;
@property (nonatomic, readonly) PresenceConnection *presence;

- (void) disconnect;

@end
