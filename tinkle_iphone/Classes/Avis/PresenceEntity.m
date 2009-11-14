#import "PresenceEntity.h"

#import "utils.h"

@implementation PresenceEntity

@synthesize presenceId, name, status, lastUpdatedAt, userAgent;

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
  status = [[PresenceStatus offlineStatus] retain];
  
  return self;
}

- (void) dealloc
{
  [presenceId release];
  [name release];
  [status release];
  [lastUpdatedAt release];
  [userAgent release];
  
  [super dealloc];
}

- (id) copyWithZone: (NSZone *) zone
{
  PresenceEntity *copy = [[[self class] allocWithZone: zone] init];
  
  copy->presenceId = [presenceId copyWithZone: zone];
  copy->name = [name copyWithZone: zone];
  copy->status = [status copyWithZone: zone];
  copy->lastUpdatedAt = [lastUpdatedAt copyWithZone: zone];
  
  return copy;
}

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

@end
