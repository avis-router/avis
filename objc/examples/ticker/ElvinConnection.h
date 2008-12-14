#import <Cocoa/Cocoa.h>

#include <avis/elvin.h>

@interface ElvinConnection : NSObject 
{
  Elvin elvin;
  NSString *elvinUrl;
  id lifecycleDelegate;
}

- (id) initWithUrl: (NSString *) url lifecycleDelegate: (id) delegate;

- (void) close;

- (void) sendTickerMessage: (NSString *) messageText 
         toGroup: (NSString *) group inReplyTo: (NSString *) replyToId;

- (void) subscribe: (NSString *) subscription withObject: (id) object 
         usingHandler: (SEL) handler;

@end
