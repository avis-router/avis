#import "TickerMessagesTextView.h"

@implementation TickerMessagesTextView

- (BOOL) validateMenuItem: (NSMenuItem *) item
{
  SEL action = [item action];
  
  if (action == @selector (delete:) || action == @selector (cut:)) 
    return [self selectedRange].length > 0;
  else
    return [super validateMenuItem: item];
}

- (void) delete: (id) sender
{
  [self setEditable: YES];
  [super delete: sender];
  [self setEditable: NO];
}

- (void) cut: (id) sender
{
  [self setEditable: YES];
  [super cut: sender];
  [self setEditable: NO];
}

@end
