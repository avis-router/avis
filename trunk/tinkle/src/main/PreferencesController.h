#import <Cocoa/Cocoa.h>

@interface PreferencesController : NSWindowController
{
  IBOutlet id presenceGroupAddSheet;
  IBOutlet id addPresenceGroupTextField;
  IBOutlet id tickerGroupAddSheet;
  IBOutlet id addTickerGroupTextField;
  IBOutlet id messagingPanel;
  IBOutlet id presencePanel;
  IBOutlet id advancedPanel;
  IBOutlet id addPresenceGroupSheet;

  IBOutlet NSArrayController *presenceGroupsController;
  IBOutlet NSArrayController *tickerGroupsController;
}

+ (void) registerUserDefaults;

- (IBAction) addPresenceGroup: (id) sender;
- (IBAction) closePresenceGroupAddSheet: (id) sender;

- (IBAction) addTickerGroup: (id) sender;
- (IBAction) closeTickerGroupAddSheet: (id) sender;

@end
