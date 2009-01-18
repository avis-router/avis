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

@synthesize appController;

@end
