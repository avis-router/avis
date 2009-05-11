#import "PresenceConnection.h"

#import "utils.h"

#import "ElvinConnection.h"
#import "PresenceEntity.h"
#import "Preferences.h"

#import "RHSystemIdleTimer.h"

// Time (in seconds) before a user is considered stale and in need of refresh
#define STALE_USER_AGE (5 * 60)

// Time (in seconds) after idle mode starts before the user is considered away
#define AUTO_IDLE_TIME (5 * 60)

static NSString *stringValueForAttribute (NSDictionary *notification, 
                                          NSString *attribute)
{
  NSObject *value = [notification valueForKey: attribute];

  if ([value isKindOfClass: [NSString class]])
    return (NSString *)value;
  else
    return nil;
}

/**
 * Turn a string list into the format: "|item1|item2|".
 */
static NSString *listToBarDelimitedString (NSArray *list)
{
  if ([list count] == 0)
    return @"";
  
  NSMutableString *string = [NSMutableString string];
  
  for (NSString *item in list)
  {
    [string appendString: @"|"];
    // TODO escape item
    [string appendString: [item lowercaseString]];
  }
  
  [string appendString: @"|"];
  
  return string;
}

/**
 * Turn a string list into the format: "|item1|", "|item2|", ...
 */
static NSString *listToParameterString (NSArray *list)
{
  NSMutableString *string = [NSMutableString string];
  
  // TODO escape item
  for (NSString *item in list)
    [string appendFormat: @", \"|%@|\"", [item lowercaseString]];

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
  - (void) emitPresenceInfoAsStatusUpdate;
  - (void) emitPresenceInfo: (NSString *) inReplyTo 
           includingFields: (PresenceFields) fields;
  - (void) handleElvinOpen: (void *) unused;
  - (void) handleElvinClose: (void *) unused;
  - (void) clearEntities;
  - (PresenceEntity *) findUserWithId: (NSString *) presenceId;
  - (void) resetLivenessTimer;
  - (void) startAutoAwayTimer;
  - (void) stopAutoAwayTimer;
@end

@implementation PresenceConnection

- (id) initWithElvin: (ElvinConnection *) theElvinConnection
{
  if (!(self = [super init]))
    return nil;

  elvin = theElvinConnection;
  entities = [[NSMutableSet setWithCapacity: 5] retain];
  presenceStatus = [[PresenceStatus onlineStatus] retain];
  
  // subscribe to incoming presence info
  // TODO resub on user name/groups change
  // TODO re-emit presence on groups change
  [elvin subscribe: [self presenceInfoSubscription] withDelegate: self 
     usingSelector: @selector (handlePresenceInfo:)];

  // subscribe to requests
  [elvin subscribe: [self presenceRequestSubscription] withDelegate: self 
     usingSelector: @selector (handlePresenceRequest:)];
    
  // listen for elvin open/close
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];
  
  [notifications addObserver: self selector: @selector (handleElvinOpen:)
                        name: ElvinConnectionOpenedNotification object: nil]; 
  
  [notifications addObserver: self selector: @selector (handleElvinClose:)
                        name: ElvinConnectionWillCloseNotification object: nil]; 

  [notifications addObserver: self selector: @selector (handleElvinClose:)
                        name: ElvinConnectionClosedNotification object: nil]; 
                          
  if ([elvin isConnected])
  {
    [self emitPresenceInfo];
    [self requestPresenceInfo];
  }

  [self startAutoAwayTimer];
  
  return self;
}

- (void) dealloc
{
  [self stopAutoAwayTimer];
  
  [entities release];
  [presenceStatus release];
  
  [[NSNotificationCenter defaultCenter] removeObserver: self];
  
  [super dealloc];
}

- (PresenceStatus *) presenceStatus
{
  return presenceStatus;
}

- (void) setPresenceStatusSilently: (PresenceStatus *) newStatus
{
  if (![newStatus isEqual: presenceStatus])
  {
    [presenceStatus release];
  
    presenceStatus = [newStatus retain];
  }
}

- (void) setPresenceStatus: (PresenceStatus *) newStatus
{
  if (![newStatus isEqual: presenceStatus])
  {
    [self setPresenceStatusSilently: newStatus];
    
    if ([elvin isConnected])
      [self emitPresenceInfoAsStatusUpdate];
  }
}

#pragma mark -

- (void) handleElvinOpen: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    [self setPresenceStatusSilently: [PresenceStatus onlineStatus]];
    [self clearEntities];
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
  if ([[NSThread currentThread] isMainThread])
  {
    [self clearEntities];
    [self setPresenceStatus: [PresenceStatus offlineStatus]];
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinClose:) 
          withObject: nil waitUntilDone: NO];
  }
}

#pragma mark -

- (NSString *) presenceRequestSubscription
{
  NSString *userName = prefString (PrefOnlineUserName);
  
  // TODO escape user name
  NSMutableString *expr = 
    [NSMutableString stringWithFormat: 
     @"Presence-Protocol < 2000 && string (Presence-Request) && \
     Requestor != \"%@\" && ", userName];
     
  [expr appendFormat: @"(contains (fold-case (Users), \"|%@|\")", 
    [userName lowercaseString]];
  
  NSArray *groups = prefArray (PrefPresenceGroups);
  
  if ([groups count] > 0)
  {
    [expr appendFormat: @" || contains (fold-case (Groups) %@)", 
      listToParameterString (groups)];
  }
  
  [expr appendString: @")"];
  
  TRACE (@"Presence request subscription is: %@", expr);
  
  return expr;
}

- (void) handlePresenceRequest: (NSDictionary *) notification
{
  [self emitPresenceInfo: [notification objectForKey: @"Presence-Request"]
         includingFields: FieldsAll];
}

- (NSString *) presenceInfoSubscription
{
  // TODO escape user name
  NSMutableString *expr = [NSMutableString stringWithFormat: 
    @"Presence-Protocol < 2000 && string (Groups) && string (User) && \
      string (Client-Id) && User != \"%@\" ", prefString (PrefOnlineUserName)];
  
  // TODO this subs to all groups when array is empty
  NSArray *groups = prefArray (PrefPresenceGroups);
  
  if ([groups count] > 0)
  {
    [expr appendFormat: @" && contains (fold-case (Groups) %@)", 
      listToParameterString (groups)];
  }
  
  TRACE (@"Presence info subscription is: %@", expr);
  
  return expr;
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

  user.name = userName;
  
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

- (void) requestPresenceInfo
{
  // TODO support users, groups and distribution prefs
  NSString *groups = listToBarDelimitedString (prefArray (PrefPresenceGroups));
  NSString *buddies = 
    listToBarDelimitedString (prefArray (PrefPresenceBuddies));
  
  [elvin sendPresenceRequestMessage: prefString (PrefOnlineUserUUID) 
                      fromRequestor: prefString (PrefOnlineUserName) 
                           toGroups: groups
                           andUsers: buddies
                         sendPublic: YES];
}

/**
 * Clear and re-start (if online) the liveness timer that sends out updates
 * when more than STALE_USER_AGE seconds are about to pass with no global
 * presence info being sent.
 */
- (void) resetLivenessTimer
{
  [NSObject cancelPreviousPerformRequestsWithTarget: self];
  
  if (presenceStatus.statusCode != OFFLINE)
  {
    [self performSelector: 
      @selector (emitPresenceInfoAsStatusUpdate) withObject: nil 
      afterDelay: STALE_USER_AGE - 5];
  }
}

#pragma mark Auto away idle timer methods

- (void) timerContinuesIdling: (id) sender
{
  if ([presenceStatus statusCode] == ONLINE)
  {
    NSLog (@"User is idle");
      
    [self setPresenceStatus: [PresenceStatus inactiveStatus]]; 
  }
}

- (void) timerFinishedIdling: (id) sender
{
  if ([presenceStatus statusCode] == MAYBE_UNAVAILABLE)
  {
    NSLog (@"User is active");
    
    [self setPresenceStatus: [PresenceStatus onlineStatus]]; 
  }
}

- (void) startAutoAwayTimer
{
  idleTimer =
    [[[RHSystemIdleTimer alloc] 
      initSystemIdleTimerWithTimeInterval: AUTO_IDLE_TIME] retain];
  [idleTimer setDelegate: self];
}

- (void) stopAutoAwayTimer
{
  [idleTimer invalidate];
  [idleTimer release];
}

#pragma mark Presence info messaging

- (void) emitPresenceInfo
{
  [self emitPresenceInfo: @"initial" includingFields: FieldsAll];
}

- (void) emitPresenceInfoAsStatusUpdate
{
  [self emitPresenceInfo: @"update" includingFields: FieldStatus];
}

- (void) emitPresenceInfo: (NSString *) inReplyTo 
         includingFields: (PresenceFields) fields
{
  if (![elvin isConnected])
    return;

  NSString *groups = listToBarDelimitedString (prefArray (PrefPresenceGroups));
  NSString *buddies = 
    (fields & FieldBuddies) ? 
      listToBarDelimitedString (prefArray (PrefPresenceBuddies)) : nil;

  [elvin sendPresenceInfoMessage: prefString (PrefOnlineUserUUID) 
                         forUser: prefString (PrefOnlineUserName) 
                       inReplyTo: inReplyTo
                      withStatus: presenceStatus
                        toGroups: groups 
                      andBuddies: buddies
                 includingFields: fields
                      sendPublic: YES];

  if ([inReplyTo isEqual: @"initial"] || [inReplyTo isEqual: @"update"])
    [self resetLivenessTimer];
}

- (void) refresh
{
  [self clearEntities];
  [self requestPresenceInfo];
}

#pragma mark -

@synthesize entities;

- (void) clearEntities
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
