#import <Foundation/Foundation.h>

#import "PresenceStatus.h"

@class ElvinConnection;

@interface PresenceConnection : NSObject
{
  ElvinConnection * elvin;
  NSMutableSet *    entities;
  PresenceStatus *  presenceStatus;
}

- (id) initWithElvin: (ElvinConnection *) theElvinConnection;

@property (readonly, assign) IBOutlet NSSet * entities;

@end
