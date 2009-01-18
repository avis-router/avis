#import "PresenceConnection.h"

#import "ElvinConnection.h"
#import "PresenceEntity.h"

@implementation PresenceConnection

- (id) initWithElvin: (ElvinConnection *) theElvinConnection
{
  if (!(self = [super init]))
    return nil;

  elvin = theElvinConnection;
  entities = [[NSMutableArray arrayWithCapacity: 5] retain];
  
  [entities addObject: [[PresenceEntity alloc] initWithName: @"Matthew"]];
  [entities addObject: [[PresenceEntity alloc] initWithName: @"Fred"]];
  [entities addObject: [[PresenceEntity alloc] initWithName: @"John"]];
  
  return self;
}

- (void) dealloc
{
  [entities release];
  
  [super dealloc];
}


@synthesize entities;

@end
