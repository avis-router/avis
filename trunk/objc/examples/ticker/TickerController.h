#import <Cocoa/Cocoa.h>

#include <avis/elvin.h>

@interface TickerController : NSObject
{
  IBOutlet id text;

  Elvin elvin;
  
  CFMessagePortRef remoteCocoaPort;
}

- (void) applicationWillTerminate: (NSNotification *)notification;

- (void) awakeFromNib;

- (void) elvinEventLoopThread: (NSObject *)object;

@end
