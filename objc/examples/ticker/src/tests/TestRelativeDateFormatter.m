#import "TestRelativeDateFormatter.h"

#import "RelativeDateFormatter.h"

#define MINUTE 60
#define HOUR (60 * MINUTE)
#define DAY (24 * HOUR)
#define WEEK (7 * DAY)

@implementation TestRelativeDateFormatter

- (void) testBasic
{
  RelativeDateFormatter *formatter = 
    [[[RelativeDateFormatter alloc] init] autorelease];
  
  NSDate *date = 
     [NSDate dateWithTimeIntervalSinceNow: -(1 * WEEK + 1 * DAY + 1 * HOUR + 5 * MINUTE)];
  
  STAssertEqualObjects ([formatter stringForObjectValue: date], 
                        @"1 week 1 day 1:05 ago", @"Date not formatted");
  
  date = 
    [NSDate dateWithTimeIntervalSinceNow: -(1 * WEEK + 0 * DAY + 1 * HOUR + 5 * MINUTE)];
  
  STAssertEqualObjects ([formatter stringForObjectValue: date], 
                        @"1 week 1:05 ago", @"Date not formatted");
  
  date = 
    [NSDate dateWithTimeIntervalSinceNow: -(2 * DAY + 0 * HOUR + 5 * MINUTE)];
  
  STAssertEqualObjects ([formatter stringForObjectValue: date], 
                        @"2 days 0:05 ago", @"Date not formatted");
}

@end
