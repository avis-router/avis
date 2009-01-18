#import <Foundation/Foundation.h>

@class ElvinConnection;

@interface PresenceConnection : NSObject
{
  ElvinConnection * elvin;
  NSMutableArray *  entities;
}

- (id) initWithElvin: (ElvinConnection *) theElvinConnection;

@property (readonly, assign) IBOutlet NSArray * entities;

@end
