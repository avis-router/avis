#import <Cocoa/Cocoa.h>

/**
 * Adds auto link underlining to an NSTextView on cursor hover.
 */
@interface TextViewWithLinks : NSTextView
{
  NSValue *underlinedRange;
}

@end
