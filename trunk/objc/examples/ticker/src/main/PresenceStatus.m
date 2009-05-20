#import "PresenceStatus.h"

@implementation PresenceStatus

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

+ (PresenceStatus *) onlineStatus
{
  PresenceStatus *status = [[PresenceStatus new] autorelease];
  
  status.statusCode = ONLINE;
  status.statusText = @"Online";
  status.changedAt = [NSDate date];
  
  return status;
}

+ (PresenceStatus *) offlineStatus
{
  PresenceStatus *status = [[PresenceStatus new] autorelease];
  
  status.statusCode = OFFLINE;
  status.statusText = @"Logged off";
  status.changedAt = [NSDate date];
  
  return status;
}

+ (PresenceStatus *) inactiveStatus
{
  PresenceStatus *status = [[PresenceStatus new] autorelease];
  
  status.statusCode = MAYBE_UNAVAILABLE;
  status.statusText = @"Inactive";
  status.changedAt = [NSDate date];
  
  return status;
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
           [status->changedAt isEqual: changedAt] &&
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

@synthesize statusCode;
@synthesize statusText;
@synthesize changedAt;

@end
