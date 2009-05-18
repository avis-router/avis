#import "PresenceTableViewController.h"

#import "PresenceEntity.h"
#import "PresenceEntityFormatter.h"

@implementation PresenceTableViewController

- (CGFloat) tableView: (NSTableView *) tableView heightOfRow: (NSInteger) row
{
  static int rowHeight = 0;
  
  if (rowHeight == 0)
  {
    PresenceEntityFormatter *formatter = 
      [[[PresenceEntityFormatter alloc] init] autorelease];
    
    PresenceEntity *entity = [PresenceEntity entityWithName: @"Test"];
    
    NSAttributedString *text = 
      [formatter attributedStringForObjectValue: entity 
       withDefaultAttributes: [NSDictionary dictionary]];
                              
    rowHeight = [text size].height;
  }
                        
  return rowHeight;
}

@end
