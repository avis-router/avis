#import "PresenceGroupsController.h"

@implementation PresenceGroupsController

- (void) awakeFromNib
{
  [self setSortDescriptors: 
    [NSArray arrayWithObject: 
      [[NSSortDescriptor alloc] 
         initWithKey: @"" ascending: YES 
         selector: @selector (caseInsensitiveCompare:)]]];
  
  [self setAutomaticallyRearrangesObjects: YES];
}

- (BOOL) containsGroup: (NSString *) group
{
  for (NSString *g in [self arrangedObjects])
  {
    if ([g caseInsensitiveCompare: group] == 0)
      return YES;
  }
  
  return NO;
}

- (void) addObject: (id) object
{
  NSString *group = 
    [object stringByTrimmingCharactersInSet: 
      [NSCharacterSet whitespaceAndNewlineCharacterSet]];
  
  if ([group length] > 0 && ![self containsGroup: group])
    [super addObject: group];
}

@end
