#import <Cocoa/Cocoa.h>

@interface PreferencesController : NSWindowController
{
  id addPresenceGroupSheet;
  
  IBOutlet id addPresenceGroupTextField;
  IBOutlet id addPresenceGroupAddButton;
  IBOutlet NSArrayController *presenceGroupsController;
}

- (IBAction) addPresenceGroup: (id) sender;
- (IBAction) closeAddPresenceGroupSheet: (id) sender;

@end
