#import <Cocoa/Cocoa.h>

@interface TextViewLinkifier : NSObject
{
  NSTextView *view;
}

@property (readwrite, retain) IBOutlet NSTextView *view;

@end
