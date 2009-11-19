#import <Foundation/Foundation.h>

#import "PresenceStatus.h"

extern NSString *PresenceStatusChangedNotification;

@class ElvinConnection;
@class RHSystemIdleTimer;

@interface PresenceConnection : NSObject
{
  ElvinConnection    * elvin;
  NSMutableSet       * entities;
  PresenceStatus     * presenceStatus;
  NSArray            * groups;
  NSArray            * buddies;
  NSString           * userId;
  NSString           * userName;
  RHSystemIdleTimer  * idleTimer;
  id                   presenceInfoSubscription;
  id                   presenceRequestSubscription;
}

- (id) initWithElvin: (ElvinConnection *) theElvinConnection 
              userId: (NSString *) newUserId userName: (NSString *) newUserName
              groups: (NSArray *) newGroups buddies: (NSArray *) newBuddies;

- (void) refresh;

@property (readonly, assign)  IBOutlet NSMutableSet   * entities;
@property (readwrite, retain) IBOutlet PresenceStatus * presenceStatus;
@property (readwrite, retain) NSArray                 * groups;
@property (readwrite, retain) NSArray                 * buddies;
@property (readonly, retain)  NSString                * userId;
@property (readwrite, retain) NSString                * userName;

@end
