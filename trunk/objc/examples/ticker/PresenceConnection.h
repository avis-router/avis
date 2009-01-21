#import <Foundation/Foundation.h>

@class ElvinConnection;

@interface PresenceConnection : NSObject
{
  ElvinConnection * elvin;
  NSMutableSet *    entities;
}

- (id) initWithElvin: (ElvinConnection *) theElvinConnection;

@property (readonly, assign) IBOutlet NSSet * entities;

@end
