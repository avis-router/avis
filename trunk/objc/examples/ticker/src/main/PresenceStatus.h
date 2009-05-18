#import <Foundation/Foundation.h>

typedef enum 
{ONLINE, MAYBE_UNAVAILABLE, UNAVAILABLE, COFFEE, OFFLINE} OnlineStatus; 

@interface PresenceStatus : NSObject 
{
  OnlineStatus statusCode;
  NSString *   statusText;
  NSDate *     changedAt;
}

+ (OnlineStatus)     statusCodeFromString: (NSString *) string;
+ (PresenceStatus *) onlineStatus;
+ (PresenceStatus *) offlineStatus;
+ (PresenceStatus *) inactiveStatus;

- (id) copyWithZone: (NSZone *) zone;

@property (readwrite, assign) OnlineStatus statusCode;
@property (readonly)          NSString *   statusCodeAsString;
@property (readonly)          NSString *   statusCodeAsUIString;
@property (readonly)          NSString *   statusDurationAsUIString;
@property (readonly)          uint32_t     statusDurationAsSecondsElapsed;
@property (readwrite, retain) NSString *   statusText;
@property (readwrite, retain) NSDate *     changedAt;

@end
