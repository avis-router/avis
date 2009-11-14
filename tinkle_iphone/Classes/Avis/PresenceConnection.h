#import <Foundation/Foundation.h>

#import "PresenceStatus.h"

extern NSString *PresenceStatusChangedNotification;

@class ElvinConnection;

@interface PresenceConnection : NSObject
{
  ElvinConnection    * elvin;
  NSMutableSet       * entities;
  PresenceStatus     * presenceStatus;
}

- (id) initWithElvin: (ElvinConnection *) theElvinConnection;

- (void) refresh;

@property (readonly, assign)  IBOutlet NSMutableSet   * entities;
@property (readwrite, retain) IBOutlet PresenceStatus * presenceStatus;

@end
