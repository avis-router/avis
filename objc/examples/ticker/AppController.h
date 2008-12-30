#import <Cocoa/Cocoa.h>

@class ElvinConnection;
@class PreferencesController;

@interface AppController : NSObject
{
  IBOutlet id tickerWindow;
  
  ElvinConnection       * elvin;
  PreferencesController * preferencesController;
}

@property (readonly) ElvinConnection *elvin;

- (IBAction) showTickerWindow: (id) sender;

- (IBAction) showPreferencesWindow: (id) sender;

@end
