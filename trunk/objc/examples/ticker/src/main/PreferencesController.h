#import <Cocoa/Cocoa.h>

@interface PreferencesController : NSWindowController
{
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
