#import <Cocoa/Cocoa.h>

@class AppController;

extern NSString *PresenceUserWasDoubleClicked;

@interface PresenceController : NSWindowController 
{
  AppController * appController;
  
  IBOutlet NSTableView * presenceTable;
  IBOutlet NSArrayController * presenceTableController;
}

- (id) initWithAppController: (AppController *) theAppController;

@property (readonly) IBOutlet AppController * appController;

@end
