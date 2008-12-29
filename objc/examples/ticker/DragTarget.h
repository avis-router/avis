#import <Cocoa/Cocoa.h>

/**
 * A generic drag target that forwards D&D calls to its delegate.
 */
@interface DragTarget : NSView
{
  id dragDelegate;
}

@property (readwrite, assign) id dragDelegate;

@end
