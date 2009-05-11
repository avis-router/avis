#import "Growl/GrowlApplicationBridge.h"

#import "AppController.h"
#import "ElvinConnection.h"
#import "PresenceConnection.h"
#import "TickerController.h"
#import "PresenceController.h"
#import "PreferencesController.h"
#import "Preferences.h"

#import "utils.h"

#pragma mark Declare Private Methods

@implementation AppController

@synthesize elvin;

@synthesize presence;

#pragma mark -

+ (void) initialize
{
  [PreferencesController registerUserDefaults];
}

- (id) init
{
  self = [super init];
  
  if (self)
  {
    elvin = 
      [[[ElvinConnection alloc] initWithUrl: prefString (PrefElvinURL)] 
        retain];
    
    presence = [[[PresenceConnection alloc] initWithElvin: elvin] retain];
    
    elvin.keys = prefArray (@"Keys");
    
    NSString *userAgent = 
      [NSString stringWithFormat: @"%@ (%@)",
        [[NSBundle mainBundle] objectForInfoDictionaryKey: @"CFBundleName"],
        [[NSBundle mainBundle] objectForInfoDictionaryKey: @"CFBundleShortVersionString"],
        [[NSBundle mainBundle] objectForInfoDictionaryKey: @"CFBundleVersion"]];
    
    elvin.userAgent = userAgent;
  }

  return self;
}

- (void) dealloc
{
  [[[NSWorkspace sharedWorkspace] notificationCenter] removeObserver: self];
  [[NSUserDefaultsController sharedUserDefaultsController] removeObserver: self];
  
  [elvin disconnect];
  
  [presence release];
  [elvin release];

  [super dealloc];
}

#pragma mark -

- (void) applicationDidFinishLaunching: (NSNotification *) notification 
{
  [GrowlApplicationBridge setGrowlDelegate: @""];
    
  // listen for sleep/wake
  NSNotificationCenter *workspaceNotifications = 
    [[NSWorkspace sharedWorkspace] notificationCenter];

  [workspaceNotifications addObserver: self selector: @selector (handleWake:)
    name: NSWorkspaceDidWakeNotification object: nil]; 
   
  [workspaceNotifications addObserver: self selector: @selector (handleSleep:)
    name: NSWorkspaceWillPowerOffNotification object: nil]; 
   
  [workspaceNotifications addObserver: self selector: @selector (handleSleep:)
    name: NSWorkspaceWillSleepNotification object: nil]; 
  
  // listen for preference changes
  NSUserDefaultsController *userPreferences = 
    [NSUserDefaultsController sharedUserDefaultsController];
		
  [userPreferences addObserver: self forKeyPath: @"values.ElvinURL" 
                   options: 0 context: self];
  [userPreferences addObserver: self forKeyPath: @"values.TickerSubscription" 
                       options: 0 context: self];
                   
  [elvin connect];
}

- (void) applicationWillTerminate: (NSNotification *) notification 
{
  [elvin disconnect];
}

/**
 * Re-open ticker window on app re-activate.
 */
- (BOOL) applicationOpenUntitledFile: (NSApplication *) application
{
  [self showTickerWindow: self];
  
  return YES;
}

#pragma mark -

- (IBAction) showTickerWindow: (id) sender
{
  if (!tickerController)
  {
    tickerController = 
      [[TickerController alloc] initWithElvin: elvin 
        subscription: trim (prefString (PrefTickerSubscription))];
  }
  
  [tickerController showWindow: self];
}

- (IBAction) showPresenceWindow: (id) sender
{
  if (!presenceController)
    presenceController = [[PresenceController alloc] initWithAppController: self];
  
  [presenceController showWindow: self];
}

- (IBAction) showPreferencesWindow: (id) sender
{
  if (!preferencesController)
    preferencesController = [PreferencesController new];
  
  [preferencesController showWindow: self];
}

- (IBAction) refreshPresence: (id) sender
{
  [presence refresh];
}

#pragma mark -

/*
 * Handle preference changes.
 */
- (void) observeValueForKeyPath: (NSString *) keyPath ofObject: (id) object 
         change: (NSDictionary *) change context: (void *) context
{
  // TODO ticker controller should handle pref change OR bind prefs to properties
  if (context == self)
  {
    if ([keyPath hasSuffix: PrefElvinURL])
      elvin.elvinUrl = prefString (PrefElvinURL);
    else if ([keyPath hasSuffix: PrefTickerSubscription])
      tickerController.subscription = prefString (PrefTickerSubscription);
  } else 
  {
    [super observeValueForKeyPath: keyPath ofObject: object change: change 
           context: context];
  }
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

@end
