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

// TODO
//- (id) copyWithZone: (NSZone *) zone
//{
//  PresenceEntity *copy = [[self class] allocWithZone: zone];
//  
//  copy->presenceId = [presenceId copyWithZone: zone];
//  copy->name = [name copyWithZone: zone];
//  copy->status = [PresenceStatus onlineStatus];
//  
//  return copy;
//}

@synthesize presenceId;
@synthesize name;
@synthesize status;
@synthesize lastChangedAt;
@synthesize lastUpdatedAt;

@end
