#import "PreferencesController.h"

#import "Preferences.h"
#import "utils.h"

@interface PreferencesController (PRIVATE)
  - (void) setPrefView: (id) sender;
@end

@implementation PreferencesController

#define TOOLBAR_MESSAGING   @"TOOLBAR_MESSAGING"
#define TOOLBAR_PRESENCE    @"TOOLBAR_PRESENCE"
#define TOOLBAR_ADVANCED    @"TOOLBAR_ADVANCED"

- (id) init
{
  if ([super initWithWindowNibName: @"Preferences"])
    return self;
  else
    return nil;
}

- (void) awakeFromNib
{
  NSToolbar *toolbar = 
    [[[NSToolbar alloc] initWithIdentifier: @"Preferences Toolbar"] autorelease];
    
  [toolbar setDelegate: self];
  [toolbar setAllowsUserCustomization: NO];
  [toolbar setDisplayMode: NSToolbarDisplayModeIconAndLabel];
  [toolbar setSizeMode: NSToolbarSizeModeRegular];
  [[self window] setToolbar: toolbar];

  [toolbar setSelectedItemIdentifier: TOOLBAR_MESSAGING];
  [self setPrefView: nil];
}

- (NSToolbarItem *) toolbar: (NSToolbar *) toolbar 
                    itemForItemIdentifier: (NSString *) ident
                    willBeInsertedIntoToolbar: (BOOL) flag
{
  NSToolbarItem * item = 
    [[[NSToolbarItem alloc] initWithItemIdentifier: ident] autorelease];

  [item setTarget: self];
  [item setAction: @selector (setPrefView:)];
  [item setAutovalidates: NO];

  if ([ident isEqualToString: TOOLBAR_MESSAGING])
  {
    [item setLabel: @"Messaging"];
    [item setImage: [NSImage imageNamed: @"Tinkle_512"]];
  } else if ([ident isEqualToString: TOOLBAR_PRESENCE])
  {
    [item setLabel: @"Presence"];
    [item setImage: [NSImage imageNamed: @"NSUser"]];
  } else if ([ident isEqualToString: TOOLBAR_ADVANCED])
  {
    [item setLabel: @"Advanced"];
    [item setImage: [NSImage imageNamed: @"NSPreferencesGeneral"]];
  } else
  {
    return nil;
  }

  return item;
}

- (NSArray *) toolbarSelectableItemIdentifiers: (NSToolbar *) toolbar
{
  return [self toolbarDefaultItemIdentifiers: toolbar];
}

- (NSArray *) toolbarDefaultItemIdentifiers: (NSToolbar *) toolbar
{
  return [self toolbarAllowedItemIdentifiers: toolbar];
}

- (NSArray *) toolbarAllowedItemIdentifiers: (NSToolbar *) toolbar
{
  return [NSArray arrayWithObjects: 
           TOOLBAR_MESSAGING, TOOLBAR_PRESENCE, TOOLBAR_ADVANCED, nil];
}

- (void) setPrefView: (id) sender
{
  NSWindow *window = [self window];
  NSString *identifier = [[window toolbar] selectedItemIdentifier];
  NSView *view;  
  
  if ([identifier isEqualToString: TOOLBAR_ADVANCED])
    view = advancedPanel;
  else if ([identifier isEqualToString: TOOLBAR_PRESENCE])
    view = presencePanel;
  else
    view = messagingPanel;

  if ([window contentView] == view)
    return;

  NSRect windowRect = [window frame];
  CGFloat difference = 
    ([view frame].size.height - [[window contentView] frame].size.height) * 
      [window userSpaceScaleFactor];
      
  windowRect.origin.y -= difference;
  windowRect.size.height += difference;

  [view setHidden: YES];
  [window setContentView: view];
  [window setFrame: windowRect display: YES animate: YES];
  [view setHidden: NO];

  // set title label
  NSToolbar * toolbar = [window toolbar];
  NSString * itemIdentifier = [toolbar selectedItemIdentifier];
  
  for (NSToolbarItem * item in [toolbar items])
  {
    if ([[item itemIdentifier] isEqualToString: itemIdentifier])
    {
      [window setTitle: [item label]];
      break;
    }
  }
}

- (void) showAdvancedTab
{
  [[[self window] toolbar] setSelectedItemIdentifier: TOOLBAR_ADVANCED];
  
  [self setPrefView: self];
}

#pragma mark Add presence group stuff

- (IBAction) addPresenceGroup: (id) sender
{
  [NSApp beginSheet: presenceGroupAddSheet
     modalForWindow: [self window]
      modalDelegate: self
     didEndSelector: @selector (addPresenceGroupSheetDidEnd:returnCode:contextInfo:)
        contextInfo: nil];
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

#pragma mark Add ticker group stuff

- (IBAction) addTickerGroup: (id) sender
{
  [NSApp beginSheet: tickerGroupAddSheet
     modalForWindow: [self window]
      modalDelegate: self
     didEndSelector: @selector (addTickerGroupSheetDidEnd:returnCode:contextInfo:)
        contextInfo: nil];
}

- (IBAction) closeTickerGroupAddSheet: (id) sender
{
  [NSApp endSheet: tickerGroupAddSheet returnCode: [sender tag]];
}

- (void) addTickerGroupSheetDidEnd: (NSWindow *) sheet 
                        returnCode: (int) returnCode 
                       contextInfo: (void *) contextInfo
{
  if (returnCode == NSOKButton)
    [tickerGroupsController addObject: [addTickerGroupTextField stringValue]];
  
  [sheet orderOut: self];
}

@end