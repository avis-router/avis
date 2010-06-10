#import <Foundation/Foundation.h>

extern NSString *PrefOnlineUserName;
extern NSString *PrefOnlineUserUUID;
extern NSString *PrefElvinURL;
extern NSString *PrefDefaultSendGroup;
extern NSString *PrefPresenceGroups;
extern NSString *PrefTickerGroups;
extern NSString *PrefPresenceBuddies;
extern NSString *PrefTickerSubscription;
extern NSString *PrefPresenceColumnSorting;
extern NSString *PrefElvinKeys;
extern NSString *PrefShowUnreadMessageCount;
extern NSString *PrefShowPresenceWindow;
extern NSString *PrefSecureGroups;
extern NSString *PrefShowConnectHelper;

static inline NSString *prefString (const NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] stringForKey: (NSString *)name];
}

static inline NSArray *prefArray (const NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] arrayForKey: (NSString *)name];
}

static inline NSDictionary *prefDict (const NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] dictionaryForKey: (NSString *)name];
}

static inline BOOL prefBool (const NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] boolForKey: (NSString *)name];
}

void registerUserDefaults ();