#import <Cocoa/Cocoa.h>

#include <avis/elvin.h>

@interface TickerController : NSObject
{
  IBOutlet id text;

  Elvin elvin;
}

@end
