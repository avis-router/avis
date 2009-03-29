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

  if (!key)
  {
    NSLog (@"Failed to read key: %@", 
           [error localizedFailureReason]);
  }
  
  STAssertNotNil (error, @"Error reading key");
}

@end
