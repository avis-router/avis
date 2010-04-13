#import <Cocoa/Cocoa.h>

/**
 * Protocol for delegates that handle enter/exit link callbacks from the text
 * view.
 */
@protocol LinkCallbacks

@optional

- (void) mouseEnteredLink: (NSRange) linkRange ofTextView: (NSTextView *) view;
- (void) mouseExitedLink: (NSRange) linkRange ofTextView: (NSTextView *) view;

@end

/**
 * Adds auto link underlining to an NSTextView on cursor hover.
 */
@interface TextViewWithLinks : NSTextView
{
  NSValue *underlinedRange;
}

@end
