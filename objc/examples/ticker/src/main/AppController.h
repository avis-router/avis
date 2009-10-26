#import <Cocoa/Cocoa.h>

@class ElvinConnection;
@class PresenceConnection;
@class TickerController;
@class PresenceController;
@class PreferencesController;

@interface AppController : NSObject
{
  ElvinConnection *       elvin;
  PresenceConnection *    presence;
  TickerController *      tickerController;
  PresenceController *    presenceController;
  PreferencesController * preferencesController;
}

@property (readonly) ElvinConnection *    elvin;

@property (readonly) PresenceConnection * presence;

- (IBAction) showTickerWindow: (id) userAgent;

- (IBAction) showPreferencesWindow: (id) sender;

- (IBAction) showPresenceWindow: (id) sender;

- (IBAction) refreshPresence: (id) sender;

- (IBAction) presenceSetOnline: (id) sender;

- (IBAction) presenceSetAway: (id) sender;

@end
