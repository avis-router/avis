#import <Foundation/Foundation.h>

@class RelativeDateFormatter;

@interface PresenceEntityFormatter : NSFormatter
{
  RelativeDateFormatter *durationFormatter;
}

@end
