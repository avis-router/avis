#import "PresenceEntity.h"

#import "utils.h"

@implementation PresenceEntity

- (id) initWithId: (NSString *) newId;
{
  if (!(self = [super init]))
    return nil;
    
  presenceId = [newId retain];
  status = [[PresenceStatus onlineStatus] retain];
  
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
@synthesize lastUpdatedAt;

@end
