#import <Cocoa/Cocoa.h>

@interface RolloverButton : NSButton
{
  NSImage *originalImage;
}

- (void) setRollover: (BOOL) rolledOver;

@end
