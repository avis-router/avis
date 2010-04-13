#import "PresenceViewController.h"
#import "PresenceConnection.h"
#import "PresenceEntity.h"
#import "Preferences.h"

NSString *PresenceEntityClickedNotification = @"PresenceEntityClicked";

static NSString *formatDuration (NSDate *value);

@implementation PresenceViewController

- (void) dealloc
{
  [super dealloc];
}

- (void) viewDidLoad
{
  [super viewDidLoad];
  
  // self.tableView.directionalLockEnabled = YES;
}

- (void) setPresence: (PresenceConnection *) newPresence
{
  [presence release];
  
  presence = [newPresence retain];
  
  presence.delegate = self;
  
  [self.tableView reloadData];
}

- (PresenceConnection *) presence
{
  return presence;
}

- (void) presenceEntitiesAdded
{
  [self.tableView reloadData];
}

- (void) presenceEntityChanged: (NSIndexPath *) row
{
  [self.tableView reloadRowsAtIndexPaths: [NSArray arrayWithObject: row]
    withRowAnimation: UITableViewRowAnimationFade];
}

- (void) presenceEntitiesCleared
{
  [self.tableView reloadData];
}

// Override to allow orientations other than the default portrait orientation.
- (BOOL) shouldAutorotateToInterfaceOrientation: (UIInterfaceOrientation) orientation
{
  return YES;
}

- (void) didReceiveMemoryWarning
{
	// Releases the view if it doesn't have a superview.
  [super didReceiveMemoryWarning];
	
	// Release any cached data, images, etc that aren't in use.
}

- (void) viewDidUnload
{
	// Release any retained subviews of the main view.
	// e.g. self.myOutlet = nil;
}

#pragma mark -
#pragma mark UITableViewDelegate

- (void) tableView: (UITableView *) tableView 
         didSelectRowAtIndexPath: (NSIndexPath *) indexPath
{ 
  PresenceEntity *entity = [presence.entities objectAtIndex: indexPath.row];
  
  [[NSNotificationCenter defaultCenter] 
    postNotificationName: PresenceEntityClickedNotification object: entity];
}

#pragma mark -
#pragma mark Table view methods

- (NSInteger) numberOfSectionsInTableView: (UITableView *) tableView
{
  return 1;
}

// Customize the number of rows in the table view.
- (NSInteger) tableView: (UITableView *) tableView 
  numberOfRowsInSection: (NSInteger) section 
{
  return [presence.entities count];
}

// Customize the appearance of table view cells.
- (UITableViewCell *) tableView: (UITableView *) tableView
    cellForRowAtIndexPath: (NSIndexPath *) indexPath 
{
  static NSString *CellIdentifier = @"PresenceCell";
    
  UITableViewCell *cell = 
    [tableView dequeueReusableCellWithIdentifier: CellIdentifier];

  if (cell == nil)
  {
    cell = [[[UITableViewCell alloc]
             initWithStyle: UITableViewCellStyleSubtitle
               reuseIdentifier: CellIdentifier] autorelease];
  } 
  
  PresenceEntity *entity = [presence.entities objectAtIndex: indexPath.row];
  PresenceStatus *status = entity.status;
  
	cell.textLabel.text = entity.name;
  cell.detailTextLabel.text = 
    [NSString stringWithFormat: @"%@ (%@)", status.statusText, 
     formatDuration (status.changedAt)];
     
  NSString *image;
  
  if (status.statusCode == ONLINE)
    image = @"lolly_green.png";
  else if (status.statusCode == OFFLINE)
    image = @"lolly_grey.png";
  else
    image = @"lolly_yellow.png";

  cell.imageView.image = [UIImage imageNamed: image];

  return cell;
}

#pragma mark -
#pragma mark Duration formatting

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

NSString *formatDuration (NSDate *value)
{
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
