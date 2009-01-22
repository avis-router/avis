#import "PresenceEntity.h"

#import "utils.h"

@implementation PresenceEntity

+ (OnlineStatus) statusFromString: (NSString *) string
{
  if ([string isEqual: @"online"])
    return ONLINE;
  else if ([string isEqual: @"unavailable"])
    return UNAVAILABLE;
  else if ([string isEqual: @"unavailable?"])
    return MAYBE_UNAVAILABLE;
  else if ([string isEqual: @"coffee"])
    return COFFEE;
  else
    return OFFLINE;
}

- (id) initWithId: (NSString *) newId;
{
  if (!(self = [super init]))
    return nil;
    
  presenceId = [newId retain];
  status = OFFLINE;
  
  return self;
}

+ entityWithName: (NSString *) name
{
  PresenceEntity *entity = 
    [[[PresenceEntity alloc] initWithId: uuidString ()] autorelease];
  
  [entity setName: name];
  
  return entity;
}

@synthesize presenceId;
@synthesize name;
@synthesize status;
@synthesize statusText;
@synthesize lastUpdatedAt;

@end
