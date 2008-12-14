#import "AppController.h"
#import "TickerController.h"

@implementation AppController

@synthesize elvin;

- (void) applicationDidFinishLaunching: (NSNotification *) notification 
{
  elvin = 
    [[[ElvinConnection alloc] initWithUrl: @"elvin://elvin" 
                              lifecycleDelegate: self] retain];
}

- (void) applicationWillTerminate: (NSNotification *) notification
{
  [elvin close];
  
  [elvin release];
  elvin = nil;
}

/*
 * Handles delegate call from Elvin connection.
 */
- (void) elvinConnectionDidOpen: (ElvinConnection *) connection
{
  [self openTickerWindow];
}

- (void) openTickerWindow
{
  // TODO this would leak the TickerController object
  // possibly due to circular ref?

  NSLog (@"Open Ticker Window");
  
  if (![NSBundle loadNibNamed: @"TickerWindow" owner: self])
  {
    NSLog (@"No nib file for ticker?");
    return;
  }
  
  [tickerWindow makeKeyAndOrderFront: nil];
}

@end
