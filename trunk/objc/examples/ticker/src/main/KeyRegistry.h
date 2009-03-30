#import <Foundation/Foundation.h>

typedef enum {REG_ERROR_MISSING_DIR} RegError;

@class Key;

@interface KeyRegistry : NSObject
{
  NSMutableArray *keys;
}

- (id) initWithKeysInDir: (NSString *) dir error: (NSError **) error;

- (NSUInteger) count;

@end
