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

- (IBAction) showPreferencesWindow: (id) sender;

@property (readonly, assign) ElvinConnection *elvin;

@end
