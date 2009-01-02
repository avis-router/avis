#import <Cocoa/Cocoa.h>

/**
 * Adds auto link underlining to an NSTextView on cursor hover.
 */
@interface TextViewLinkifier : NSObject
{
  NSTextView *view;
}

@property (readwrite, retain) IBOutlet NSTextView *view;

@end
