#import <Foundation/Foundation.h>

typedef enum {KEY_TYPE_PUBLIC, KEY_TYPE_PRIVATE} KeyAccessType;

typedef enum {KEY_IO_VERSION, KEY_IO_ACCESS, KEY_IO_UNKNOWN_FIELD, 
              KEY_IO_MISSING_VALUE, KEY_IO_BAD_HEX_DATA, 
              KEY_IO_MISSING_FIELD} KeyIOError;

@interface ElvinKey : NSObject 
{
  KeyAccessType type;
  NSString *name;
  NSData *data;
}

@property (readwrite) KeyAccessType type;

@property (readwrite, copy) NSString * name;

@property (readwrite, copy) NSData * data;

- (id) initWithFile: (NSString *) file error: (NSError **) error;

- (id) initWithString: (NSString *) text error: (NSError **) error;

@end
