#import "RolloverButton.h"

@implementation RolloverButton

+ (void) initialize
{
  [self exposeBinding: @"rollover"];
}
   
- (void) updateTrackingAreas
{
  self.rollover = NO;
  
  for (NSTrackingArea *area in [self trackingAreas])
  {
    if ([area owner] == self)
      [self removeTrackingArea: area];
  }
  
  NSRect frame = [self frame];
  NSRect rect = NSMakeRect (0, 0, frame.size.width, frame.size.height);
  
  NSTrackingArea *area = 
    [[NSTrackingArea alloc] initWithRect: rect 
      options: (NSTrackingMouseEnteredAndExited | NSTrackingActiveInKeyWindow)
      owner: self userInfo: nil];
  
  [self addTrackingArea: area];
  
  [area release];
}

- (void) setRollover: (BOOL) rolledOver
{
  if ([self alternateImage] == nil)
    return;
  
  if (rolledOver)
  {
    if (originalImage == nil)
    {
      originalImage = [[self image] retain];
    
      [self setImage: [self alternateImage]];    
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

- (BOOL) rollover
{
  return originalImage != nil;
}

- (void) mouseEntered: (NSEvent *) event
{
  self.rollover = YES;
}

- (void) mouseExited: (NSEvent *) event
{
  self.rollover = NO;
}

@end
