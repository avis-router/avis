#import "Key.h"

#import "utils.h"

#define keyError(code, message, args...) \
  makeError (@"ticker.key", code, message, args)

@implementation Key

static BOOL readNameValue (NSString *line, 
                           NSString **returnName, 
                           NSString **returnValue, 
                           NSError **error);

static NSData *unhexify (NSString *text, NSError **error);

@synthesize type;

@synthesize name;

@synthesize data;

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
  
  NSString *field, *value;
  
  for (NSString *line in lines)
  {
    line = trim (line);
    
    if ([line length] == 0)
      continue;

    if (readNameValue (line, &field, &value, error))
      break;
      
    if ([field isEqual: @"Version"])
    {
      if (![value hasPrefix: @"1."])
      {
        *error = keyError (KEY_IO_VERSION, 
                            @"Unknown key format version: \"%@\"", value);
      }
    } else if ([field isEqual: @"Name"])
    {
      self.name = value;
    } else if ([field isEqual: @"Access"])
    {
      if ([value isEqual: @"Shared"])
        self.type = KEY_TYPE_PUBLIC;
      else if ([value isEqual: @"Private"])
        self.type = KEY_TYPE_PRIVATE;
      else
        *error = keyError (KEY_IO_ACCESS, 
                            @"Unknown key access type: \"%@\"", value);
    } else if ([field isEqual: @"Key"])
    {
      self.data = unhexify (value, error);
    } else 
    {
      *error = 
        keyError (KEY_IO_UNKNOWN_FIELD, @"Unknown field: \"%@\"", field);
    }
    
    if (*error)
      break;
  }
  
  return *error ? nil : self;
}

BOOL readNameValue (NSString *line, 
                    NSString **returnName, 
                    NSString **returnValue, 
                    NSError **error)
{
  NSMutableString *field = [[NSMutableString alloc] initWithCapacity: 20];
  NSMutableString *value = [[NSMutableString alloc] initWithCapacity: 80];
  NSMutableString *target = field;
  
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
        if (target == field)
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
    *returnName = trim (field);
    *returnValue = trim (value);    
  } else
  {
    *error = keyError (KEY_IO_MISSING_VALUE, 
                        @"Key field \"%@\" is missing a value", field);
   
    *returnName = *returnValue = nil;
  }
  
  [field release];
  [value release];
  
  return *error != nil;
}

NSData *unhexify (NSString *text, NSError **error)
{
  NSMutableData *data = [NSMutableData dataWithCapacity: [text length]];
  
  for (NSUInteger i = 0; i < [text length] && !*error; i++)
  {
    unichar c = [text characterAtIndex: i];
    unsigned char b;
    
    if (c >= '0' && c <= '9')
      b = c - '0';
    else if (c >= 'a' && c <= 'f')
      b = c - 'a' + 10;
    else if (c >= 'A' && c <= 'F')
      b = c - 'A' + 10;
    else
      *error = keyError (KEY_IO_BAD_HEX_DIGIT, 
                          @"Invalid hex digit: %C", c);
    
    [data appendBytes: &b length: 1];
  }
  
  return *error ? nil : data;
}
                    
@end
