#import "PresenceTableViewController.h"
#import "PresenceConnection.h"
#import "PresenceEntity.h"
#import "Preferences.h"

static NSString *formatDuration (NSDate *value);

@implementation PresenceTableViewController

- (void) dealloc
{
  [super dealloc];
}

/*
- (id)initWithStyle:(UITableViewStyle)style {
    // Override initWithStyle: if you create the controller programmatically and want to perform customization that is not appropriate for viewDidLoad.
    if (self = [super initWithStyle:style]) {
    }
    return self;
}
*/

- (void) viewDidLoad
{
  [super viewDidLoad];

  self.title = @"Presence";
  
  // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
  // self.navigationItem.rightBarButtonItem = self.editButtonItem;
}

- (void) setPresence: (PresenceConnection *) newPresence
{
  [presence release];
  
  presence = [newPresence retain];
  
  presence.delegate = self;
  
  [self.tableView reloadData];
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

- (PresenceConnection *) presence
{
  return presence;
}

/*
- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
}
*/
/*
- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
}
*/
/*
- (void)viewWillDisappear:(BOOL)animated {
	[super viewWillDisappear:animated];
}
*/
/*
- (void)viewDidDisappear:(BOOL)animated {
	[super viewDidDisappear:animated];
}
*/

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
  
  setPref (PrefDefaultSendGroup, entity.name);
  
  self.tabBarController.selectedIndex = 1;
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

/*
// Override to support conditional editing of the table view.
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    // Return NO if you do not want the specified item to be editable.
    return YES;
}
*/


/*
// Override to support editing the table view.
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        // Delete the row from the data source
        [tableView deleteRowsAtIndexPaths:[NSArray arrayWithObject:indexPath] withRowAnimation:YES];
    }   
    else if (editingStyle == UITableViewCellEditingStyleInsert) {
        // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
    }   
}
*/


/*
// Override to support rearranging the table view.
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath {
}
*/


/*
// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    // Return NO if you do not want the item to be re-orderable.
    return YES;
}
*/

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

