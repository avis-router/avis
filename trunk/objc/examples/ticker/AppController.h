#import <Cocoa/Cocoa.h>

#include <avis/elvin.h>

@interface AppController : NSObject
{
  Elvin elvin;
}

- (void) sendMessage: (NSString *) messageText toGroup: (NSString *) group;

- (void) subscribe: (NSString *) subscription withObject: (id) object 
      usingHandler: (SEL) handler;

@end
