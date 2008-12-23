#import "AppController.h"
#import "TickerController.h"
#import "PreferencesController.h"

#pragma mark Declare Private Methods

@interface AppController ()
  - (void) openTickerWindow;
@end

@implementation AppController

#pragma mark Public methods

@synthesize elvin;

- (IBAction) showPreferencesWindow: (id) sender
{
  if (!preferencesController)
    preferencesController = [PreferencesController new];
    
  [preferencesController showWindow: self];
}

#pragma mark Private methods

+ (void) initialize
{
  NSMutableDictionary *defaultValues = [NSMutableDictionary dictionary];

  [defaultValues setObject: @"elvin://public.elvin.org" forKey: @"ElvinURL"];

  [[NSUserDefaults standardUserDefaults] registerDefaults: defaultValues];
}

- (void) applicationDidFinishLaunching: (NSNotification *) notification 
{
  elvin = 
    [[[ElvinConnection alloc] initWithUrl: @"elvin://elvin" 
                              lifecycleDelegate: self] retain];

  NSNotificationCenter *notificationCenter = 
    [[NSWorkspace sharedWorkspace] notificationCenter];

  [notificationCenter addObserver: self selector: @selector (handleWake:)
    name: NSWorkspaceDidWakeNotification object: nil]; 
   
  [notificationCenter addObserver: self selector: @selector (handleSleep:)
    name: NSWorkspaceWillPowerOffNotification object: nil]; 
   
  [notificationCenter addObserver: self selector: @selector (handleSleep:)
    name: NSWorkspaceWillSleepNotification object: nil]; 
}

- (void) handleSleep: (void *) unused
{
  NSLog (@"Disconnect on sleep");
  
  [elvin disconnect];
}

- (void) handleWake: (void *) unused
{
  NSLog (@"Reconnect on wake");
  
  [elvin connect];
}

- (void) applicationWillTerminate: (NSNotification *) notification
{
  [elvin disconnect];
  [elvin release];
  elvin = nil;
}

/*
 * Handles delegate call from Elvin connection.
 */
- (void) elvinConnectionDidOpen: (ElvinConnection *) connection
{
  if (tickerWindow == nil)
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
