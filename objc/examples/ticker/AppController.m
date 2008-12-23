#import "AppController.h"
#import "TickerController.h"
#import "PreferencesController.h"

static inline NSString *prefsString (NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] stringForKey: name];
}

NSString *PreferencesContext = @"PreferencesContext";

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
    [[[ElvinConnection alloc] 
      initWithUrl: prefsString (@"ElvinURL") lifecycleDelegate: self] 
      retain];

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
}

- (void) dealloc
{
  [[[NSWorkspace sharedWorkspace] notificationCenter] removeObserver: self];
  [[NSUserDefaultsController sharedUserDefaultsController] removeObserver: self];
  
  [elvin disconnect];
  [elvin release];
  elvin = nil;

  [super dealloc];
}

/*
 * Handle preference changes.
 */
- (void) observeValueForKeyPath: (NSString *) keyPath ofObject: (id) object 
         change: (NSDictionary *) change context: (void *) context
{
  if (context == PreferencesContext)
  {
		NSLog (@"Elvin URL changed: %@", prefsString (@"ElvinURL"));
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
