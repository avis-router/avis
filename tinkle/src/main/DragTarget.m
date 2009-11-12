#import "DragTarget.h"

@implementation DragTarget

@synthesize dragDelegate;

- (int) forwardMethod: (SEL) selector sender: (id) sender
{
  int result;
  NSInvocation *invocation = 
    [NSInvocation invocationWithMethodSignature: 
      [[dragDelegate class] instanceMethodSignatureForSelector: selector]];
  
  [invocation setSelector: selector];
  [invocation setTarget: dragDelegate];
  [invocation setArgument: &sender atIndex: 2];
  [invocation invoke];
  [invocation getReturnValue: &result];
  
  return result;
}

- (NSDragOperation) draggingEntered: (id <NSDraggingInfo>) sender
{
  if ([dragDelegate respondsToSelector: @selector (draggingEntered:)])
    return [self forwardMethod: @selector (draggingEntered:) sender: sender];
  else
    return NSDragOperationNone;
}

- (void) draggingExited: (id <NSDraggingInfo>) sender
{
  if ([dragDelegate respondsToSelector: @selector (draggingExited:)])
    [self forwardMethod: @selector (draggingExited:) sender: sender];
}

- (BOOL) prepareForDragOperation: (id <NSDraggingInfo>) sender
{
  if ([dragDelegate respondsToSelector: @selector (prepareForDragOperation:)])
    return [self forwardMethod: @selector (prepareForDragOperation:) sender: sender];
  else
    return YES;
}

- (BOOL) performDragOperation: (id <NSDraggingInfo>) sender
{
  if ([dragDelegate respondsToSelector: @selector (performDragOperation:)])
    return [self forwardMethod: @selector (performDragOperation:) sender: sender];
  else
    return NO;
}

- (void) concludeDragOperation: (id <NSDraggingInfo>) sender
{
  if ([dragDelegate respondsToSelector: @selector (concludeDragOperation:)])
    [self forwardMethod: @selector (concludeDragOperation:) sender: sender];
}

@end
