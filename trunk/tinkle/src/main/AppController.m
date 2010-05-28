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

static void observe (id observer, NSUserDefaultsController *prefs, 
                     NSString *property);

void observe (id observer, NSUserDefaultsController *prefs, NSString *property)
{
  [prefs addObserver: observer 
          forKeyPath: [NSString stringWithFormat: @"values.%@", property]
             options: 0 context: observer];
}

#pragma mark Declare Private Methods

@interface AppController (Private)
  - (void) handleElvinStatusChange: (NSString *) status;
  - (void) handleTickerMessage: (NSNotification *) notification;
  - (void) handlePresenceChange: (NSNotification *) notification;
  - (void) registerForPresenceChangesAfterDelay;
  - (void) unregisterForPresenceChanges;
  - (NSString *) createTickerSubscription;
  - (void) showNotConnectedDockBadgeAfterDelay;
  - (BOOL) isTickerActive;
  - (BOOL) shouldMuteGrowl;
  - (void) handleAppActivated: (NSNotification *) notification;
  - (void) incrementUnreadMessageCount;
  - (void) clearUnreadMessageCount;
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
    
    presence = 
      [[[PresenceConnection alloc]
        initWithElvin: elvin
        userId: prefString (PrefOnlineUserUUID) 
        userName: prefString (PrefOnlineUserName)
        groups: prefArray (PrefPresenceGroups)
        buddies: prefArray (PrefPresenceBuddies)] retain];
    
    elvin.keys = prefArray (@"Keys");
    
    NSString *userAgent = 
      [NSString stringWithFormat: @"%@ (%@)",
        [[NSBundle mainBundle] objectForInfoDictionaryKey: @"CFBundleName"],
        [[NSBundle mainBundle] objectForInfoDictionaryKey: @"CFBundleShortVersionString"]];
    
    elvin.userAgent = userAgent;
    
    tickerEditCount = 0;
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
  [GrowlApplicationBridge setGrowlDelegate: self];

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

  // application activated
  [notifications addObserver: self selector: @selector (handleAppActivated:)
                        name: NSApplicationDidBecomeActiveNotification object: nil]; 

  // listen for ticker messages
  [notifications addObserver: self selector: @selector (handleTickerMessage:)
                        name: TickerMessageReceivedNotification object: nil];

  // listen for ticker message edit start/stop
  [notifications addObserver: self selector: @selector (handleTickerEditStart:)
                        name: TickerMessageStartedEditingNotification 
                      object: nil];

  [notifications addObserver: self selector: @selector (handleTickerEditStop:)
                        name: TickerMessageStoppedEditingNotification 
                      object: nil];
  
  // listen for preference changes
  NSUserDefaultsController *userPreferences = 
    [NSUserDefaultsController sharedUserDefaultsController];

  observe (self, userPreferences, PrefElvinURL);
  observe (self, userPreferences, PrefTickerSubscription);
  observe (self, userPreferences, PrefTickerGroups);
  observe (self, userPreferences, PrefElvinKeys);
  observe (self, userPreferences, PrefOnlineUserName);
  observe (self, userPreferences, PrefPresenceGroups);
  observe (self, userPreferences, PrefShowUnreadMessageCount);
  
  [self connect];
  
  [[NSApp dockTile] setContentView: dockTile];
  [[NSApp dockTile] display];
  
  if (prefBool (PrefShowPresenceWindow))
    [self showPresenceWindow: self];
}

- (void) applicationWillTerminate: (NSNotification *) notification 
{
  [self disconnect];
  
  [[NSNotificationCenter defaultCenter] removeObserver: self];
  [NSObject cancelPreviousPerformRequestsWithTarget: self];
  
  BOOL presenceVisible =
    presenceController.window && [presenceController.window isVisible];
  
  [[NSUserDefaults standardUserDefaults] 
    setBool: presenceVisible forKey: PrefShowPresenceWindow];
}

/*
 * Handle preference changes.
 */
- (void) observeValueForKeyPath: (NSString *) keyPath ofObject: (id) object 
                         change: (NSDictionary *) change
                        context: (void *) context
{
  if (context == self)
  {
    if ([keyPath hasSuffix: PrefElvinURL])
    {
      elvin.elvinUrl = prefString (PrefElvinURL);
    } else if ([keyPath hasSuffix: PrefTickerSubscription] ||
               [keyPath hasSuffix: PrefTickerGroups])
    {
      tickerController.subscription = [self createTickerSubscription];
    } else if ([keyPath hasSuffix: PrefElvinKeys])
    {
      elvin.keys = prefArray (PrefElvinKeys);
    } else if ([keyPath hasSuffix: PrefOnlineUserName])
    {
      presence.userName = prefString (PrefOnlineUserName);
    } else if ([keyPath hasSuffix: PrefPresenceGroups])
    {
      presence.groups = prefArray (PrefPresenceGroups);
    } else if ([keyPath hasSuffix: PrefShowUnreadMessageCount])
    {
      [self clearUnreadMessageCount];
    }
  } else 
  {
    [super observeValueForKeyPath: keyPath ofObject: object change: change 
                          context: context];
  }
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

- (void) connect
{
  [elvin connect];
  
  [self showNotConnectedDockBadgeAfterDelay];
}

- (void) disconnect
{
  [elvin disconnect];
  
  [NSObject cancelPreviousPerformRequestsWithTarget: self 
    selector: @selector (showNotConnectedDockBadge) object: nil];
}

- (void) showNotConnectedDockBadgeAfterDelay
{
  [self performSelector: @selector (showNotConnectedDockBadge) 
             withObject: nil afterDelay: 5];
}

- (void) showNotConnectedDockBadge
{
  [warningBadge setHidden: NO];
  [[NSApp dockTile] display];
}

- (void) hideNotConnectedDockBadge
{
  [warningBadge setHidden: YES];
  [[NSApp dockTile] display];
  
  [NSObject cancelPreviousPerformRequestsWithTarget: self 
    selector: @selector (showNotConnectedDockBadge) object: nil];
}

- (NSString *) createTickerSubscription
{
  NSArray *groups = prefArray (PrefTickerGroups);
  NSString *subscription = trim (prefString (PrefTickerSubscription));
  NSString *user = 
    [ElvinConnection escapedSubscriptionString: prefString (PrefOnlineUserName)];
  
  NSMutableString *fullSubscription = 
    [NSMutableString stringWithString: 
       @"(string (Message) && string (From) && string (Group))"];
  
  // user's messages
  
  [fullSubscription appendFormat: 
     @" && ((From == '%@' || Group == '%@' || Thread-Id == '%@')",
     user, user, user];
  
  // groups
  
  if ([groups count] > 0 || [subscription length] > 0)
    [fullSubscription appendString: @" || ("];
  
  if ([groups count] > 0)
  {
    [fullSubscription appendString: @"equals (Group"];
    
    for (NSString *group in groups)
      [fullSubscription appendFormat: @", '%@'",
         [ElvinConnection escapedSubscriptionString: group]];
    
    [fullSubscription appendString: @")"];
  }
  
  // extra subscription
  
  if ([subscription length] > 0)
  {
    if ([groups count] > 0)
      [fullSubscription appendString: @" || "];
  
    [fullSubscription appendFormat: @"(%@)", subscription];
  }
  
  if ([groups count] > 0 || [subscription length] > 0)
    [fullSubscription appendString: @")"];
  
  [fullSubscription appendString: @")"];
  
  return fullSubscription;
}

- (IBAction) showTickerWindow: (id) sender
{
  if (!tickerController)
  {
    tickerController = 
      [[TickerController alloc] initWithElvin: elvin 
         subscription: [self createTickerSubscription]];
         
    // listen for activations
    [[NSNotificationCenter defaultCenter] 
        addObserver: self
          selector: @selector (handleAppActivated:)
          name: NSWindowDidBecomeKeyNotification 
          object: [tickerController window]];
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

- (IBAction) presenceSetOnline: (id) sender
{
  presence.presenceStatus = [PresenceStatus onlineStatus];
}

- (IBAction) presenceSetAway: (id) sender
{
  presence.presenceStatus = [PresenceStatus awayStatus];
}

- (IBAction) presenceSetCoffee: (id) sender
{
  presence.presenceStatus = [PresenceStatus coffeeStatus];
}

- (IBAction) presenceSetDoNotDisturb: (id) sender
{
  presence.presenceStatus = [PresenceStatus doNotDisturbStatus];
}

/**
 * Set the selected state of a presence status menu item to reflect the 
 * current status.
 */
- (BOOL) setPresenceItemSelected: (NSMenuItem *) item
                          status: (PresenceStatus *) status
{
  [item setState: 
    [presence.presenceStatus isEqual: status] ? NSOnState : NSOffState];
  
  return YES;
}

- (BOOL) validateMenuItem: (NSMenuItem *) item
{
  SEL action = [item action];
  
  if (action == @selector (presenceSetOnline:))
  {
    return [self setPresenceItemSelected: item 
                                  status: [PresenceStatus onlineStatus]];
  } else if (action == @selector (presenceSetAway:))
  {
    return [self setPresenceItemSelected: item 
                                  status: [PresenceStatus awayStatus]];
  } else if (action == @selector (presenceSetCoffee:))
  {
    return [self setPresenceItemSelected: item 
                                  status: [PresenceStatus coffeeStatus]];
  } else if (action == @selector (presenceSetDoNotDisturb:))
  {
    return [self setPresenceItemSelected: item 
                                  status: [PresenceStatus doNotDisturbStatus]];
  } else
  {
    return YES;
  }
}

#pragma mark -

- (void) handleSleep: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    NSLog (@"Disconnect on sleep");
    
    [self disconnect];
  } else
  {
    [self performSelectorOnMainThread: @selector (handleSleep:) 
                           withObject: nil waitUntilDone: YES];
  }
}

- (void) handleWake: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    NSLog (@"Reconnect on wake");
    
    [self connect];
  } else
  {
    [self performSelectorOnMainThread: @selector (handleWake:) 
                           withObject: nil waitUntilDone: NO];
  }
}

- (void) handleElvinOpen: (NSNotification *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    [self handleElvinStatusChange: @"connected"];
    
    [self registerForPresenceChangesAfterDelay];
    
    [self hideNotConnectedDockBadge];
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinOpen:) 
                           withObject: nil waitUntilDone: NO];
  }
}

- (void) handleElvinClose: (NSNotification *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    [self handleElvinStatusChange: @"disconnected"];
    
    [self unregisterForPresenceChanges];
   
    [self showNotConnectedDockBadgeAfterDelay];
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinClose:) 
                           withObject: nil waitUntilDone: NO];
  }
}

/**
 * True if the app is active and the ticker window is visible.
 */
- (BOOL) isTickerActive
{
  return tickerController && [[tickerController window] isVisible] &&
         [[NSApplication sharedApplication] isActive];
}

- (BOOL) shouldMuteGrowl
{
  return presence.presenceStatus.statusCode == UNAVAILABLE &&
    [presence.presenceStatus isEqual: [PresenceStatus doNotDisturbStatus]];
}

/**
 * Handle app and ticker activatation: clear unread count.
 */
- (void) handleAppActivated: (NSNotification *) notification
{
  if ([self isTickerActive])
    [self clearUnreadMessageCount];
}

- (void) incrementUnreadMessageCount
{
  if (prefBool (PrefShowUnreadMessageCount) == YES)
  {
    unreadMessages++;
    
    [[NSApp dockTile] setBadgeLabel: 
      [NSString stringWithFormat: @"%i", unreadMessages]];
  }
}

- (void) clearUnreadMessageCount
{
  unreadMessages = 0;
  
  [[NSApp dockTile] setBadgeLabel: nil];
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
  if (![self isTickerActive])
    [self incrementUnreadMessageCount];
  
  // no growling when in DND mode
  if ([self shouldMuteGrowl])
    return;
  
  TickerMessage *message = [[notification userInfo] valueForKey: @"message"];
  
  NSString *description =
    [NSString stringWithFormat: @"%@: %@", message->from, message->message];
  
  // Growl should really handle long messages sensibly...
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
    iconData: nil priority: priority isSticky: sticky clickContext: @"message"];
}

- (void) handlePresenceChange: (NSNotification *) notification
{
  // no growling when in DND mode
  if ([self shouldMuteGrowl])
    return;
  
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
    iconData: [[NSImage imageNamed: @"NSUser"] TIFFRepresentation] 
    priority: 0 isSticky: NO clickContext: @"presence_status"];
}

- (void) growlNotificationWasClicked: (id) context
{
  if ([context isEqual: @"message"])
    [self showTickerWindow: self];
  else if ([context isEqual: @"presence_status"])
    [self showPresenceWindow: self];
  
  [self clearUnreadMessageCount];
}

- (void) registerForPresenceChangesAfterDelay
{
  [self unregisterForPresenceChanges];
  
  [self performSelector: @selector (registerForPresenceChanges) 
    withObject: nil afterDelay: 10];
}

- (void) registerForPresenceChanges
{
  [self unregisterForPresenceChanges];
  
  [[NSNotificationCenter defaultCenter] addObserver: self 
    selector: @selector (handlePresenceChange:) 
    name: PresenceStatusChangedNotification object: nil];
}

- (void) unregisterForPresenceChanges
{
  [NSObject cancelPreviousPerformRequestsWithTarget: self 
    selector: @selector (registerForPresenceChanges) object: nil];
  
  [[NSNotificationCenter defaultCenter] removeObserver: self 
    name: PresenceStatusChangedNotification object: nil];
}

- (void) handleTickerEditStart: (NSNotification *) notification
{
  if (++tickerEditCount == 1)
  {
    if (presence.presenceStatus.statusCode != UNAVAILABLE)
    {
      PresenceStatus *composingStatus = [PresenceStatus composingStatus];
      
      composingStatus.changedAt = presence.presenceStatus.changedAt;
      
      presence.presenceStatus = composingStatus;
    }
  }
}

- (void) handleTickerEditStop: (NSNotification *) notification
{    
  if (tickerEditCount > 0 && --tickerEditCount == 0)
  {
    if ([presence.presenceStatus isEqual: [PresenceStatus composingStatus]])
    {
      PresenceStatus *onlineStatus = [PresenceStatus onlineStatus];
      
      onlineStatus.changedAt = presence.presenceStatus.changedAt;
      
      presence.presenceStatus = onlineStatus; 
    }
  }
}

@end
