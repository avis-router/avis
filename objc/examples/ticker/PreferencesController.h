#import <Cocoa/Cocoa.h>

@interface PreferencesController : NSWindowController
{
  id addPresenceGroupSheet;
  
  IBOutlet id addPresenceGroupTextField;
  IBOutlet id addPresenceGroupAddButton;
}

- (IBAction) addPresenceGroup: (id) sender;
- (IBAction) closeAddPresenceGroupSheet: (id) sender;

@end
