#import <Cocoa/Cocoa.h>

@interface KeysArrayController : NSArrayController 
{
}

- (IBAction) importFromFile: (id) sender;

- (IBAction) importFromClipboard: (id) sender;

- (IBAction) exportToClipboard: (id) sender;

@end
