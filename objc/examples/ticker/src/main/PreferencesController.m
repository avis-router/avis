#import "PreferencesController.h"

#import "Preferences.h"
#import "utils.h"

static NSString *computerName ()
{
  NSDictionary *systemPrefs = 
    [NSDictionary dictionaryWithContentsOfFile: 
      @"/Library/Preferences/SystemConfiguration/preferences.plist"];
      
  NSString *computerName = 
    [systemPrefs valueForKeyPath: @"System.System.ComputerName"];
    
  return computerName ? computerName : @"sticker";
}

@interface PreferencesController (PRIVATE)
  - (void) setPrefView: (id) sender;
@end

@implementation PreferencesController

#define TOOLBAR_GENERAL     @"TOOLBAR_GENERAL"
#define TOOLBAR_TICKER      @"TOOLBAR_TICKER"
#define TOOLBAR_PRESENCE    @"TOOLBAR_PRESENCE"

+ (void) registerUserDefaults
{
  NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
  NSMutableDictionary *defaults = [NSMutableDictionary dictionary];

  // assign user a UUID if needed
  if (![preferences objectForKey: PrefOnlineUserUUID])
    [preferences setObject: uuidString () forKey: PrefOnlineUserUUID];

  if (![preferences objectForKey: PrefOnlineUserName])
  {
    [defaults setObject: [NSString stringWithFormat: @"%@@%@", 
                          NSFullUserName (), computerName ()] 
      forKey: PrefOnlineUserName];
  }
  
  [defaults setObject: @"elvin://public.elvin.org" forKey: PrefElvinURL];
  [defaults setObject: @"Chat" forKey: PrefDefaultSendGroup];  
  [defaults setObject: [NSArray arrayWithObject: @"Chat"] 
               forKey: PrefTickerGroups];
  [defaults setObject: [NSArray arrayWithObject: @"elvin"] 
            forKey: PrefPresenceGroups];
  [defaults setObject: [NSArray array] forKey: PrefPresenceBuddies];
  [defaults setObject: @"Group != 'lawley-rcvstore'" 
               forKey: PrefTickerSubscription];
  
  // presence column sorting
  NSSortDescriptor *statusDescriptor = 
    [[[NSSortDescriptor alloc] 
      initWithKey: @"status.statusCode" ascending: YES] autorelease];
  NSSortDescriptor *nameDescriptor = 
    [[[NSSortDescriptor alloc] 
      initWithKey: @"name" ascending: YES 
      selector: @selector (caseInsensitiveCompare:)] autorelease];
  
  [defaults setObject: 
    [NSArchiver archivedDataWithRootObject:
      [NSArray arrayWithObjects: statusDescriptor, nameDescriptor, nil]] 
    forKey: PrefPresenceColumnSorting]; 
  
  [preferences registerDefaults: defaults];
}

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

  [toolbar setSelectedItemIdentifier: TOOLBAR_GENERAL];
  [self setPrefView: nil];
}

- (NSToolbarItem *) toolbar: (NSToolbar *) toolbar 
                    itemForItemIdentifier: (NSString *) ident
                    willBeInsertedIntoToolbar: (BOOL) flag
{
  NSToolbarItem *item = 
    [[[NSToolbarItem alloc] initWithItemIdentifier: ident] autorelease];

  [item setTarget: self];
  [item setAction: @selector (setPrefView:)];
  [item setAutovalidates: NO];
    
  if ([ident isEqualToString: TOOLBAR_GENERAL])
  {
    [item setLabel: @"General"];
    [item setImage: [NSImage imageNamed: @"NSPreferencesGeneral"]];
  } else if ([ident isEqualToString: TOOLBAR_TICKER])
  {
    [item setLabel: @"Ticker"];
    [item setImage: [NSImage imageNamed: @"Ticker_512"]];
  } else if ([ident isEqualToString: TOOLBAR_PRESENCE])
  {
    [item setLabel: @"Presence"];
    [item setImage: [NSImage imageNamed: @"NSUserGroup"]];
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
  return [NSArray arrayWithObjects: TOOLBAR_GENERAL, TOOLBAR_TICKER,
                                    TOOLBAR_PRESENCE, nil];
}

- (void) setPrefView: (id) sender
{
  NSView *view;  
  NSString *identifier = [sender itemIdentifier];
  
  if ([identifier isEqualToString: TOOLBAR_TICKER])
    view = tickerPanel;
  else if ([identifier isEqualToString: TOOLBAR_PRESENCE])
    view = presencePanel;
  else
    view = generalPanel;

  NSWindow *window = [self window];
  
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
  if (sender)
  {
    [window setTitle: [sender label]];
  } else
  {
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