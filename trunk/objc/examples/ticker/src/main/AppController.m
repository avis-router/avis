#import "Growl/GrowlApplicationBridge.h"

#import "AppController.h"
#import "ElvinConnection.h"
#import "PresenceConnection.h"
#import "TickerController.h"
#import "TickerMessage.h"
#import "PresenceEntity.h"
#import "PresenceController.h"
#import "PreferencesController.h"
#import "Preferences.h"

#import "utils.h"

#define MAX_GROWL_MESSAGE_LENGTH 200

#pragma mark Declare Private Methods

@interface AppController (Private)
  - (void) handleElvinStatusChange: (NSString *) status;
  - (void) handleTickerMessage: (NSNotification *) notification;
  - (void) handlePresenceChange: (NSNotification *) notification;
  - (void) registerForPresenceChangesAfterDelay;
@end

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
        [[NSBundle mainBundle] objectForInfoDictionaryKey: @"CFBundleShortVersionString"]];
    
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

  // listen for elvin open/close
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];
  
  [notifications addObserver: self selector: @selector (handleElvinOpen:)
                        name: ElvinConnectionOpenedNotification object: nil]; 
  
  [notifications addObserver: self selector: @selector (handleElvinClose:)
                        name: ElvinConnectionClosedNotification object: nil]; 
  
  // listen for ticker messages
  [notifications addObserver: self selector: @selector (handleTickerMessage:)
                        name: TickerMessageReceivedNotification object: nil];

  // listen for presence changes
  [self registerForPresenceChangesAfterDelay];
  
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
  
  [[NSNotificationCenter defaultCenter] removeObserver: self];
  [NSObject cancelPreviousPerformRequestsWithTarget: self];
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
  [self registerForPresenceChangesAfterDelay];
  
  [presence refresh];
}

- (void) registerForPresenceChangesAfterDelay
{
  [NSObject cancelPreviousPerformRequestsWithTarget: self 
    selector: @selector (registerForPresenceChanges) object: nil];
  
  [[NSNotificationCenter defaultCenter] removeObserver: self 
    name: PresenceStatusChangedNotification object: nil];
  
  [self performSelector: @selector (registerForPresenceChanges) 
    withObject: nil afterDelay: 10];
}

- (void) registerForPresenceChanges
{
  [[NSNotificationCenter defaultCenter] addObserver: self 
    selector: @selector (handlePresenceChange:) 
    name: PresenceStatusChangedNotification object: nil];
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

- (void) handleElvinOpen: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    [self handleElvinStatusChange: @"connected"];
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinOpen:) 
                           withObject: nil waitUntilDone: NO];
  }
}

- (void) handleElvinClose: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    [self handleElvinStatusChange: @"disconnected"];
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinClose:) 
                           withObject: nil waitUntilDone: NO];
  }
}

- (void) handleElvinStatusChange: (NSString *) status
{
  NSString *message = 
    [NSString stringWithFormat: @"Elvin %@ (%@)", status, [elvin elvinUrl]];
  
  [GrowlApplicationBridge
    notifyWithTitle: @"Elvin Connection"
    description: message  
    notificationName: @"Connection Status"
    iconData: nil priority: 1 isSticky: NO clickContext: nil];
}

- (void) handleTickerMessage: (NSNotification *) notification
{
  TickerMessage *message = [[notification userInfo] valueForKey: @"message"];
  
  NSString *description =
    [NSString stringWithFormat: @"%@: %@", message->from, message->message];
  
  // Growl should really handle long messages...
  if ([description length] > MAX_GROWL_MESSAGE_LENGTH)
  {
    description = 
      [NSString stringWithFormat: @"%@...", 
        [description substringToIndex: MAX_GROWL_MESSAGE_LENGTH]];
  }
  
  const NSString *userName = prefString (PrefOnlineUserName);
  NSString *type;
  int priority = 0;
  BOOL sticky = NO;
  
  if ([message->group isEqual: userName])
  {
    type = @"Personal Message";
    
    if (![message->from isEqual: userName])
    {
      priority = 1;
      sticky = YES;
    }
  } else
  {
    type = @"Ticker Message";
  }
  
  [GrowlApplicationBridge
    notifyWithTitle: type
    description: description notificationName: type
    iconData: nil priority: priority isSticky: sticky clickContext: nil];
}

- (void) handlePresenceChange: (NSNotification *) notification
{
  PresenceEntity *user = [[notification userInfo] valueForKey: @"user"];
  
  NSString *statusCode = user.status.statusCodeAsUIString;
  NSString *statusText = user.status.statusText;
  NSString *description;
  
  if ([statusCode isEqual: statusText])
  {
    description = [NSString stringWithFormat: @"%@ is now %@", 
                   user.name, statusText];
  } else
  {
    description = [NSString stringWithFormat: @"%@ is now %@ (%@)", 
      user.name, statusCode, statusText];
  }
  
  [GrowlApplicationBridge
    notifyWithTitle: @"Status Changed"
    description: description notificationName: @"Presence Status"
    iconData: nil priority: 0 isSticky: NO clickContext: nil];
}

@end
