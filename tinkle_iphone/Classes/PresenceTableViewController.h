#import <UIKit/UIKit.h>

@class PresenceConnection;

@interface PresenceTableViewController : UITableViewController
{
  IBOutlet PresenceConnection *presence;
}

@property (readwrite, retain) PresenceConnection * presence;

@end
