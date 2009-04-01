#import "KeysArrayController.h"

#import "utils.h"

#define KEY_LENGTH 20

@implementation KeysArrayController

- (void) add: (id) sender
{
  unsigned char randomBytes [KEY_LENGTH];
  
  randomiseBytes (randomBytes, KEY_LENGTH);
  
  NSDictionary *newKey = 
    [NSDictionary dictionaryWithObjectsAndKeys: 
       @"New Key", @"Name", 
       [NSData dataWithBytes: randomBytes length: KEY_LENGTH], @"Data", 
       [NSNumber numberWithBool: YES], @"Private", nil];
  
  [self addObject: newKey];
}

- (IBAction) importFromFile: (id) sender
{
  NSOpenPanel *panel = [NSOpenPanel openPanel];
  
  [panel setCanChooseDirectories: NO];
  [panel setCanChooseFiles: YES];
  [panel setAllowsMultipleSelection: YES];
  
  [panel setTitle: @"Select key files to import"];
  [panel setPrompt: @"Select"];
  
  [panel beginSheetForDirectory: nil file: nil 
    types: nil modalForWindow: [mainPanel window] modalDelegate: self 
    didEndSelector: @selector (importFromFileDidEnd:returnCode:contextInfo:) 
    contextInfo: nil];
}

- (IBAction) importFromClipboard: (id) sender
{
}

- (IBAction) exportToClipboard: (id) sender
{
}

- (void) importFromFileDidEnd: (NSOpenPanel *) panel 
           returnCode: (int) returnCode contextInfo:(void  *)contextInfo
{
}

@end
