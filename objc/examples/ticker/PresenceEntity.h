#import <Foundation/Foundation.h>

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

@property (readonly, retain)           NSString *       presenceId;
@property (readwrite, retain) IBOutlet NSString *       name;
@property (readwrite, assign) IBOutlet PresenceStatus * status;
@property (readwrite, retain) IBOutlet NSDate *         lastUpdatedAt;

@end
