#import <Cocoa/Cocoa.h>

#import "ElvinConnection.h"

@class PreferencesController;

@interface AppController : NSObject
{
  IBOutlet id tickerWindow;
  IBOutlet id messageWindow;
  
  ElvinConnection       * elvin;
  PreferencesController * preferencesController;
}

@property (readonly, assign) ElvinConnection *elvin;

- (IBAction) showPreferencesWindow: (id) sender;

@end
