#import <UIKit/UIKit.h>

extern NSString *PresenceEntityClickedNotification;

@class PresenceConnection;

@interface PresenceViewController : UITableViewController
{
  IBOutlet PresenceConnection *presence;
}

@property (readwrite, retain) PresenceConnection * presence;

@end
