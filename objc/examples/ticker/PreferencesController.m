#import "PreferencesController.h"

const NSString *PrefOnlineUserName = @"OnlineUserName";

@implementation PreferencesController

- (id) init
{
  if ([super initWithWindowNibName: @"Preferences"])
    return self;
  else
    return nil;
}

@end
