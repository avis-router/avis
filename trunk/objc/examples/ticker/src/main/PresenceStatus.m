#import "PresenceStatus.h"

@implementation PresenceStatus

@synthesize statusCode;
@synthesize statusText;
@synthesize changedAt;

+ (OnlineStatus) statusCodeFromString: (NSString *) string
{
  if ([string isEqual: @"online"])
    return ONLINE;
  else if ([string isEqual: @"unavailable"])
    return UNAVAILABLE;
  else if ([string isEqual: @"unavailable?"])
    return MAYBE_UNAVAILABLE;
  else if ([string isEqual: @"coffee"])
    return COFFEE;
  else
    return OFFLINE;
}

static inline PresenceStatus *status (OnlineStatus statusCode, NSString *text)
{
  PresenceStatus *status = [[PresenceStatus new] autorelease];
  
  status.statusCode = statusCode;
  status.statusText = text;
  status.changedAt = nil; // wildcard
  
  return status;
}

+ (PresenceStatus *) onlineStatus
{
  return status (ONLINE, @"Online");
}

+ (PresenceStatus *) offlineStatus
{
  return status (OFFLINE, @"Logged off");
}

+ (PresenceStatus *) coffeeStatus
{
  return status (COFFEE,  @"Coffee break!");
}

+ (PresenceStatus *) composingStatus
{
  return status (ONLINE,  @"Composing...");
}

+ (PresenceStatus *) awayStatus
{
  return status (UNAVAILABLE, @"Away");
}

+ (PresenceStatus *) inactiveStatus
{
  return status (MAYBE_UNAVAILABLE, @"Inactive");
}

+ (PresenceStatus *) doNotDisturbStatus
{
  return status (UNAVAILABLE, @"Please do not disturb");
}

+ (PresenceStatus *) status: (OnlineStatus) code text: (NSString *) text
{
  return status (code, text);
}

- (void) dealloc
{
  [changedAt release];
  [statusText release];
  
  [super dealloc];
}

- (id) copyWithZone: (NSZone *) zone
{
  PresenceStatus *copy = [[[self class] allocWithZone: zone] init];
  
  copy->statusCode = statusCode;
  copy->statusText = [statusText copyWithZone: zone];
  copy->changedAt = [changedAt copyWithZone: zone];
   
  return copy;
}

- (BOOL) isEqual: (id) object
{
  if ([object isKindOfClass: [PresenceStatus class]])
  {
    PresenceStatus *status = object;
    
    return status->statusCode == statusCode && 
           (status->changedAt == nil || 
             [status->changedAt isEqual: changedAt]) &&
           [status->statusText isEqual: statusText];
  } else
  {
    return NO;
  }
}

- (NSString *) statusCodeAsString
{
  switch (statusCode)
  { 
    case ONLINE:
      return @"online";
    case UNAVAILABLE:
      return @"unavailable";
    case MAYBE_UNAVAILABLE:
      return @"unavailable?";
    case COFFEE:
      return @"coffee";
    default:
      return @"offline";
  }
}

- (NSString *) statusCodeAsUIString
{
  switch (statusCode)
  { 
    case ONLINE:
      return @"Online";
    case UNAVAILABLE:
      return @"Unavailable";
    case MAYBE_UNAVAILABLE:
      return @"Away";
    case COFFEE:
      return @"At Coffee Break!";
    default:
      return @"Offline";
  }
}

- (NSString *) statusDurationAsUIString
{
  // TODO
  return @"?? seconds ago";
}

- (uint32_t) statusDurationAsSecondsElapsed
{
  return (uint32_t)[[NSDate date] timeIntervalSinceDate: changedAt];
}

- (NSString *) description
{
  return statusText;
}

@end
