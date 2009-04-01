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
}

- (IBAction) importFromClipboard: (id) sender
{
}

- (IBAction) exportToClipboard: (id) sender
{
}

@end
