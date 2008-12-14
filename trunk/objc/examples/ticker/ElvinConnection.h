#import <Cocoa/Cocoa.h>

#include <avis/elvin.h>

@interface ElvinConnection : NSObject 
{
  Elvin elvin;
  NSString *elvinUrl;
  id lifecycleDelegate;
  NSMutableArray *subscriptions;
  NSThread *eventLoopThread;
}

- (id) initWithUrl: (NSString *) url lifecycleDelegate: (id) delegate;

- (void) disconnect;

- (void) connect;

- (void) sendTickerMessage: (NSString *) messageText 
         toGroup: (NSString *) group inReplyTo: (NSString *) replyToId;

- (void) subscribe: (NSString *) subscriptionExpr withObject: (id) object 
         usingHandler: (SEL) handler;

@end
