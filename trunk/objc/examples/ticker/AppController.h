#import <Cocoa/Cocoa.h>

#include <avis/elvin.h>

@interface AppController : NSObject
{
  IBOutlet id tickerWindow;
  IBOutlet id messageWindow;
  
  Elvin elvin;
}

- (void) sendMessage: (NSString *) messageText toGroup: (NSString *) group
           inReplyTo: (NSString *) replyToId;

- (void) subscribe: (NSString *) subscription withObject: (id) object 
      usingHandler: (SEL) handler;

@end
