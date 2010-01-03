#import <Foundation/Foundation.h>

#import "PresenceStatus.h"

extern NSString *PresenceStatusChangedNotification;

@class ElvinConnection;

@interface PresenceConnection : NSObject
{
  ElvinConnection    * elvin;
  NSMutableArray     * entities;
  PresenceStatus     * presenceStatus;
  id                   delegate;
  id                   presenceInfoSubscription;
  id                   presenceRequestSubscription;
}

- (id) initWithElvin: (ElvinConnection *) theElvinConnection;

- (void) refresh;

@property (readonly, assign)  IBOutlet NSMutableArray * entities;
@property (readwrite, retain) IBOutlet PresenceStatus * presenceStatus;
@property (readwrite, assign) IBOutlet id delegate;

@end
