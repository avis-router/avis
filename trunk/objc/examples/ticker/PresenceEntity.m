#import "PresenceEntity.h"

#import "utils.h"

@implementation PresenceEntity

- (id) init: (NSString *) newId name: (NSString *) newName
{
  if (!(self = [super init]))
    return nil;
    
  presenceId = [newId retain];
  name = [newName retain];
  
  return self;
}

- (id) initWithName: (NSString *) newName
{    
  return [self init: uuidString () name: newName];
}

@synthesize presenceId;
@synthesize name;
@synthesize status;
@synthesize statusText;
@synthesize lastUpdatedAt;

@end
