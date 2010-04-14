#import <Cocoa/Cocoa.h>

@interface RolloverButton : NSButton
{
  NSImage *originalImage;
}

@property (readwrite, assign) IBOutlet BOOL rollover;

@end
