#import <Cocoa/Cocoa.h>

@class ElvinConnection;
@class PreferencesController;

@interface AppController : NSObject
{
  IBOutlet id tickerWindow;
  
  ElvinConnection       * elvin;
  PreferencesController * preferencesController;
}

@property (readonly, assign) ElvinConnection *elvin;

- (IBAction) showPreferencesWindow: (id) sender;

@end
