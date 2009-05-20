#import <Foundation/Foundation.h>

@interface TickerMessage : NSObject
{
  @public
  
  NSString * messageId;
  NSString * from;
  NSString * message;
  NSString * group;
  NSString * userAgent;
  NSURL    * url;
  BOOL       public;
  BOOL       secure;
  NSDate   * receivedAt;
}

+ (TickerMessage *) messageForNotification: (NSDictionary *) notification;

@end
