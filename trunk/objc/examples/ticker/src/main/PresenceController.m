#import "PresenceController.h"

#import "PresenceEntity.h"
#import "AppController.h"

NSString *PresenceUserWasDoubleClicked = @"PresenceUserWasDoubleClicked";

@implementation PresenceController

- (id) initWithAppController: (AppController *) theAppController
{
  self = [super initWithWindowNibName: @"PresenceWindow"];
  
  if (self)
  {
    appController = theAppController;    
  }
  
  return self;
}

- (void) awakeFromNib
{
  [presenceTable setDoubleAction: @selector (doubleClickHandler:)];
}

@synthesize appController;

- (void) doubleClickHandler: (id) sender
{
  NSInteger row = [presenceTable clickedRow];
  
  PresenceEntity *clickedUser = (row == -1) ? 
    nil : [[presenceTableController arrangedObjects] objectAtIndex: row];
  
  [[NSNotificationCenter defaultCenter] 
     postNotificationName: PresenceUserWasDoubleClicked object: self 
     userInfo: 
       [NSDictionary dictionaryWithObject: clickedUser forKey: @"user"]];
}

@end
