#import "Growl/GrowlApplicationBridge.h"

#import "AppController.h"
#import "ElvinConnection.h"
#import "PresenceConnection.h"
#import "TickerController.h"
#import "PresenceController.h"
#import "PreferencesController.h"

#import "utils.h"

NSString *PreferencesContext = @"PreferencesContext";

#pragma mark Declare Private Methods

@implementation AppController

@synthesize elvin;

@synthesize presence;

#pragma mark -

+ (void) initialize
{
  NSMutableDictionary *defaults = [NSMutableDictionary dictionary];

  // TODO NSHost:name sometimes blocks for 60 seconds ... why?
  NSString *defaultUserName = 
    [NSString stringWithFormat: @"%@@%@", 
     NSFullUserName (), [[NSHost currentHost] name]];
     
  [defaults setObject: @"elvin://public.elvin.org" forKey: @"ElvinURL"];
  [defaults setObject: defaultUserName forKey: @"OnlineUserName"];
  [defaults setObject: @"Chat" forKey: @"DefaultSendGroup"];

  [[NSUserDefaults standardUserDefaults] registerDefaults: defaults];
}

- (id) init
{
  self = [super init];
  
  if (self)
  {
    elvin = 
      [[[ElvinConnection alloc] initWithUrl: prefsString (@"ElvinURL")] 
        retain];
    presence = [[[PresenceConnection alloc] initWithElvin: elvin] retain];
  }
  
  return self;
}

- (void) dealloc
{
  [[[NSWorkspace sharedWorkspace] notificationCenter] removeObserver: self];
  [[NSUserDefaultsController sharedUserDefaultsController] removeObserver: self];
  
  NSLog (@"Delloc");
  
  [elvin disconnect];
  [elvin release];
  elvin = nil;
  
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
                   options: 0 context: PreferencesContext];
                   
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
    tickerController = [[TickerController alloc] initWithAppController: self];
  
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

#pragma mark -

/*
 * Handle preference changes.
 */
- (void) observeValueForKeyPath: (NSString *) keyPath ofObject: (id) object 
         change: (NSDictionary *) change context: (void *) context
{
  if (context == PreferencesContext)
  {
		NSLog (@"Elvin URL changed: %@", prefsString (@"ElvinURL"));
    
    elvin.elvinUrl = prefsString (@"ElvinURL");
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
