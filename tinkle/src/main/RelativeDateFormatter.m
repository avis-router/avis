#import "RelativeDateFormatter.h"

/*
 < 60 s

 1 second ago
 30 seconds ago
 
 < 1 hour
 
 1m:00-1:45 1 minute ago
 1m:45 Nearly 2 minutes ago
 
 2m:00-2:45 Nearly 3 minutes ago
 
 < 1 day
 
 1h:00m 1 Hour ago
 1h:02 1 hour 2 minutes ago
 
 2h:14m 2 hours 14 minutes ago
 
 < 1 year
 
 1d:01h:15m 1 day 1:15 ago
 
 2d:05h:45m 2 days 5:45 ago
 
 7d:01h:45m 1 week 1 day 5:45 ago
 
 29d:01h:45m 4 weeks 1 day 5:45 ago
 
 > 1 year
 
 More than a year ago
*/

#define MINUTE 60
#define HOUR (60 * MINUTE)
#define DAY (24 * HOUR)
#define WEEK (7 * DAY)

static NSString *pluralizeSeconds (int seconds)
{
  return (seconds == 1) ? @"second" : @"seconds";
}

static NSString *pluralizeMinutes (int minutes)
{
  return (minutes == 1) ? @"minute" : @"minutes";
}

static NSString *pluralizeHours (int hours)
{
  return (hours == 1) ? @"hour" : @"hours";
}

static NSString *pluralizeDays (int days)
{
  return (days == 1) ? @"day" : @"days";
}

static NSString *pluralizeWeeks (int weeks)
{
  return (weeks == 1) ? @"week" : @"weeks";
}

@implementation RelativeDateFormatter

- (NSString *) stringForObjectValue: (id) value
{
  if (![value isKindOfClass: [NSDate class]])
    return @"!!";
  
  int duration = 
    (int)[[NSDate date] timeIntervalSinceDate: value];

  if (duration < 10)
  {
    return @"Just now";
  } else if (duration < 1 * MINUTE)
  {
    // less than a minute
    return [NSString stringWithFormat: @"%i %@ ago", 
            duration, pluralizeSeconds (duration)];
  } else if (duration < 1 * HOUR)
  {
    // less than an hour
    int minutes = duration / MINUTE;
    int seconds = duration % MINUTE;

    if (minutes > 57)
    {
      return @"Nearly an hour ago";
    } else if (seconds < 45)
    {
      return [NSString stringWithFormat: @"%i %@ ago", 
              minutes, pluralizeMinutes (minutes)];  
    } else
    {
      return [NSString stringWithFormat: @"Nearly %i %@ ago", 
              minutes + 1, pluralizeMinutes (minutes + 1)];    
    }
  } else if (duration < 1 * DAY)
  {
    // less than a day
    int hours = duration / HOUR;
    int minutes = (duration - hours * HOUR) / MINUTE;
    
    if (minutes == 0)
    {
      return [NSString stringWithFormat: @"%i %@ ago", 
              hours, pluralizeHours (hours)];
    } else if (hours == 23 && minutes >= 30)
    {
      return @"Nearly a day ago";
    } else
    {
      return [NSString stringWithFormat: @"%i %@ %i %@ ago", 
              hours, pluralizeHours (hours), 
              minutes, pluralizeMinutes (minutes)];
    }
  } else if (duration < 1 * WEEK)
  {
    // less than week
    int days = duration / DAY;
    duration %= DAY;
    int hours = duration / HOUR;
    duration %= HOUR;
    int minutes = duration / MINUTE;
    
    if (days == 6 && hours == 23)
      return @"Nearly a week ago";
    else
      return [NSString stringWithFormat: @"%i %@ %i:%.2i ago", 
              days, pluralizeDays (days), hours, minutes];
  } else if (duration < 8 * WEEK)
  {
    // up to 8 weeks ago
    int weeks = duration / WEEK;
    duration %= WEEK;
    int days = duration / DAY;
    duration %= DAY;
    int hours = duration / HOUR;
    duration %= HOUR;
    int minutes = duration / MINUTE;

    if (days == 0)
    {
      return [NSString stringWithFormat: @"%i %@ %i:%.2i ago", 
              weeks, pluralizeWeeks (weeks), hours, minutes];
    } else
    {
      return [NSString stringWithFormat: @"%i %@ %i %@ %i:%.2i ago", 
              weeks, pluralizeWeeks (weeks), days, pluralizeDays (days), 
              hours, minutes];
    }
  } else
  {
    return @"More than two months ago";
  }
}

@end
