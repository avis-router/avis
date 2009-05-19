#import <Cocoa/Cocoa.h>

#import "PresenceStatus.h"

@class ElvinConnection;
@class RHSystemIdleTimer;

@interface PresenceConnection : NSObject
{
  ElvinConnection * elvin;
  NSMutableSet *    entities;
  PresenceStatus *  presenceStatus;
  RHSystemIdleTimer *idleTimer;
}

- (id) initWithElvin: (ElvinConnection *) theElvinConnection;

- (void) refresh;

@property (readonly, assign) IBOutlet  NSMutableSet * entities;
@property (readwrite, retain) IBOutlet PresenceStatus * presenceStatus;

@end
