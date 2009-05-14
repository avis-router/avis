#import <Cocoa/Cocoa.h>

@class AppController;

@interface PresenceController : NSWindowController 
{
  AppController * appController;
  
  IBOutlet NSTableView * presenceTable;
}

- (id) initWithAppController: (AppController *) theAppController;

@property (readonly) IBOutlet AppController * appController;

@end
