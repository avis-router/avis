#import <Foundation/Foundation.h>

@class RelativeDateFormatter;

@interface PresenceStatusFormatter : NSFormatter
{
  RelativeDateFormatter *durationFormatter;
}

@end
