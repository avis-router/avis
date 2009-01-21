#import <Foundation/Foundation.h>

extern const NSString *PrefOnlineUserName;

static inline NSString *prefString (const NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] stringForKey: (NSString *)name];
}
