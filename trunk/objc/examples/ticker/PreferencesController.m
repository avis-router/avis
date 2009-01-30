#import "PreferencesController.h"

@implementation PreferencesController

- (id) init
{
  if ([super initWithWindowNibName: @"Preferences"])
    return self;
  else
    return nil;
}

//- (void) windowDidLoad
//{
//  [presenceGroupsController setSortDescriptors: 
//    [NSArray arrayWithObject: 
//     [[NSSortDescriptor alloc] 
//        initWithKey: @"" ascending: YES 
//           selector: @selector (caseInsensitiveCompare:)]]];
//  
//  [presenceGroupsController setAutomaticallyRearrangesObjects: YES];  
//}

- (void) didEndSheet: (NSWindow *) sheet returnCode: (int) returnCode 
         contextInfo: (void *) contextInfo
{
//  NSString *group = 
//    [[addPresenceGroupTextField stringValue] 
//      stringByTrimmingCharactersInSet: 
//        [NSCharacterSet whitespaceAndNewlineCharacterSet]];
//  
//  if (returnCode == NSOKButton && [group length] > 0 &&
//      ![[presenceGroupsController arrangedObjects] containsObject: group])
//  {
//    [presenceGroupsController addObject: group];
//  }
//  
//  [sheet orderOut: self];
  
  if (returnCode == NSOKButton)
    [presenceGroupsController addObject: [addPresenceGroupTextField stringValue]];
  
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