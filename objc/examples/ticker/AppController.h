#import <Cocoa/Cocoa.h>

#import "ElvinConnection.h"

#include <avis/elvin.h>

@interface AppController : NSObject
{
  IBOutlet id tickerWindow;
  IBOutlet id messageWindow;
  
  ElvinConnection *elvin;
}

- (void) openTickerWindow;

@property (readonly, assign) ElvinConnection *elvin;

@end
