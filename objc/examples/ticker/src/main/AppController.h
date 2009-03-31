#import <Cocoa/Cocoa.h>

@class ElvinConnection;
@class PresenceConnection;
@class TickerController;
@class PresenceController;
@class PreferencesController;
@class KeyRegistry;

@interface AppController : NSObject
{
  ElvinConnection *       elvin;
  PresenceConnection *    presence;
  TickerController *      tickerController;
  PresenceController *    presenceController;
  PreferencesController * preferencesController;
  KeyRegistry *           keys;
}

@property (readonly) ElvinConnection *    elvin;

@property (readonly) PresenceConnection * presence;

@property (readonly) KeyRegistry *keys;

- (IBAction) showTickerWindow: (id) sender;

- (IBAction) showPreferencesWindow: (id) sender;

- (IBAction) showPresenceWindow: (id) sender;

- (IBAction) refreshPresence: (id) sender;

@end
