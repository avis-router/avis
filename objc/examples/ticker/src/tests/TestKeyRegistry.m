#import "TestKeyRegistry.h"

#import "Key.h"

@implementation TestKeyRegistry

- (void) testReadKey
{
  //STFail (@"Failed!");
  //KeyRegistry *registry = [KeyRegistry registryWithKeysInDir: @"test_keys"];
  //KeyRegistry *registry = [[KeyRegistry alloc] init];
  
  NSError *error;
  Key *key = [[Key alloc] initWithFile: @"test_keys/key1.key" error: &error];

  STAssertNotNil (key, @"Error reading key: %@", [error localizedDescription]);
}

@end
