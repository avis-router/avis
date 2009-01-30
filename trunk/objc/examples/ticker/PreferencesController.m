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

- (IBAction) closePresenceGroupAddSheet: (id) sender
{
  [NSApp endSheet: presenceGroupAddSheet returnCode: [sender tag]];
}

- (IBAction) addPresenceGroup: (id) sender
{
  if (!presenceGroupAddSheet)
    [NSBundle loadNibNamed: @"Preferences_AddPresenceGroup" owner: self];

  [NSApp beginSheet: presenceGroupAddSheet
     modalForWindow: [self window]
      modalDelegate: self
     didEndSelector: @selector (addPresenceGroupSheetDidEnd:returnCode:contextInfo:)
        contextInfo: nil];
}

@end