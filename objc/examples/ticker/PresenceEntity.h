#import <Foundation/Foundation.h>

typedef enum 
{ONLINE, MAYBE_UNAVAILABLE, UNAVAILABLE, COFFEE, OFFLINE} OnlineStatus; 

@interface PresenceEntity : NSObject
{
  NSString *   presenceId;
  NSString *   name;
  OnlineStatus status;
  NSString *   statusText;
  NSDate *     lastUpdatedAt;
}

+ (OnlineStatus) statusFromString: (NSString *) string;

- (id) initWithId: (NSString *) newId;

+ entityWithName: (NSString *) name;

@property (readonly, retain)           NSString *   presenceId;
@property (readwrite, retain) IBOutlet NSString *   name;
@property (readwrite, assign) IBOutlet OnlineStatus status;
@property (readwrite, retain) IBOutlet NSString *   statusText;
@property (readwrite, retain) IBOutlet NSDate *     lastUpdatedAt;

@end
