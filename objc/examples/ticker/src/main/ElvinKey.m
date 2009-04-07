#import "ElvinKey.h"

#import "utils.h"

#define keyError(code, message, ...) \
  makeError (@"ticker.key", code, message, nil, ##__VA_ARGS__)

@implementation ElvinKey

static BOOL readNameValue (NSString *line, 
                           NSString **returnName, 
                           NSString **returnValue, 
                           NSError **error);

static NSData *unhexify (NSString *text, NSError **error);

static unsigned char hexToDec (unichar digit, NSError **error);

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
  
  type = -1;
  name = nil;
  data = nil;
  BOOL versionSeen = NO;
  
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
      if ([value hasPrefix: @"1."])
        versionSeen = YES;
      else
        *error = keyError (KEY_IO_VERSION, 
                            @"Unknown key format version: \"%@\"", value);
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
  
  if (!*error)
  {
    if (type == -1)
      *error = 
        keyError (KEY_IO_MISSING_FIELD, @"Missing access type");
    else if (name == nil)
      *error = 
        keyError (KEY_IO_MISSING_FIELD, @"Missing name");
    else if (data == nil)
      *error = 
        keyError (KEY_IO_MISSING_FIELD, @"Missing data");
    else if (versionSeen == NO)
      *error = 
        keyError (KEY_IO_MISSING_FIELD, @"Missing version");
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
  if ([text length] % 2 != 0)
  {
    *error = keyError (KEY_IO_BAD_HEX_DATA, 
                       @"Hex data must have an even number of digits");
    
    return nil;
  }

  NSMutableData *data = [NSMutableData dataWithCapacity: [text length] / 2];
  
  for (NSUInteger i = 0; i < [text length] && !*error; i += 2)
  {
    unichar c1 = [text characterAtIndex: i];
    unichar c2 = [text characterAtIndex: i + 1];
    
    unsigned char b = (hexToDec (c1, error) << 4) + hexToDec (c2, error);
    
    [data appendBytes: &b length: 1];
  }
  
  return *error ? nil : data;
}

unsigned char hexToDec (unichar digit, NSError **error)
{
  if (digit >= '0' && digit <= '9')
    return digit - '0';
  else if (digit >= 'a' && digit <= 'f')
    return digit - 'a' + 10;
  else if (digit >= 'A' && digit <= 'F')
    return digit - 'A' + 10;
  else
  {
    *error = keyError (KEY_IO_BAD_HEX_DATA, 
                       @"Invalid hex digit: %C", digit);  
    return 0;
  }
}
                    
@end
