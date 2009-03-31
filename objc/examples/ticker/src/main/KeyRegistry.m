#import "KeyRegistry.h"
#import "ElvinKey.h"
#import "utils.h"

#define regError(code, message, ...) \
  makeError (@"ticker.key_registry", code, message, ##__VA_ARGS__)

@implementation KeyRegistry

- (id) initWithKeysInDir: (NSString *) dir error: (NSError **) error
{
  if (!(self = [super init]))
    return nil;

  keys = [[[NSMutableArray alloc] initWithCapacity: 10] retain];
  
  NSDirectoryEnumerator *direnum = 
    [[NSFileManager defaultManager] enumeratorAtPath: dir];
    
  if (direnum == nil)
  {
    *error = 
      regError (REG_ERROR_MISSING_DIR, @"Directory \"%@\" is not readable", dir);

    return nil;
  }
  
  NSString *file;
  
  while (file = [direnum nextObject])
  {
    if ([[file pathExtension] isEqualToString: @"key"])
    {
      NSError *keyError;
      NSString *fullPath = [dir stringByAppendingPathComponent: file];
      
      ElvinKey *key = 
        [[ElvinKey alloc] initWithFile: fullPath error: &keyError];

      if (key)
      {
        [keys addObject: key];
      } else
      {
        NSLog (@"Skipping unreadable key \"%@\": %@", 
               fullPath, [keyError localizedDescription]);
      }
    }
  }
  
  return self;
}

- (void) dealloc
{
  [keys release];
  
  [super dealloc];
}

- (NSUInteger) count
{
  return [keys count];
}

@synthesize keys;

@end
