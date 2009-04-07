#import "KeysArrayController.h"

#import "ElvinKeyIO.h"

#import "utils.h"

#define KEY_LENGTH 20

@interface KeysArrayController (PRIVATE)
  - (void) importKey: (NSString *) contents;
  - (void) presentKeyImportError: (NSError *) error;
@end

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

- (IBAction) importFromClipboard: (id) sender
{
  NSString *contents = 
  [[NSPasteboard generalPasteboard] stringForType: NSStringPboardType];
  
  if (contents)
    [self importKey: contents];
}

- (IBAction) exportToClipboard: (id) sender
{
}

- (IBAction) importFromFile: (id) sender
{
  NSOpenPanel *panel = [NSOpenPanel openPanel];
  
  [panel setCanChooseDirectories: NO];
  [panel setCanChooseFiles: YES];
  [panel setAllowsMultipleSelection: YES];
    
  [panel beginSheetForDirectory: nil file: nil 
    types: nil modalForWindow: [mainPanel window] modalDelegate: self 
    didEndSelector: @selector (importFromFileDidEnd:returnCode:contextInfo:) 
    contextInfo: nil];
}

- (void) importFromFileDidEnd: (NSOpenPanel *) panel 
           returnCode: (int) returnCode contextInfo: (void *) contextInfo
{
  if (returnCode != NSOKButton)
    return;
  
  [panel orderOut: self];
  
  for (NSString *file in [panel filenames])
  {
    NSError *error;
    NSString *contents = 
      [NSString stringWithContentsOfFile: file 
        encoding: NSUTF8StringEncoding error: &error];
    
    if (contents)
      [self importKey: contents];
    else
      [self presentKeyImportError: error];
  }
}

- (void) importKey: (NSString *) contents
{
  NSError *error;
  NSDictionary *key = [ElvinKeyIO keyFromString: contents error: &error];
  
  if (key)
    [self addObject: key];
  else
    [self presentKeyImportError: error];
}

- (void) presentKeyImportError: (NSError *) error
{
  [[mainPanel window] presentError: error
                    modalForWindow: [mainPanel window]
                          delegate: self
                didPresentSelector: @selector (didPresentErrorWithRecovery:contextInfo:)
                       contextInfo: nil];  
}

- (void) didPresentErrorWithRecovery: (BOOL) recover
                         contextInfo: (void *)info
{
  // zip
}

@end
