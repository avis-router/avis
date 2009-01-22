#import "PresenceConnection.h"

#import "ElvinConnection.h"
#import "PresenceEntity.h"
#import "Preferences.h"

static NSString *stringValueForAttribute (NSDictionary *notification, 
                                          NSString *attribute)
{
  NSObject *value = [notification valueForKey: attribute];

  if ([value isKindOfClass: [NSString class]])
    return (NSString *)value;
  else
    return nil;
}

#pragma mark -

@interface PresenceConnection (PRIVATE)
  - (void) handlePresenceNotification: (NSDictionary *) notification;
  - (NSString *) presenceSubscription;
  - (void) requestPresenceInfo;
  - (PresenceEntity *) findUserWithId: (NSString *) presenceId;
  - (void) handleElvinOpen: (void *) unused;
  - (void) handleElvinClose: (void *) unused;
@end

@implementation PresenceConnection

- (id) initWithElvin: (ElvinConnection *) theElvinConnection
{
  if (!(self = [super init]))
    return nil;

  elvin = theElvinConnection;
  entities = [[NSMutableSet setWithCapacity: 5] retain];
  
  // TODO resub on user name change
  [elvin subscribe: [self presenceSubscription] withDelegate: self 
     usingSelector: @selector (handlePresenceNotification:)];
 
  if ([elvin isConnected])
    [self requestPresenceInfo];
  
  // listen for elvin open/close
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];
  
  [notifications addObserver: self selector: @selector (handleElvinOpen:)
                        name: ElvinConnectionOpenedNotification object: nil]; 
  
  [notifications addObserver: self selector: @selector (handleElvinClose:)
                        name: ElvinConnectionClosedNotification object: nil]; 
  
  return self;
}

- (void) dealloc
{
  [entities release];
  
  [super dealloc];
}

#pragma mark -

- (void) handlePresenceNotification: (NSDictionary *) notification
{
  BOOL createdUser;
  NSString *clientId = [notification valueForKey: @"Client-Id"];
  NSString *userName = [notification valueForKey: @"User"];
  NSString *status = stringValueForAttribute (notification, @"Status");
  NSString *statusText = stringValueForAttribute (notification, @"Status-Text");
  
  PresenceEntity *user = [self findUserWithId: clientId];
  
  if (user == nil)
  {
    user = [[PresenceEntity alloc] initWithId: clientId];
    
    createdUser = YES;
  } else
  {
    createdUser = NO;
  }

  [user setName: userName];
  
  if (status)
    [user setStatus: [PresenceEntity statusFromString: status]];
  
  if (statusText)
    [user setStatusText: statusText];
  
  [user setLastUpdatedAt: [NSDate date]];
  
  if (createdUser)
  {
    NSSet *addedObjects = [NSSet setWithObject: user];
    
    [self willChangeValueForKey: @"entities" 
                withSetMutation: NSKeyValueUnionSetMutation
                   usingObjects: addedObjects];
    
    [entities addObject: user];

    [self didChangeValueForKey: @"entities" 
               withSetMutation: NSKeyValueUnionSetMutation
                  usingObjects: addedObjects];
    
  }
}

- (void) handleElvinOpen: (void *) unused
{
  [self requestPresenceInfo];
}

- (void) handleElvinClose: (void *) unused
{
  // TODO
}

#pragma mark -

@synthesize entities;

- (NSString *) presenceSubscription
{
  // TODO escape user name
  // TODO restrict subscription
  return [NSString stringWithFormat: 
          @"Presence-Protocol < 2000 && string (Groups) && string (User) && \
          string (Client-Id) && User != \"%@\"", 
          prefString (PrefOnlineUserName)];
}

- (PresenceEntity *) findUserWithId: (NSString *) presenceId
{  
  for (PresenceEntity *e in entities)
  {
    if ([[e presenceId] isEqual: presenceId])
      return e;
  }
  
  return nil;
}

- (void) requestPresenceInfo
{
  // TODO support users, groups and distribution
  [elvin sendPresenceRequestMessage: prefString (PrefOnlineUserUUID) 
                      fromRequestor: prefString (PrefOnlineUserName) 
                          toGroups: @"|elvin|dsto|" andUsers: @"" 
                        sendPublic: YES];
}

@end
