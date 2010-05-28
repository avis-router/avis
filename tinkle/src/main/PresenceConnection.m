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

NSString *PresenceStatusChangedNotification = 
  @"PresenceStatusChangedNotification";

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
    [string appendString: 
      [ElvinConnection escapedSubscriptionString: [item lowercaseString]]];
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
  
  for (NSString *item in list)
    [string appendFormat: @", \"|%@|\"", 
      [ElvinConnection escapedSubscriptionString: [item lowercaseString]]];

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
  - (void) setPresenceStatusSilently: (PresenceStatus *) newStatus;
@end

@implementation PresenceConnection

@synthesize entities, userId;

- (id) initWithElvin: (ElvinConnection *) theElvinConnection 
            userId: (NSString *) newUserId userName: (NSString *) newUserName
            groups: (NSArray *) newGroups buddies: (NSArray *) newBuddies
{
  if (!(self = [super init]))
    return nil;

  elvin = theElvinConnection;
  userId = [newUserId retain];
  userName = [newUserName retain];
  groups = [newGroups retain];
  buddies = [newBuddies retain];
  
  entities = [[NSMutableSet setWithCapacity: 5] retain];
  presenceStatus = [[PresenceStatus onlineStatus] retain];
  
  presenceStatus.changedAt = [NSDate date];
  
  // subscribe to incoming presence info
  presenceInfoSubscription =
    [elvin subscribe: [self presenceInfoSubscription] withDelegate: self 
       onNotify: @selector (handlePresenceInfo:) onError: nil];

  // subscribe to requests
  presenceRequestSubscription = 
    [elvin subscribe: [self presenceRequestSubscription] withDelegate: self 
       onNotify: @selector (handlePresenceRequest:) onError: nil];
    
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

- (NSString *) userName
{
  return userName;
}

- (void) setUserName: (NSString *) newUserName
{
  if (![userName isEqual: newUserName])
  {
    self.presenceStatus = [PresenceStatus offlineStatus];
 
    userName = [newUserName retain];
    
    [elvin resubscribe: presenceRequestSubscription
           usingSubscription: [self presenceRequestSubscription]];

    self.presenceStatus = [PresenceStatus onlineStatus];
  }
}

- (NSArray *) groups
{
  return groups;
}

- (void) setGroups: (NSArray *) newGroups
{
  if (![groups isEqualToArray: newGroups])
  {
    [groups release];
    
    groups = [newGroups copy];
    
    [elvin resubscribe: presenceInfoSubscription 
       usingSubscription: [self presenceInfoSubscription]];
    
    [elvin resubscribe: presenceRequestSubscription
      usingSubscription: [self presenceRequestSubscription]];
    
    [self emitPresenceInfo];
    [self requestPresenceInfo];
  }
}

- (NSArray *) buddies
{
  return buddies;
}

- (void) setBuddies: (NSArray *) newBuddies
{
  if (![buddies isEqualToArray: newBuddies])
  {
    [buddies release];
    
    buddies = [newBuddies copy];
    
    [elvin resubscribe: presenceInfoSubscription 
     usingSubscription: [self presenceInfoSubscription]];
    
    [elvin resubscribe: presenceRequestSubscription
     usingSubscription: [self presenceRequestSubscription]];
    
    [self emitPresenceInfo];
    [self requestPresenceInfo];
  }
}

- (void) setPresenceStatusSilently: (PresenceStatus *) newStatus
{
  if (![newStatus isEqual: presenceStatus])
  {
    [presenceStatus release];
  
    presenceStatus = [newStatus copy];
    
    if (presenceStatus.changedAt == nil)
      presenceStatus.changedAt = [NSDate date];
  }
}

- (void) setPresenceStatus: (PresenceStatus *) newStatus
{
  if (![newStatus isEqual: presenceStatus])
  {
    [self setPresenceStatusSilently: newStatus];
    
    [self emitPresenceInfoAsStatusUpdate];
  }
}

#pragma mark -

- (void) handleElvinOpen: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    if (presenceStatus.statusCode == OFFLINE)
    {
      [self willChangeValueForKey: @"presenceStatus"];
    
      [self setPresenceStatusSilently: [PresenceStatus onlineStatus]];
      
      [self didChangeValueForKey: @"presenceStatus"];
    }
    
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
    
    if (presenceStatus.statusCode == ONLINE)
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
  NSString *userNameStr = 
    [ElvinConnection escapedSubscriptionString: userName];
  
  NSMutableString *expr = 
    [NSMutableString stringWithFormat: 
     @"Presence-Protocol < 2000 && string (Presence-Request) && Requestor != '%@' && ", 
     [ElvinConnection escapedSubscriptionString: userId]];

  [expr appendFormat: @"(contains (fold-case (Users), '|%@|')", 
    [userNameStr lowercaseString]];
  
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
  NSMutableString *expr = 
    [NSMutableString stringWithFormat: 
      @"Presence-Protocol < 2000 && string (Groups) && string (User) && Client-Id != '%@'", 
      [ElvinConnection escapedSubscriptionString: userId]];
  
  if ([groups count] > 0)
  {
    [expr appendFormat: @" && contains (fold-case (Groups) %@)", 
      listToParameterString (groups)];
  } else
  {
    // don't sub to all groups when array is empty
    [expr appendString: @" && contains (Groups, '__nothing__')"];
  }

  TRACE (@"Presence info subscription is: %@", expr);
  
  return expr;
}

- (void) handlePresenceInfo: (NSDictionary *) notification
{
  BOOL createdUser;
  NSString *clientId = [notification valueForKey: @"Client-Id"];
  NSString *clientName = [notification valueForKey: @"User"];
  NSString *statusCode = stringValueForAttribute (notification, @"Status");
  NSString *statusText = stringValueForAttribute (notification, @"Status-Text");
  NSString *userAgent = stringValueForAttribute (notification, @"User-Agent");
  
  PresenceEntity *user = [self findUserWithId: clientId];
  
  if (user == nil)
  {
    user = [[[PresenceEntity alloc] initWithId: clientId] autorelease];
    
    createdUser = YES;
  } else
  {
    createdUser = NO;
  }

  OnlineStatus oldStatusCode = user.status.statusCode;
  
  user.name = clientName;
  
  if (statusCode)
    user.status.statusCode = [PresenceStatus statusCodeFromString: statusCode];
  
  if (statusText)
    user.status.statusText = statusText;
  
  if ([notification valueForKey: @"Status-Duration"])
  {
    NSInteger duration = 
      [[notification valueForKey: @"Status-Duration"] integerValue];
    
    user.status.changedAt = [[NSDate date] addTimeInterval: -duration];
  }
  
  if (userAgent)
    user.userAgent = userAgent;
  
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
  
  if (user.status.statusCode != oldStatusCode)
  {
    [[NSNotificationCenter defaultCenter] 
      postNotificationName: PresenceStatusChangedNotification object: self
      userInfo: [NSDictionary dictionaryWithObject: user forKey: @"user"]];
  }
}

- (void) requestPresenceInfo
{
  // TODO support distribution pref
  [elvin sendPresenceRequestMessage: userId
                      fromRequestor: userId
                           toGroups: listToBarDelimitedString (groups)
                           andUsers: listToBarDelimitedString (buddies)
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

  NSString *groupsStr = listToBarDelimitedString (groups);
  NSString *buddiesStr = 
    (fields & FieldBuddies) ? 
      listToBarDelimitedString (buddies) : nil;

  [elvin sendPresenceInfoMessage: userId
                         forUser: userName
                       inReplyTo: inReplyTo
                      withStatus: presenceStatus
                        toGroups: groupsStr 
                      andBuddies: buddiesStr
                 includingFields: fields
                      sendPublic: YES];

  if ([inReplyTo isEqual: @"initial"] || [inReplyTo isEqual: @"update"])
    [self resetLivenessTimer];
}

#pragma mark -

- (void) clearEntities
{
  NSSet *empty = [NSSet set];
  
  [self willChangeValueForKey: @"entities" 
              withSetMutation: NSKeyValueSetSetMutation
                 usingObjects: empty];
                 
  [self.entities removeAllObjects];
  
  [self didChangeValueForKey: @"entities" 
             withSetMutation: NSKeyValueSetSetMutation
                usingObjects: empty];
}

- (void) refresh
{
  [self clearEntities];
  [self requestPresenceInfo];
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
