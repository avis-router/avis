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

static NSString *listToString (NSArray *list)
{
  if ([list count] == 0)
    return @"";
  
  NSMutableString *string = [NSMutableString string];
  
  for (NSString *item in list)
  {
    [string appendString: @"|"];
    [string appendString: [item lowercaseString]];
  }
  
  [string appendString: @"|"];
  
  return string;
}

#pragma mark -

@interface PresenceConnection (PRIVATE)
  - (void) handlePresenceInfo: (NSDictionary *) notification;
  - (void) handlePresenceRequest: (NSDictionary *) notification;
  - (NSString *) presenceInfoSubscription;
  - (NSString *) presenceRequestSubscription;
  - (void) requestPresenceInfo;
  - (void) emitPresenceInfo;
  - (void) emitPresenceInfo: (NSString *) inReplyTo 
           includingFields: (PresenceFields) fields;
  - (PresenceEntity *) findUserWithId: (NSString *) presenceId;
  - (void) handleElvinOpen: (void *) unused;
  - (void) handleElvinClose: (void *) unused;
  - (void) clearPresenceEntities;
@end

@implementation PresenceConnection

- (id) initWithElvin: (ElvinConnection *) theElvinConnection
{
  if (!(self = [super init]))
    return nil;

  elvin = theElvinConnection;
  entities = [[NSMutableSet setWithCapacity: 5] retain];
  presenceStatus = [[PresenceStatus onlineStatus] retain];
  
  // suscribe to incoming presence info
  // TODO resub on user name change
  [elvin subscribe: [self presenceInfoSubscription] withDelegate: self 
     usingSelector: @selector (handlePresenceInfo:)];

  // subscribe to requests
  [elvin subscribe: [self presenceRequestSubscription] withDelegate: self 
     usingSelector: @selector (handlePresenceRequest:)];
  
  if ([elvin isConnected])
  {
    [self emitPresenceInfo];
    [self requestPresenceInfo];
  }
  
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
  [presenceStatus release];
  
  [super dealloc];
}

#pragma mark -

- (NSString *) presenceRequestSubscription
{
  // TODO escape user name
  NSMutableString *expr = 
    [NSMutableString stringWithString: 
     @"Presence-Protocol < 2000 && string (Presence-Request) && "];
     
  [expr appendString: 
    [NSString stringWithFormat: @"(contains (fold-case (Users), \"|%@|\")", 
              [prefString (PrefOnlineUserName) lowercaseString]]];
  
  NSArray *groups = prefArray (PrefPresenceGroups);
  
  if ([groups count] >  0)
  {
    [expr appendString: 
      [NSString stringWithFormat: @" || contains (fold-case (Groups), \"%@\")", 
                listToString (prefArray (PrefPresenceGroups))]];
  }
  
  [expr appendString: @")"];
  
  NSLog (@"request sub: %@", expr);
  
  return expr;
}

- (void) requestPresenceInfo
{
  // TODO support users, groups and distribution
  [elvin sendPresenceRequestMessage: prefString (PrefOnlineUserUUID) 
                      fromRequestor: prefString (PrefOnlineUserName) 
                           toGroups: listToString (prefArray (PrefPresenceGroups)) 
                           andUsers: listToString (prefArray (PrefPresenceBuddies)) 
                         sendPublic: YES];
}

- (void) emitPresenceInfo
{
  [self emitPresenceInfo: @"initial" includingFields: FieldsAll];
}

- (void) emitPresenceInfo: (NSString *) inReplyTo 
         includingFields: (PresenceFields) fields
{
  [elvin sendPresenceInfoMessage: prefString (PrefOnlineUserUUID) 
                         forUser: prefString (PrefOnlineUserName) 
                       inReplyTo: inReplyTo
                      withStatus: presenceStatus
                        toGroups: listToString (prefArray (PrefPresenceGroups)) 
                        andUsers: listToString (prefArray (PrefPresenceBuddies)) 
                   fromUserAgent: @"Blue Sticker"
                 includingFields: fields
                      sendPublic: YES];
}

- (NSString *) presenceInfoSubscription
{
  // TODO escape user name
  // TODO restrict subscription
  return [NSString stringWithFormat: 
          @"Presence-Protocol < 2000 && string (Groups) && string (User) && \
          string (Client-Id) && User != \"%@\"", 
          prefString (PrefOnlineUserName)];
}

- (void) handlePresenceRequest: (NSDictionary *) notification
{
  [self emitPresenceInfo: [notification objectForKey: @"Presence-Request"]
         includingFields: FieldsAll];
}

- (void) handlePresenceInfo: (NSDictionary *) notification
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
    user.status.statusCode = [PresenceStatus statusCodeFromString: status];
  
  if (statusText)
    user.status.statusText = statusText;
    
  // TODO set duration
  
  user.lastUpdatedAt = [NSDate date];
  
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
  if ([[NSThread currentThread] isMainThread])
  {
    [self clearPresenceEntities];
    [self requestPresenceInfo];
    [self emitPresenceInfo];
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinOpen:) 
          withObject: nil waitUntilDone: NO];
  }
}

- (void) handleElvinClose: (void *) unused
{
  // TODO
}

#pragma mark -

@synthesize entities;

- (void) clearPresenceEntities
{
  NSMutableSet *newEntities = [NSMutableSet set];
  
  [self willChangeValueForKey: @"entities" 
              withSetMutation: NSKeyValueSetSetMutation
                 usingObjects: newEntities];
  
  [entities removeAllObjects];

  [self didChangeValueForKey: @"entities" 
             withSetMutation: NSKeyValueSetSetMutation
                usingObjects: newEntities];
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

@end
