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
  
  NSError *error = nil;
  Key *key1 = 
    [[[Key alloc] 
       initWithFile: 
         [NSString stringWithFormat: @"%@/test_keys/key1.key", resourceDir] 
       error: &error] autorelease];

  unsigned char key_data [16] = 
    {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
  
  STAssertNotNil (key1, @"Error reading key: %@", [error localizedDescription]);
  STAssertEqualObjects (@"test key #1", key1.name, @"Names not equal");
  STAssertEquals (KEY_TYPE_PUBLIC, key1.type, @"Types not equal");
  STAssertEqualObjects 
    ([NSData dataWithBytes: key_data length: sizeof (key_data)], key1.data, 
     @"Data not equal");
  
  error = nil;
  
  Key *key2 = 
    [[[Key alloc] 
      initWithFile: 
      [NSString stringWithFormat: @"%@/test_keys/key_error_version.key", resourceDir] 
        error: &error] autorelease];
  
  STAssertNil (key2, @"Key must be nil");
  STAssertNotNil (error, @"Error must be set");
  STAssertEquals (KEY_IO_VERSION, [error code], @"Wrong error code");
  
  error = nil;
  
  Key *key3 = 
    [[[Key alloc] 
      initWithFile: 
        [NSString stringWithFormat: @"%@/test_keys/key_error_hexdata.key", resourceDir] 
        error: &error] autorelease];
  
  STAssertNil (key3, @"Key must be nil");
  STAssertNotNil (error, @"Error must be set");
  STAssertEquals (KEY_IO_BAD_HEX_DIGIT, [error code], @"Wrong error code");
  
  error = nil;
  
  Key *key4 = 
  [[[Key alloc] 
    initWithFile: 
    [NSString stringWithFormat: @"%@/test_keys/key_error_version.key", resourceDir] 
    error: &error] autorelease];
  
  STAssertNil (key4, @"Key must be nil");
  STAssertNotNil (error, @"Error must be set");
  STAssertEquals (KEY_IO_VERSION, [error code], @"Wrong error code");
  
  error = nil;
  
  Key *key5 = 
    [[[Key alloc] 
      initWithFile: 
      [NSString stringWithFormat: @"%@/test_keys/key2.key", resourceDir] 
      error: &error] autorelease];
  
  STAssertNotNil (key5, @"Error reading key: %@", [error localizedDescription]);
  STAssertEqualObjects (@"test key #2", key5.name, @"Names not equal");
  STAssertEquals (KEY_TYPE_PRIVATE, key5.type, @"Types not equal");
}

@end
