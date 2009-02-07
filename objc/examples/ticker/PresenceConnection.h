#import <Cocoa/Cocoa.h>

#import "PresenceStatus.h"

@class ElvinConnection;

@interface PresenceConnection : NSObject
{
  ElvinConnection * elvin;
  NSMutableSet *    entities;
  PresenceStatus *  presenceStatus;
}

- (id) initWithElvin: (ElvinConnection *) theElvinConnection;

- (void) refresh;

@property (readonly, assign) IBOutlet  NSSet * entities;
@property (readwrite, retain) IBOutlet PresenceStatus * presenceStatus;

@end
