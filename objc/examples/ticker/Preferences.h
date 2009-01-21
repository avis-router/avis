#import <Foundation/Foundation.h>

extern NSString *PrefOnlineUserName;
extern NSString *PrefOnlineUserUUID;
extern NSString *PrefElvinURL;
extern NSString *PrefDefaultSendGroup;

static inline NSString *prefString (const NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] stringForKey: (NSString *)name];
}
