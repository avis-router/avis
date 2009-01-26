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
  status.changedAt = [[NSDate date] retain];
  
  return status;
}

- (void) dealloc
{
  [changedAt release];
  
  [super dealloc];
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
      return @"Coffee!";
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
