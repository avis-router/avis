#import "TextViewLinkifier.h"

@interface TextViewLinkifier ()
  - (void) updateTrackingAreas;
  - (void) handleTrackingUpdate: (void *) unused;
@end

@implementation TextViewLinkifier

@dynamic view;

- (NSTextView *) view
{
  return view;
}

- (void) setView: (NSTextView *) theView
{
  [view release];
  
  view = [theView retain];
  
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];

  [notifications addObserver: self selector: @selector (handleTrackingUpdate:)
    name: NSViewFrameDidChangeNotification object: view];
  
  [notifications addObserver: self selector: @selector (handleTrackingUpdate:)
    name: NSTextViewDidChangeTypingAttributesNotification object: view];

  [self updateTrackingAreas];
}

- (void) handleTrackingUpdate: (void *) unused
{
  [self updateTrackingAreas];
}

- (NSCursor *) cursorForLink: (NSObject *) linkObject
    atIndex: (unsigned) charIndex
{
  NSCursor *result = nil;

  //  If the delegate implements the method, consult it.
//  if ([[self delegate] respondsToSelector: @selector (cursorForLink:atIndex:ofTextView:)])
//    result = [[self delegate] cursorForLink: linkObject atIndex: charIndex ofTextView: self];

  //  If the delegate didn't implement it, or it did but returned nil, substitute a guess.
  if (result == nil)
//      result = [[self class] fingerCursor];
    result = [NSCursor pointingHandCursor];

  return result;
}

- (void) updateTrackingAreas
{
  for (NSTrackingArea *area in view.trackingAreas)
  {
    [view removeTrackingArea: area];
    [area release];
  }
  
  [view discardCursorRects];
  
  NSAttributedString  *attrString;
  NSPoint             containerOrigin;
  NSRect              visRect;
  NSRange              visibleGlyphRange, visibleCharRange, attrsRange;

  //  Get the attributed text inside us
  attrString = [view textStorage];

  //  Figure what part of us is visible (we're typically inside a scrollview)
  containerOrigin = [view textContainerOrigin];
  visRect = NSOffsetRect ([view visibleRect], -containerOrigin.x, -containerOrigin.y);

  //  Figure the range of characters which is visible
  visibleGlyphRange = [[view layoutManager] glyphRangeForBoundingRect:visRect inTextContainer:[view textContainer]];
  visibleCharRange = [[view layoutManager] characterRangeForGlyphRange:visibleGlyphRange actualGlyphRange:NULL];

  //  Prime for the loop
  attrsRange = NSMakeRange (visibleCharRange.location, 0);

  //  Loop until we reach the end of the visible range of characters
  while (NSMaxRange(attrsRange) < NSMaxRange(visibleCharRange)) // find all visible URLs and set up cursor rects
  {
    NSString *linkObject;

    //  Find the next link inside the range
    linkObject = [attrString attribute: NSLinkAttributeName 
        atIndex: NSMaxRange(attrsRange)
        effectiveRange: &attrsRange];

    if (linkObject != nil)
    {
      NSCursor    *cursor;
      NSRectArray    rects;
      unsigned int  rectCount, rectIndex;
      NSRect      oneRect;

      //  Figure what cursor to show over this link.
      cursor = [self cursorForLink: linkObject  atIndex: attrsRange.location];

      //  Find the rectangles where this range falls. (We could use -boundingRectForGlyphRange:...,
      //  but that gives a single rectangle, which might be overly large when a link runs
      //  through more than one line.)
      rects = [[view layoutManager] rectArrayForCharacterRange: attrsRange
          withinSelectedCharacterRange: NSMakeRange (NSNotFound, 0)
          inTextContainer: [view textContainer]
          rectCount: &rectCount];

      //  For each rectangle, find its visible portion and ask for the cursor to appear
      //  when they're over that rectangle.
      for (rectIndex = 0; rectIndex < rectCount; rectIndex++)
      {
        oneRect = NSIntersectionRect (rects[rectIndex], [view visibleRect]);
        
        //[self addCursorRect: oneRect  cursor: cursor];
        NSTrackingArea *area = [[NSTrackingArea alloc] initWithRect: oneRect
          options: (NSTrackingMouseEnteredAndExited | NSTrackingActiveInKeyWindow | NSTrackingCursorUpdate)
          owner: self userInfo: nil];
        
        [view addTrackingArea: area]; 
      }
    }
  }
}

- (void) mouseEntered: (NSEvent *) event
{
  NSLog (@"mouse enter");
  [[NSCursor pointingHandCursor] push];
//  NSPoint eyeCenter = [self convertPoint:[theEvent locationInWindow] fromView:nil];
//  eyeBox = NSMakeRect((eyeCenter.x-10.0), (eyeCenter.y-10.0), 20.0, 20.0);
//  [self setNeedsDisplayInRect:eyeBox];
//  [self displayIfNeeded];
}

- (void) mouseExited: (NSEvent *) event
{
  NSLog (@"mouse exit");
  [[NSCursor pointingHandCursor] pop];
}

- (void) cursorUpdate: (NSEvent *) event
{
  NSLog (@"cursor");
//  [[NSCursor pointingHandCursor] set];
}

@end
