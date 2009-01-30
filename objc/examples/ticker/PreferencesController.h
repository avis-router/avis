#import <Cocoa/Cocoa.h>

@interface PreferencesController : NSWindowController
{
  IBOutlet id presenceGroupAddSheet;
  IBOutlet id addPresenceGroupTextField;
  IBOutlet NSArrayController *presenceGroupsController;
}

- (IBAction) addPresenceGroup: (id) sender;
- (IBAction) closePresenceGroupAddSheet: (id) sender;

@end
