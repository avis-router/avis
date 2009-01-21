#import "PresenceConnection.h"

#import "ElvinConnection.h"
#import "PresenceEntity.h"
#import "Preferences.h"

static NSString *stringValueForKey (NSDictionary *notification, NSString *key)
{
  NSObject *value = [notification valueForKey: key];
  
  if ([value class] == [NSString class])
    return (NSString *)value;
  else
    return nil;
}

#pragma mark -

@interface PresenceConnection (PRIVATE)
  - (void) handlePresenceNotification: (NSDictionary *) notification;
  - (NSString *) presenceSubscription;
  - (PresenceEntity *) findOrCreateUser: (NSString *) presenceId;
@end

@implementation PresenceConnection

- (id) initWithElvin: (ElvinConnection *) theElvinConnection
{
  if (!(self = [super init]))
    return nil;

  elvin = theElvinConnection;
  entities = [[NSMutableArray arrayWithCapacity: 5] retain];
  
//  [entities addObject: [[PresenceEntity alloc] initWithName: @"Matthew"]];
//  [entities addObject: [[PresenceEntity alloc] initWithName: @"Fred"]];
//  [entities addObject: [[PresenceEntity alloc] initWithName: @"John"]];

  // TODO resub on user name change
  [elvin subscribe: [self presenceSubscription] withDelegate: self 
     usingSelector: @selector (handlePresenceNotification:)];
  
  return self;
}

- (void) dealloc
{
  [entities release];
  
  [super dealloc];
}


@synthesize entities;

- (NSString *) presenceSubscription
{
  // TODO escape user name
  // TODO restrict subscription
  return [NSString stringWithFormat: 
            @"Presence-Protocol < 2000 && string (Groups) && string (User) && \
              string (Client-Id) && User != \"%@\"", prefString (PrefOnlineUserName)];
}

- (void) handlePresenceNotification: (NSDictionary *) notification
{
  NSString *clientId = [notification valueForKey: @"Client-Id"];
  NSString *userName = [notification valueForKey: @"User"];
  NSString *status = stringValueForKey (notification, @"Status");
  NSString *statusText = stringValueForKey (notification, @"Status-Text");
  
  PresenceEntity *user = [self findOrCreateUser: clientId];
  
  [user setName: userName];
  
  if (status)
    [user setStatus: [PresenceEntity statusFromString: status]];
  
  if (statusText)
    [user setStatusText: statusText];
}

- (PresenceEntity *) findOrCreateUser: (NSString *) presenceId
{  
  PresenceEntity *entity = nil;
  
  for (PresenceEntity *e in entities)
  {
    if ([[e presenceId] isEqual: presenceId])
    {
      entity = e;
      break;
    }
  }
  
  if (!entity)
  {
    entity = [[PresenceEntity alloc] initWithId: presenceId];
    
    [entities addObject: entity];
  }
  
  return entity;
}

@end
