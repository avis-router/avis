#import "PresenceController.h"

#import "PresenceEntity.h"
#import "AppController.h"

NSString *PresenceUserWasDoubleClicked = @"PresenceUserWasDoubleClicked";

@implementation PresenceController

@synthesize appController;

- (id) initWithAppController: (AppController *) theAppController
{
  self = [super initWithWindowNibName: @"PresenceWindow"];
  
  if (self)
  {
    appController = theAppController;    
  }
  
  return self;
}

- (void) dealloc
{
  [refreshTimer invalidate];
  [refreshTimer release];
  
  [super dealloc];
}

- (void) awakeFromNib
{
  [presenceTable setDoubleAction: @selector (doubleClickHandler:)];
  
  refreshTimer = 
    [[NSTimer scheduledTimerWithTimeInterval: 10
      target: presenceTable selector: @selector (reloadData) 
      userInfo: nil repeats: YES] retain];
}

- (void) doubleClickHandler: (id) sender
{
  NSInteger row = [presenceTable clickedRow];
    
  if (row != -1)
  {
    PresenceEntity *clickedUser = 
      [[presenceTableController arrangedObjects] objectAtIndex: row];

    [[NSNotificationCenter defaultCenter] 
       postNotificationName: PresenceUserWasDoubleClicked object: self 
       userInfo: 
         [NSDictionary dictionaryWithObject: clickedUser forKey: @"user"]];
  }
}

- (NSString *) tableView: (NSTableView *) view toolTipForCell: (NSCell *) cell 
    rect: (NSRectPointer) rect tableColumn: (NSTableColumn *) column 
    row: (NSInteger) row mouseLocation: (NSPoint) mouseLocation
{
  PresenceEntity *entity = 
    [[presenceTableController arrangedObjects] objectAtIndex: row];
  
  if ([[column identifier] isEqual: @"status"])
  {
    NSDateFormatter *dateFormatter = [[NSDateFormatter new] autorelease];
    [dateFormatter setDateStyle: NSDateFormatterLongStyle];
    [dateFormatter setTimeStyle: NSDateFormatterShortStyle]; 
    
    return [NSString stringWithFormat: @"%@ since %@",
             entity.status.statusCodeAsUIString,
             [dateFormatter stringFromDate: entity.status.changedAt]];
  } else
  {
    // TODO return user info
    return nil;
  }
}

@end
