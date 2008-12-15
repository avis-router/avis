#import <Foundation/Foundation.h>

#include <avis/elvin.h>

@interface ElvinConnection : NSObject 
{
  Elvin            elvin;
  NSString *       elvinUrl;
  NSMutableArray * subscriptions;
  NSThread *       eventLoopThread;
  id               lifecycleDelegate;
}

- (id) initWithUrl: (NSString *) url lifecycleDelegate: (id) delegate;

- (void) disconnect;

- (void) connect;

- (void) sendTickerMessage: (NSString *) messageText toGroup: (NSString *) group
                 inReplyTo: (NSString *) replyToId sendPublic: (BOOL) isPublic;

- (void) subscribe: (NSString *) subscriptionExpr withDelegate: (id) delegate 
         usingSelector: (SEL) handler;

@end
