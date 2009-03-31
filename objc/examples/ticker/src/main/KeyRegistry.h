#import <Foundation/Foundation.h>

typedef enum {REG_ERROR_MISSING_DIR} RegError;

@class ElvinKey;

@interface KeyRegistry : NSObject
{
  NSMutableArray *keys;
}

- (id) initWithKeysInDir: (NSString *) dir error: (NSError **) error;

@property (readonly) NSUInteger count;

@property (readonly) NSArray *keys;

@end
