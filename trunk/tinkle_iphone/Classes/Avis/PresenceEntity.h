#import <Foundation/Foundation.h>

#import "PresenceStatus.h"

@interface PresenceEntity : NSObject
{
  NSString *       presenceId;
  NSString *       name;
  PresenceStatus * status;
  NSString *       userAgent;
  NSDate *         lastUpdatedAt;
}

+ entityWithName: (NSString *) name;

- (id) initWithId: (NSString *) newId;

- (NSComparisonResult) sortByUserName: (PresenceEntity *) entity;

@property (readonly, retain)  NSString *       presenceId;
@property (readwrite, retain) NSString *       name;
@property (readwrite, retain) PresenceStatus * status;
@property (readwrite, retain) NSString *       userAgent;
@property (readwrite, retain) NSDate *         lastUpdatedAt;

@end
