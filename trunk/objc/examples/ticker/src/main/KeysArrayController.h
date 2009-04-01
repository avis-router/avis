#import <Cocoa/Cocoa.h>

@interface KeysArrayController : NSArrayController 
{
  IBOutlet NSView *mainPanel;
}

- (IBAction) importFromFile: (id) sender;

- (IBAction) importFromClipboard: (id) sender;

- (IBAction) exportToClipboard: (id) sender;

@end
