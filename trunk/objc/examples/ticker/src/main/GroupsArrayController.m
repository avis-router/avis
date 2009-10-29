#import "GroupsArrayController.h"

#import "utils.h"

@implementation GroupsArrayController

- (void) awakeFromNib
{
  [self setSortDescriptors: 
    [NSArray arrayWithObject: 
      [[NSSortDescriptor alloc] 
         initWithKey: @"" ascending: YES 
         selector: @selector (localizedCaseInsensitiveCompare:)]]];
  
  [self setAutomaticallyRearrangesObjects: YES];
}

- (BOOL) containsGroup: (NSString *) group
{
  for (NSString *g in [self arrangedObjects])
  {
    if ([g localizedCaseInsensitiveCompare: group] == NSOrderedSame)
      return YES;
  }
  
  return NO;
}

- (void) addObject: (id) object
{
  NSString *group = trim (object);
  
  if ([group length] > 0 && ![self containsGroup: group])
    [super addObject: group];
}

@end
