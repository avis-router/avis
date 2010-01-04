#import "Tinkle_AppDelegate.h"

#import "ElvinConnection.h"
#import "PresenceConnection.h"
#import "PresenceViewController.h"
#import "MessagesViewController.h"
#import "MainWindowController.h"
#import "Preferences.h"

@implementation Tinkle_AppDelegate

@synthesize window;
@synthesize mainWindowController;
@synthesize presence;

- (id) init
{
  self = [super init];

  if (!self)
    return nil;

  registerUserDefaults ();
  
  elvin = 
    [[ElvinConnection alloc] initWithUrl: prefString (PrefElvinURL)];
    
  presence = [[[PresenceConnection alloc] initWithElvin: elvin] retain];
    
  NSString *userAgent = 
    [NSString stringWithFormat: @"%@ for iPhone (%@)",
      [[NSBundle mainBundle] objectForInfoDictionaryKey: @"CFBundleDisplayName"],
      [[NSBundle mainBundle] objectForInfoDictionaryKey: @"CFBundleShortVersionString"]];

  elvin.userAgent = userAgent;
  
  return self;
}

- (void) dealloc 
{
  [mainWindowController release];
  [window release];
  
  [self disconnect];
  
  [presence release];
  [elvin release];
  
  [super dealloc];
}

- (void) applicationDidFinishLaunching: (UIApplication *) application 
{ 
  [window addSubview: mainWindowController.view];

  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];
  
  [notifications addObserver: self selector: @selector (handleElvinOpen:)
                        name: ElvinConnectionOpenedNotification object: nil]; 
  
  [notifications addObserver: self selector: @selector (handleElvinClose:)
                        name: ElvinConnectionClosedNotification object: nil]; 

  [notifications addObserver: self selector: @selector (handleElvinDefaultsChange:)
                        name: ElvinDefaultsDidChangeNotification object: nil];
                        
  [elvin connect];
}

- (void) applicationWillTerminate: (UIApplication *) application
{
  [self disconnect];
}

- (void) disconnect
{
  [elvin disconnect];
}

- (void) handleElvinOpen: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    messagesController.elvin = elvin;
    presenceController.presence = presence;
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
    // TODO
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinClose:) 
                           withObject: nil waitUntilDone: NO];
  }
}

- (void) handleElvinDefaultsChange: (NSNotification *) ntfn
{
  elvin.elvinUrl = prefString (PrefElvinURL);
}

@end

