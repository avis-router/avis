#import <Cocoa/Cocoa.h>

@class AppController;

extern NSString *PresenceUserWasDoubleClicked;

@interface PresenceController : NSWindowController 
{
  AppController * appController;
  NSTimer *       refreshTimer;
  
  IBOutlet NSTableView * presenceTable;
  IBOutlet NSArrayController * presenceTableController;
}

- (id) initWithAppController: (AppController *) theAppController;

@property (readonly) IBOutlet AppController * appController;

@property (readonly) IBOutlet NSArray * statuses;

@end
