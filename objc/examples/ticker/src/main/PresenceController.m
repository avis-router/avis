#import "PresenceController.h"

#import "AppController.h"

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
  NSLog (@"Clicked %i", [presenceTable clickedRow]);
}

@end
