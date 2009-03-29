#import <Foundation/Foundation.h>

@interface Key : NSObject 
{
}

- (id) initWithFile: (NSString *) file error: (NSError **) error;

- (id) initWithString: (NSString *) text error: (NSError **) error;

@end
