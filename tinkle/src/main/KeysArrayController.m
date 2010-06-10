#import "KeysArrayController.h"

#import "ElvinKeyIO.h"

#import "utils.h"

#define KEY_LENGTH 20

@interface KeysArrayController (PRIVATE)
  - (void) importKey: (NSString *) contents;
  - (void) presentKeyImportError: (NSError *) error;
@end

@implementation KeysArrayController

/**
 * For some reason, setting "select new objects" in the controller and binding 
 * selection indexes has no effect, so we're doing it manually.
 */
- (void) addObject: (id) newObject
{
  [super addObject: newObject];
  
  [self setSelectedObjects: [NSArray arrayWithObject: newObject]];

  [[keysTableView superview] becomeFirstResponder];
}

- (id) newObject
{
  unsigned char randomBytes [KEY_LENGTH];
  
  randomiseBytes (randomBytes, KEY_LENGTH);
  
  return
    [[NSMutableDictionary alloc] initWithObjectsAndKeys: 
       @"New Key", @"Name", 
       [NSData dataWithBytes: randomBytes length: KEY_LENGTH], @"Data", 
       [NSNumber numberWithBool: YES], @"Private", nil];
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
  NSDictionary *key = [[self selectedObjects] objectAtIndex: 0];
  NSPasteboard *pasteBoard = [NSPasteboard generalPasteboard];
  
  [pasteBoard declareTypes: [NSArray arrayWithObject: NSStringPboardType] 
                     owner: self];
  
  [pasteBoard 
    setString: [ElvinKeyIO stringFromKey: key] forType: NSStringPboardType];
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
  NSData *keyData = [key valueForKey: @"Data"];
  NSArray *keys = [self content];
  
  for (NSDictionary *existingKey in keys)
  {
    if ([[existingKey valueForKey: @"Data"] isEqual: keyData])
    {
      if ([[key valueForKey: @"Name"] isEqual: [existingKey valueForKey: @"Name"]])
      {
        error = 
        makeError 
          (@"ticker.key", KEY_IO_DUPLICATE_KEY, 
           @"The key “%@” was not imported. " 
           "This key already exists in your key set.", 
           @"You may have imported this key previously.", 
           [key valueForKey: @"Name"]);
        
      } else
      {
        error = 
          makeError 
            (@"ticker.key", KEY_IO_DUPLICATE_KEY, 
             @"The key “%@” was not imported. " 
             "This key already exists in your key set, named “%@”.", 
             @"You may have previously imported this key with a different name.", 
             [key valueForKey: @"Name"], [existingKey valueForKey: @"Name"]);
      }
      
      break;
    }
  }
      
  if (!error)
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
