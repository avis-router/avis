#import <Cocoa/Cocoa.h>

@interface RolloverButton : NSButton
{
  NSImage *rolloverImage;
  NSImage *originalImage;
}

@property (readwrite, retain) IBOutlet NSImage *rolloverImage;

- (void) setRollover: (BOOL) rolledOver;

@end
