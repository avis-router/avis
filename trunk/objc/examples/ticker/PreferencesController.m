#import "PreferencesController.h"

@implementation PreferencesController

- (id) init
{
  if ([super initWithWindowNibName: @"Preferences"])
    return self;
  else
    return nil;
}

- (void)didEndSheet: (NSWindow *) sheet returnCode: (int) returnCode 
        contextInfo: (void *) contextInfo
{
  NSLog (@"Code = %u", returnCode);
  
  //if (returnCode == NSOKButton)
//  {
//    NSArray *
//  }
  
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
     didEndSelector: @selector (didEndSheet:returnCode:contextInfo:)
        contextInfo: nil];
}

@end