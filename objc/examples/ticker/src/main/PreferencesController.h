#import <Cocoa/Cocoa.h>

@class KeyRegistry;

@interface PreferencesController : NSWindowController
{
  KeyRegistry *keys;
  
  IBOutlet id presenceGroupAddSheet;
  IBOutlet id addPresenceGroupTextField;
  IBOutlet id generalPanel;
  IBOutlet id tickerPanel;
  IBOutlet id presencePanel;
  IBOutlet id addPresenceGroupSheet;
  IBOutlet NSArrayController *presenceGroupsController;
}

+ (void) registerUserDefaults;

- (IBAction) addPresenceGroup: (id) sender;
- (IBAction) closePresenceGroupAddSheet: (id) sender;

@end
