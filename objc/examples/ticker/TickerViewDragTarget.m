#import "TickerViewDragTarget.h"

@implementation TickerViewDragTarget

- (void) awakeFromNib
{
  [self registerForDraggedTypes: [NSArray arrayWithObject: NSURLPboardType]];
}

#pragma mark Dragging Destination

- (NSDragOperation) draggingEntered: (id <NSDraggingInfo>) sender
{
  if ([sender draggingSource] == self) 
    return NSDragOperationNone;
  else
    return NSDragOperationCopy;
}

- (void) draggingExited: (id <NSDraggingInfo>)sender
{
}

- (BOOL) prepareForDragOperation: (id <NSDraggingInfo>) sender
{
  return YES;
}

- (BOOL) performDragOperation: (id <NSDraggingInfo>) sender
{
  NSPasteboard *pasteboard = [sender draggingPasteboard];
  
  [tickerController setAttachedURL: [NSURL URLFromPasteboard: pasteboard]];
  
  return YES;
}

- (void) concludeDragOperation: (id <NSDraggingInfo>) sender
{
  // zip
}

@end
