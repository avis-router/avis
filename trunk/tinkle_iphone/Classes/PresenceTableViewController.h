#import <UIKit/UIKit.h>

extern NSString *PresenceEntityClickedNotification;

@class PresenceConnection;

@interface PresenceTableViewController : UITableViewController
{
  IBOutlet PresenceConnection *presence;
}

@property (readwrite, retain) PresenceConnection * presence;

@end
