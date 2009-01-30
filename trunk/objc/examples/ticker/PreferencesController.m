#import "PreferencesController.h"

@implementation PreferencesController

- (id) init
{
  if ([super initWithWindowNibName: @"Preferences"])
    return self;
  else
    return nil;
}

- (void) addPresenceGroupSheetDidEnd: (NSWindow *) sheet 
                          returnCode: (int) returnCode 
                        contextInfo: (void *) contextInfo
{
  if (returnCode == NSOKButton)
  {
    [presenceGroupsController addObject: 
      [addPresenceGroupTextField stringValue]];
  }
  
  [sheet orderOut: self];
  
}

- (IBAction) closeAddPresenceGroupSheet: (id) sender
{
  [NSApp endSheet: addPresenceGroupSheet
       returnCode: (sender == addPresenceGroupAddButton ? 
                      NSOKButton : NSCancelButton)];
}

- (IBAction) addPresenceGroup: (id) sender
{
  if (!addPresenceGroupSheet)
    [NSBundle loadNibNamed: @"Preferences_AddPresenceGroup" owner: self];

  [NSApp beginSheet: addPresenceGroupSheet
     modalForWindow: [self window]
      modalDelegate: self
     didEndSelector: @selector (addPresenceGroupSheetDidEnd:returnCode:contextInfo:)
        contextInfo: nil];
}

@end