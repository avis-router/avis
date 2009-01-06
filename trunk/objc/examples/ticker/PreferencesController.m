#import "PreferencesController.h"

@implementation PreferencesController

- (id) init
{
  if ([super initWithWindowNibName: @"Preferences"])
    return self;
  else
    return nil;
}

@end
