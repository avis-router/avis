#import <Foundation/Foundation.h>

extern const NSString *PrefOnlineUserName;
extern const NSString *PrefOnlineUserUUID;
extern const NSString *PrefElvinURL;
extern const NSString *PrefDefaultSendGroup;

static inline NSString *prefString (const NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] stringForKey: (NSString *)name];
}
