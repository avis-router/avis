#import "AppController.h"
#import "ElvinConnection.h"
#import "TickerController.h"
#import "PreferencesController.h"

#import "utils.h"

NSString *PreferencesContext = @"PreferencesContext";

#pragma mark Declare Private Methods

@interface AppController ()
  - (void) initElvin;
  - (void) deallocElvin;
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
  NSMutableDictionary *defaults = [NSMutableDictionary dictionary];

  NSString *defaultUserName = 
    [NSString stringWithFormat: @"%@@%@", 
     NSFullUserName (), [[NSProcessInfo processInfo] hostName]];
     
  [defaults setObject: @"elvin://public.elvin.org" forKey: @"ElvinURL"];
  [defaults setObject: defaultUserName forKey: @"OnlineUserName"];
  [defaults setObject: @"Chat" forKey: @"DefaultSendGroup"];

  [[NSUserDefaults standardUserDefaults] registerDefaults: defaults];
}

- (void) dealloc
{
  [[[NSWorkspace sharedWorkspace] notificationCenter] removeObserver: self];
  [[NSUserDefaultsController sharedUserDefaultsController] removeObserver: self];
  
  [self deallocElvin];
  
  [super dealloc];
}

- (void) initElvin
{
 if (!elvin)
 {
   elvin = 
    [[[ElvinConnection alloc] initWithUrl: prefsString (@"ElvinURL")] 
      retain];
  }
}

- (void) deallocElvin
{
  if (elvin)
  {
    [elvin disconnect];
    [elvin release];
    elvin = nil;
  }
}

- (void) applicationDidFinishLaunching: (NSNotification *) notification 
{
  [self initElvin];

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
                   
  [self showTickerWindow: self];
  
  [elvin connect];
}

- (void) applicationWillTerminate: (NSNotification *) notification 
{
  [self deallocElvin];
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

- (IBAction) showTickerWindow: (id) sender
{
  // TODO this would leak the TickerController object
  // possibly due to circular ref?

  if (!tickerWindow)
  {
    if (![NSBundle loadNibNamed: @"TickerWindow" owner: self])
    {
      NSLog (@"No nib file for ticker?");
      return;
    }
  }
  
  [tickerWindow makeKeyAndOrderFront: nil];
}

@end
