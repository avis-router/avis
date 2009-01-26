#import <Foundation/Foundation.h>

#import "PresenceStatus.h"

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
}

@property (readwrite, retain) NSString *elvinUrl;

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
                        andUsers: (NSString *) users
                   fromUserAgent: (NSString * ) userAgent
                 includingFields: (PresenceFields) fields
                      sendPublic: (BOOL) isPublic;

- (void) subscribe: (NSString *) subscriptionExpr withDelegate: (id) delegate 
         usingSelector: (SEL) handler;

@end
