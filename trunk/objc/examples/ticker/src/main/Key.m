#import "Key.h"

#import "utils.h"

@implementation Key

BOOL readNameValue (NSString *line, 
                    NSString **returnName, 
                    NSString **returnValue, 
                    NSError **error);

- (id) initWithFile: (NSString *) file error: (NSError **) error
{
  NSString *contents = 
    [NSString stringWithContentsOfFile: file 
      encoding: NSUTF8StringEncoding error: error];
      
  if (contents)
    self = [self initWithString: contents error: error];

  return *error ? nil : self;
}

- (id) initWithString: (NSString *) text error: (NSError **) error
{
  if (!(self = [super init]))
    return nil;

  NSArray *lines = 
    [text componentsSeparatedByCharactersInSet: 
      [NSCharacterSet newlineCharacterSet]];
  
  NSString *name, *value;
  
  for (NSString *line in lines)
  {
    line = trim (line);
    
    if ([line length] == 0)
      continue;

    if (readNameValue (line, &name, &value, error))
      break;
  }
  
  return *error ? nil : self;
}

BOOL readNameValue (NSString *line, 
                    NSString **returnName, 
                    NSString **returnValue, 
                    NSError **error)
{
  NSMutableString *name = [[NSMutableString alloc] initWithCapacity: 20];
  NSMutableString *value = [[NSMutableString alloc] initWithCapacity: 80];
  NSMutableString *target = name;
  
  for (NSUInteger i = 0; i < [line length]; i++)
  {
    unichar c = [line characterAtIndex: i];
    
    switch (c)
    { 
      case '\\':
        i++;
        
        if (i < [line length])
          [target appendFormat: @"%C", [line characterAtIndex: i]];
        break;
      case ':':
        if (target == name)
          target = value;
        else
          [target appendFormat: @"%C", c];
          
        break;
      default:
        [target appendFormat: @"%C", c];
    }
  }
  
  if (target == value)
  {
    *error = nil;
    *returnName = trim (name);
    *returnValue = trim (value);    
  } else
  {
    *error = [NSError errorWithDomain: @"ticker.key" code: 1 
               userInfo: [NSDictionary dictionaryWithObject: @"Key is missing a value" 
                  forKey: NSLocalizedFailureReasonErrorKey]];
   
    *returnName = *returnValue = nil;
  }
  
  [name release];
  [value release];
  
  return *error != nil;
}

@end
