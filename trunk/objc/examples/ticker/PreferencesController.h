#import <Cocoa/Cocoa.h>

@interface PreferencesController : NSWindowController
{
  IBOutlet id addPresenceGroupSheet;
  IBOutlet id addPresenceGroupTextField;
  IBOutlet NSArrayController *presenceGroupsController;
}

- (IBAction) addPresenceGroup: (id) sender;
- (IBAction) closeAddPresenceGroupSheet: (id) sender;

@end
