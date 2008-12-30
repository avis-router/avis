#import <Cocoa/Cocoa.h>

@class ElvinConnection;
@class TickerController;
@class PreferencesController;

@interface AppController : NSObject
{
  ElvinConnection *       elvin;
  TickerController *      tickerController;
  PreferencesController * preferencesController;
}

@property (readonly) ElvinConnection *elvin;

- (IBAction) showTickerWindow: (id) sender;

- (IBAction) showPreferencesWindow: (id) sender;

@end
