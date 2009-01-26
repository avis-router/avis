#import <Foundation/Foundation.h>

extern NSString *PrefOnlineUserName;
extern NSString *PrefOnlineUserUUID;
extern NSString *PrefElvinURL;
extern NSString *PrefDefaultSendGroup;
extern NSString *PrefPresenceGroups;
extern NSString *PrefPresenceBuddies;

static inline NSString *prefString (const NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] stringForKey: (NSString *)name];
}

static inline NSArray *prefArray (const NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] arrayForKey: (NSString *)name];
}
