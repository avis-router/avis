#import "TextViewLinkifier.h"

/**
 * See http://developer.apple.com/samplecode/TextLinks/listing5.html for 
 * the starting point for this class.
 */
 
@interface TextViewLinkifier ()
  - (void) updateTrackingAreas;
  - (void) handleTrackingUpdate: (void *) unused;
  - (void) underline: (NSRange) range underlined: (BOOL) isUnderlined;
@end

@implementation TextViewLinkifier

@dynamic view;

- (void) dealloc
{
  [view release];
  
  [[NSNotificationCenter defaultCenter] removeObserver: self];
  
  [super dealloc];
}

- (NSTextView *) view
{
  return view;
}

- (void) setView: (NSTextView *) theView
{
  [view release];
  
  view = [theView retain];
  
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];

  [notifications removeObserver: self];
  
  // track scrolling
  [notifications addObserver: self selector: @selector (handleTrackingUpdate:)
    name: NSViewBoundsDidChangeNotification 
    object: [[view enclosingScrollView] contentView]];
      
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

  if (result == nil)
    result = [NSCursor pointingHandCursor];

  return result;
}

- (void) updateTrackingAreas
{
  // clear old rects
  for (NSTrackingArea *area in view.trackingAreas)
  {
    [view removeTrackingArea: area];
    [area release];
  }
  
  NSAttributedString *attrString = [view textStorage];

  // Figure what part of us is visible (we're typically inside a scrollview)
  NSPoint containerOrigin = [view textContainerOrigin];
  NSRect visibleRect = 
    NSOffsetRect ([view visibleRect], -containerOrigin.x, -containerOrigin.y);

  // Figure the range of characters which is visible
  NSRange visibleGlyphRange = 
    [[view layoutManager] glyphRangeForBoundingRect: visibleRect 
                          inTextContainer: [view textContainer]];
  NSRange visibleCharRange = 
    [[view layoutManager] characterRangeForGlyphRange: visibleGlyphRange 
                          actualGlyphRange: NULL];

  // Prime for the loop
  NSRange attrsRange = NSMakeRange (visibleCharRange.location, 0);

  // Loop until we reach the end of the visible range of characters
  // find all visible URLs and set up cursor rects
  while (NSMaxRange (attrsRange) < NSMaxRange (visibleCharRange)) 
  {
    // Find the next link inside the range
    NSString *linkObject = 
      [attrString attribute: NSLinkAttributeName 
        atIndex: NSMaxRange (attrsRange)
        longestEffectiveRange: &attrsRange inRange: visibleCharRange];

    if (linkObject != nil)
    {
      NSUInteger rectCount;

      // Find the rectangles where this range falls. (We could use 
      // -boundingRectForGlyphRange:..., but that gives a single rectangle, 
      // which might be overly large when a link runs through more than one 
      // line.)
      NSRectArray rects = 
        [[view layoutManager] rectArrayForCharacterRange: attrsRange
          withinSelectedCharacterRange: NSMakeRange (NSNotFound, 0)
          inTextContainer: [view textContainer]
          rectCount: &rectCount];

      NSDictionary *userInfo = 
        [NSDictionary dictionaryWithObject:
         [NSValue valueWithRange: attrsRange] forKey: @"range"];
         
      // For each rectangle, find its visible portion
      for (NSUInteger rectIndex = 0; rectIndex < rectCount; rectIndex++)
      {
        NSRect rect = 
          NSIntersectionRect (rects [rectIndex], [view visibleRect]);
        
        NSTrackingArea *area = 
          [[NSTrackingArea alloc] initWithRect: rect
            options: (NSTrackingMouseEnteredAndExited | 
                      NSTrackingActiveInKeyWindow | NSTrackingCursorUpdate)
            owner: self userInfo: userInfo];
        
        [view addTrackingArea: area]; 
      }
    }
  }
}

- (void) mouseEntered: (NSEvent *) event
{
  [[NSCursor pointingHandCursor] push];
  
  NSValue *range = [(NSDictionary *)[event userData] valueForKey: @"range"];
  
  [self underline: [range rangeValue] underlined: YES];
}

- (void) mouseExited: (NSEvent *) event
{
  [[NSCursor pointingHandCursor] pop];
  
  NSValue *range = [(NSDictionary *)[event userData] valueForKey: @"range"];
  
  [self underline: [range rangeValue] underlined: NO];
}

- (void) underline: (NSRange) range underlined: (BOOL) isUnderlined
{
  NSDictionary *linkAttributes = 
    [NSDictionary dictionaryWithObject: [NSNumber numberWithBool: isUnderlined] 
      forKey: NSUnderlineStyleAttributeName];
  
  [[view textStorage] addAttributes: linkAttributes range: range];
}

@end
