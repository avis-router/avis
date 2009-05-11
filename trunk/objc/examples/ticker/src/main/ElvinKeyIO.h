#import <Foundation/Foundation.h>

extern NSString *KeyFieldName;
extern NSString *KeyFieldIsPrivate;
extern NSString *KeyFieldData;

typedef enum {KEY_IO_VERSION, KEY_IO_ACCESS, KEY_IO_UNKNOWN_FIELD, 
              KEY_IO_MISSING_VALUE, KEY_IO_BAD_HEX_DATA, 
              KEY_IO_MISSING_FIELD, KEY_IO_DUPLICATE_KEY} KeyIOError;

@interface ElvinKeyIO : NSObject
{
}

+ (NSDictionary *) keyFromFile: (NSString *) file error: (NSError **) error;

+ (NSDictionary *) keyFromString: (NSString *) contents error: (NSError **) error;

+ (NSString *) stringFromKey: (NSDictionary *) key;

@end
