#import <Foundation/Foundation.h>

#define UUID_STRING_LENGTH 100

static inline NSString *prefsString (NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] stringForKey: name];
}

void createUUID (char *uuid);

NSString *uuidString ();