#import "TestKeyRegistry.h"

#import "Key.h"

@implementation TestKeyRegistry

- (void) testReadKey
{
  NSString *resourceDir = 
    [NSString stringWithCString: getenv ("TEST_RESOURCES_DIR") 
                       encoding: NSASCIIStringEncoding];

  //STFail (@"Failed!");
  //KeyRegistry *registry = [KeyRegistry registryWithKeysInDir: @"test_keys"];
  //KeyRegistry *registry = [[KeyRegistry alloc] init];
  
  NSError *error;
  Key *key = 
    [[Key alloc] 
       initWithFile: [NSString stringWithFormat: @"%@/test_keys/key1.key", resourceDir] 
       error: &error];

  STAssertNotNil (key, @"Error reading key: %@", [error localizedDescription]);
}

@end
