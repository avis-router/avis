#import <Cocoa/Cocoa.h>

#include <avis/elvin.h>

@interface ElvinConnection : NSObject 
{
  NSString * elvinUrl;
  id         lifecycleDelegate;
  Elvin      elvin;
}

- (id) initWithUrl: (NSString *) url lifecycleDelegate: (id) delegate;

- (void) close;

- (void) sendMessage: (NSString *) messageText toGroup: (NSString *) group
           inReplyTo: (NSString *) replyToId;

- (void) subscribe: (NSString *) subscription withObject: (id) object 
      usingHandler: (SEL) handler;

@end
