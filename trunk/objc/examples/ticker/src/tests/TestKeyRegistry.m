#import "TestKeyRegistry.h"

#import "ElvinKey.h"
#import "KeyRegistry.h"

@implementation TestKeyRegistry

- (void) testReadKey
{
  NSString *resourceDir = 
    [NSString stringWithCString: getenv ("TEST_RESOURCES_DIR") 
                       encoding: NSASCIIStringEncoding];
  
  NSError *error = nil;
  ElvinKey *key1 = 
    [[[ElvinKey alloc] 
       initWithFile: 
         [NSString stringWithFormat: @"%@/test_keys/key1.key", resourceDir] 
       error: &error] autorelease];

  unsigned char key_data [18] = 
    {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 26, 255};
  
  STAssertNotNil (key1, @"Error reading key: %@", [error localizedDescription]);
  STAssertEqualObjects (key1.name, @"test key #1", @"Names not equal");
  STAssertEquals (key1.type, KEY_TYPE_PUBLIC, @"Types not equal");
  STAssertEqualObjects 
    (key1.data, [NSData dataWithBytes: key_data length: sizeof (key_data)], 
     @"Data not equal");
  
  error = nil;
  
  ElvinKey *key2 = 
    [[[ElvinKey alloc] 
      initWithFile: 
      [NSString stringWithFormat: @"%@/test_keys/key_error_version.key", resourceDir] 
        error: &error] autorelease];
  
  STAssertNil (key2, @"Key must be nil");
  STAssertNotNil (error, @"Error must be set");
  STAssertEquals ([error code], KEY_IO_VERSION, @"Wrong error code");
  
  error = nil;
  
  ElvinKey *key3 = 
    [[[ElvinKey alloc] 
      initWithFile: 
        [NSString stringWithFormat: @"%@/test_keys/key_error_hexdata.key", resourceDir] 
        error: &error] autorelease];
  
  STAssertNil (key3, @"Key must be nil");
  STAssertNotNil (error, @"Error must be set");
  STAssertEquals ([error code], KEY_IO_BAD_HEX_DATA, @"Wrong error code");
  
  error = nil;
  
  ElvinKey *key4 = 
  [[[ElvinKey alloc] 
    initWithFile: 
    [NSString stringWithFormat: @"%@/test_keys/key_error_version.key", resourceDir] 
    error: &error] autorelease];
  
  STAssertNil (key4, @"Key must be nil");
  STAssertNotNil (error, @"Error must be set");
  STAssertEquals ([error code], KEY_IO_VERSION, @"Wrong error code");
  
  error = nil;
  
  ElvinKey *key5 = 
    [[[ElvinKey alloc] 
      initWithFile: 
      [NSString stringWithFormat: @"%@/test_keys/key2.key", resourceDir] 
      error: &error] autorelease];
  
  STAssertNotNil (key5, @"Error reading key: %@", [error localizedDescription]);
  STAssertEqualObjects (key5.name, @"test key #2", @"Names not equal");
  STAssertEquals (key5.type, KEY_TYPE_PRIVATE, @"Types not equal");
  
  error = nil;
  
  ElvinKey *key6 = 
    [[[ElvinKey alloc] 
        initWithFile: 
          [NSString stringWithFormat: @"%@/test_keys/key_error_missing_field.key", resourceDir] 
          error: &error] autorelease];
  
  STAssertNil (key6, @"Key must be nil");
  STAssertNotNil (error, @"Error must be set");
  STAssertEquals ([error code], KEY_IO_MISSING_FIELD, @"Wrong error code");
}

- (void) testKeyRegistry
{
  NSString *resourceDir = 
    [NSString stringWithCString: getenv ("TEST_RESOURCES_DIR") 
                       encoding: NSASCIIStringEncoding];

  NSError *error = nil;
  KeyRegistry *registry =  
    [[[KeyRegistry alloc] initWithKeysInDir: 
      [NSString stringWithFormat: @"%@/test_keys", resourceDir] error: &error] 
        autorelease];
  
  STAssertNotNil (registry, @"Error reading registry: %@", 
                   [error localizedDescription]);

  STAssertEquals ([registry count], (NSUInteger)2, @"Wrong key count");                  
}

@end
