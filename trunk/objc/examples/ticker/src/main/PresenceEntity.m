#import "PresenceEntity.h"

#import "utils.h"

@implementation PresenceEntity

+ entityWithName: (NSString *) name
{
  PresenceEntity *entity = 
    [[[PresenceEntity alloc] initWithId: uuidString ()] autorelease];
  
  [entity setName: name];
  
  return entity;
}

- (id) initWithId: (NSString *) newId;
{
  if (!(self = [super init]))
    return nil;
    
  presenceId = [newId retain];
  status = [[PresenceStatus onlineStatus] retain];
  
  return self;
}

- (void) dealloc
{
  [presenceId release];
  [name release];
  [status release];
  [lastUpdatedAt release];
  
  [super dealloc];
}

- (id) copyWithZone: (NSZone *) zone
{
  PresenceEntity *copy = [[[self class] allocWithZone: zone] retain];
  
  copy->presenceId = [[presenceId copyWithZone: zone] retain];
  copy->name = [[name copyWithZone: zone] retain];
  copy->status = [[status copyWithZone: zone] retain];
  copy->lastUpdatedAt = [[lastUpdatedAt copyWithZone: zone] retain];
  
  return copy;
}

// TODO

- (BOOL) isEqual: (id) object
{
  if ([object isKindOfClass: [PresenceEntity class]])
  {
    PresenceEntity *entity = object;
    
    return [entity->presenceId isEqual: presenceId];
  } else
  {
    return NO;
  }
}

@synthesize presenceId;
@synthesize name;
@synthesize status;
@synthesize lastUpdatedAt;

@end
