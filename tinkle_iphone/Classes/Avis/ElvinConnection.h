#import <Foundation/Foundation.h>

#import "PresenceStatus.h"
#import "TickerMessage.h"

#include <avis/elvin.h>

typedef enum {FieldStatus = 0x01, FieldBuddies = 0x02, FieldUserAgent = 0x04,
              FieldsAll = FieldStatus | FieldBuddies | FieldUserAgent} 
              PresenceFields;

extern NSString *ElvinConnectionOpenedNotification;
extern NSString *ElvinConnectionClosedNotification;
extern NSString *ElvinConnectionWillCloseNotification;

@interface ElvinConnection : NSObject 
{
  Elvin            elvin;
  NSString *       elvinUrl;
  NSMutableArray * subscriptions;
  NSThread *       eventLoopThread;
  NSArray *        keys;
  NSString *       userAgent;
}

@property (readwrite, retain) NSString *elvinUrl;

@property (readwrite, retain) NSArray *keys;

@property (readwrite, copy) NSString *userAgent;

+ (NSString *) escapedSubscriptionString: (NSString *) str;

- (id) initWithUrl: (NSString *) url;

- (void) disconnect;

- (void) connect;

- (BOOL) isConnected;

- (void) sendTickerMessage: (NSString *) messageText 
                fromSender: (NSString *) from
                   toGroup: (NSString *) group
                 inReplyTo: (TickerMessage *) replyTo 
               attachedURL: (NSURL *) url
                sendPublic: (BOOL) isPublic
              sendInsecure: (BOOL) allowInsecure;

- (void) sendPresenceRequestMessage: (NSString *) userID 
                      fromRequestor: (NSString *) requestor
                           toGroups: (NSString *) groups 
                           andUsers: (NSString *) users
                         sendPublic: (BOOL) isPublic;

- (void) sendPresenceInfoMessage: (NSString *) userID
                         forUser: (NSString *) userName
                       inReplyTo: (NSString *) inReplyTo
                      withStatus: (PresenceStatus *) status
                        toGroups: (NSString *) groups
                      andBuddies: (NSString *) buddies
                 includingFields: (PresenceFields) fields
                      sendPublic: (BOOL) isPublic;

- (id) subscribe: (NSString *) subscriptionExpr
    withDelegate: (id) delegate onNotify: (SEL) onNotify
         onError: (SEL) onError;

- (void) resubscribe: (id) subscriptionContext 
   usingSubscription: (NSString *) newSubscription;

+ (BOOL) wasReceivedSecure: (NSDictionary *) message;

@end
