#import <Cocoa/Cocoa.h>

#import "PresenceStatus.h"

@interface PresenceEntity : NSObject
{
  NSString *       presenceId;
  NSString *       name;
  PresenceStatus * status;
  NSDate *         lastUpdatedAt;
}

+ entityWithName: (NSString *) name;

- (id) initWithId: (NSString *) newId;

@property (readonly, retain)  NSString *       presenceId;
@property (readwrite, retain) NSString *       name;
@property (readwrite, retain) PresenceStatus * status;
@property (readwrite, retain) NSDate *         lastUpdatedAt;

@end
