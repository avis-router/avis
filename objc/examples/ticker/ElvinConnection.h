#import <Foundation/Foundation.h>

#include <avis/elvin.h>

extern NSString *ElvinConnectionOpenedNotification;
extern NSString *ElvinConnectionClosedNotification;

@interface ElvinConnection : NSObject 
{
  Elvin            elvin;
  NSString *       elvinUrl;
  NSMutableArray * subscriptions;
  NSThread *       eventLoopThread;
}

- (id) initWithUrl: (NSString *) url;

- (void) disconnect;

- (void) connect;

- (BOOL) isConnected;

- (void) sendTickerMessage: (NSString *) messageText 
                fromSender: (NSString *) from
                   toGroup: (NSString *) group
                 inReplyTo: (NSString *) replyToId 
               attachedURL: (NSURL *) url
                sendPublic: (BOOL) isPublic;

- (void) subscribe: (NSString *) subscriptionExpr withDelegate: (id) delegate 
         usingSelector: (SEL) handler;

@end
