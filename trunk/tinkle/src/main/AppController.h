#import <Cocoa/Cocoa.h>

#import "Growl/GrowlApplicationBridge.h"

@class ElvinConnection;
@class PresenceConnection;
@class TickerController;
@class PresenceController;
@class PreferencesController;

@interface AppController : NSObject <GrowlApplicationBridgeDelegate>
{
  ElvinConnection *       elvin;
  PresenceConnection *    presence;
  TickerController *      tickerController;
  PresenceController *    presenceController;
  PreferencesController * preferencesController;
  
  NSInteger               tickerEditCount;
  NSInteger               unreadMessages;
  
  IBOutlet NSView *       dockTile;
  IBOutlet NSImageView *  warningBadge;
}

@property (readonly) ElvinConnection *    elvin;

@property (readonly) PresenceConnection * presence;

- (void) connect;

- (void) disconnect;

- (IBAction) showTickerWindow: (id) userAgent;

- (IBAction) showPreferencesWindow: (id) sender;

- (IBAction) showPresenceWindow: (id) sender;

- (IBAction) refreshPresence: (id) sender;

- (IBAction) presenceSetOnline: (id) sender;

- (IBAction) presenceSetAway: (id) sender;

- (IBAction) presenceSetCoffee: (id) sender;

- (IBAction) presenceSetDoNotDisturb: (id) sender;

@end