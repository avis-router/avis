#import "TextViewWithLinks.h"

/**
 * See http://developer.apple.com/samplecode/TextLinks/listing5.html for 
 * the starting point for this class.
 */

@interface TextViewWithLinks (PRIVATE)
  - (void) updateTrackingAreas;
  - (void) handleTrackingUpdate: (void *) unused;
  - (void) setUnderlinedRange: (NSValue *) range;
  - (void) underline: (NSRange) range underlined: (BOOL) isUnderlined;
@end

@implementation TextViewWithLinks

- (void) awakeFromNib
{
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];

  // track scrolling
  [notifications addObserver: self selector: @selector (handleTrackingUpdate:)
    name: NSViewBoundsDidChangeNotification 
    object: [[self enclosingScrollView] contentView]];
  
  [notifications addObserver: self selector: @selector (handleTrackingUpdate:)
    name: NSViewFrameDidChangeNotification object: self];
  
  [notifications addObserver: self selector: @selector (handleTrackingUpdate:)
    name: NSTextViewDidChangeTypingAttributesNotification object: self];
}

- (void) dealloc
{
  [[NSNotificationCenter defaultCenter] removeObserver: self];
  
  [super dealloc];
}

- (void) handleTrackingUpdate: (void *) unused
{
  [self setUnderlinedRange: nil];
  
  if (![self inLiveResize])
    [self updateTrackingAreas];
}

- (void) updateTrackingAreas
{
  [self setUnderlinedRange: nil];
  
  // clear old rects
  for (NSTrackingArea *area in self.trackingAreas)
    [self removeTrackingArea: area];
  
  [self removeAllToolTips];
  
  NSAttributedString *attrString = [self textStorage];

  // Figure what part of us is visible (we're typically inside a scrollview)
  NSPoint containerOrigin = [self textContainerOrigin];
  NSRect visibleRect = 
    NSOffsetRect ([self visibleRect], -containerOrigin.x, -containerOrigin.y);

  // Figure the range of characters which is visible
  NSRange visibleGlyphRange = 
    [[self layoutManager] glyphRangeForBoundingRect: visibleRect 
                          inTextContainer: [self textContainer]];
  NSRange visibleCharRange = 
    [[self layoutManager] characterRangeForGlyphRange: visibleGlyphRange 
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
        [[self layoutManager] rectArrayForCharacterRange: attrsRange
          withinSelectedCharacterRange: NSMakeRange (NSNotFound, 0)
          inTextContainer: [self textContainer]
          rectCount: &rectCount];

      NSDictionary *userInfo = 
        [NSDictionary dictionaryWithObject:
         [NSValue valueWithRange: attrsRange] forKey: @"range"];
         
      // For each rectangle, find its visible portion
      for (NSUInteger rectIndex = 0; rectIndex < rectCount; rectIndex++)
      {
        NSRect rect = 
          NSIntersectionRect (rects [rectIndex], [self visibleRect]);
        
        // create a tracking area and and tooltip
        NSTrackingArea *area = 
          [[NSTrackingArea alloc] initWithRect: rect
            options: (NSTrackingMouseEnteredAndExited | 
                      NSTrackingActiveInKeyWindow | NSTrackingCursorUpdate)
            owner: self userInfo: userInfo];
        
        [self addTrackingArea: area];
        [self addToolTipRect: rect owner: self userData: linkObject];
        
        [area release];
      }
    }
  }
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

- (NSString *) toolTipForLink: (id) link view: (NSTextView *) view
{
  return [link description];
}

- (NSString *) view: (NSView *) view stringForToolTip: (NSToolTipTag) tag 
              point: (NSPoint) point userData: (void *) userData
{
  if ([[self delegate] respondsToSelector: @selector (toolTipForLink:view:)])
    return [[self delegate] toolTipForLink: (id) userData view: self];
  else
    return [self toolTipForLink: (id) userData view: self];
}

- (void) cursorUpdate: (NSEvent *) event
{
  NSPoint hitPoint = 
    [self convertPoint: [event locationInWindow] fromView: nil];
  
  if ([self mouse: hitPoint inRect: [[event trackingArea] rect]]) 
    [[NSCursor pointingHandCursor] set];
  else
    [[NSCursor IBeamCursor] set];
}

- (void) setUnderlinedRange: (NSValue *) range
{
  if (underlinedRange)
  {
    [self underline: [underlinedRange rangeValue] underlined: NO];
  
    [underlinedRange release];
  }
  
  underlinedRange = [range retain];
  
  if (underlinedRange)
    [self underline: [range rangeValue] underlined: YES];
}

- (void) mouseEntered: (NSEvent *) event
{
  [self setUnderlinedRange: 
    [(NSDictionary *)[event userData] valueForKey: @"range"]];
}

- (void) mouseExited: (NSEvent *) event
{
  [self setUnderlinedRange: nil];
}

- (void) underline: (NSRange) range underlined: (BOOL) isUnderlined
{
  NSDictionary *linkAttributes = 
    [NSDictionary dictionaryWithObject: [NSNumber numberWithBool: isUnderlined] 
      forKey: NSUnderlineStyleAttributeName];
  
  [[self textStorage] addAttributes: linkAttributes range: range];
}

@end
