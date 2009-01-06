#import "RolloverButton.h"

@implementation RolloverButton

@synthesize rolloverImage;

- (void) updateTrackingAreas
{
  [self setRollover: NO];
  
  for (NSTrackingArea *area in [self trackingAreas])
  {
    if ([area owner] == self)
    {
      [self removeTrackingArea: area];
      [area release];
    }
  }
  
  NSRect frame = [self frame];
  NSRect rect = NSMakeRect (0, 0, frame.size.width, frame.size.height);
  
  NSTrackingArea *area = 
    [[NSTrackingArea alloc] initWithRect: rect 
      options: (NSTrackingMouseEnteredAndExited | NSTrackingActiveInKeyWindow)
      owner: self userInfo: nil];
  
  [self addTrackingArea: area];
}

- (void) setRollover: (BOOL) rolledOver
{
  if (rolledOver)
  {
    if (originalImage == nil)
    {
      originalImage = [[self image] retain];
    
      [self setImage: rolloverImage];    
    }
  } else
  {
    if (originalImage != nil)
    {
      [self setImage: originalImage];
      
      [originalImage release];
      originalImage = nil;
    }
  }
}

- (void) mouseEntered: (NSEvent *) event
{
  [self setRollover: YES];
}

- (void) mouseExited: (NSEvent *) event
{
  [self setRollover: NO];
}

@end
