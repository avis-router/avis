#import "ElvinKeyIO.h"

#import "utils.h"

#define keyError(code, message, ...) \
  makeError (@"ticker.key", code, \
    [NSString stringWithFormat: @"Error in key format: %@", message], \
    @"The data may not be an Elvin key, or it may have been exported from " \
    "an incompatible application.", \
    ##__VA_ARGS__)

static BOOL readNameValue (NSString *line, 
                           NSString **returnName, 
                           NSString **returnValue);

static NSData *unhexify (NSString *text, NSError **error);

static NSString *hexify (NSData *data);

static unsigned char hexToDec (unichar digit, NSError **error);

typedef enum {KEY_TYPE_PUBLIC, KEY_TYPE_PRIVATE} KeyAccessType;

NSString *KeyFieldName = @"Name";
NSString *KeyFieldIsPrivate = @"Private";
NSString *KeyFieldData = @"Data";

@implementation ElvinKeyIO

+ (NSDictionary *) keyFromFile: (NSString *) file error: (NSError **) error
{
  NSString *contents = 
    [NSString stringWithContentsOfFile: file 
      encoding: NSUTF8StringEncoding error: error];
  
  if (contents)
    return [ElvinKeyIO keyFromString: contents error: error];
  else
    return nil;
}

+ (NSDictionary *) keyFromString: (NSString *) contents error: (NSError **) error
{
  KeyAccessType type = -1;
  NSString *name = nil;
  NSData *data = nil;
  BOOL versionSeen = NO;
  
  NSArray *lines = 
    [contents componentsSeparatedByCharactersInSet: 
      [NSCharacterSet newlineCharacterSet]];
  
  NSString *field, *value;
  
  for (NSString *line in lines)
  {
    line = trim (line);
    
    if ([line length] == 0)
      continue;
    
    if (!readNameValue (line, &field, &value))
      continue;
    
    if ([field isEqual: @"Version"])
    {
      if ([value hasPrefix: @"1."])
        versionSeen = YES;
      else
        *error = keyError (KEY_IO_VERSION, 
                           @"Unknown key format version: “%@”", value);
    } else if ([field isEqual: @"Name"])
    {
      name = value;
    } else if ([field isEqual: @"Access"])
    {
      if ([value isEqual: @"Shared"])
        type = KEY_TYPE_PUBLIC;
      else if ([value isEqual: @"Private"])
        type = KEY_TYPE_PRIVATE;
      else
        *error = keyError 
          (KEY_IO_ACCESS, 
           @"Unknown key type: “%@”. Should be either “Private” or “Shared”", 
           value);
    } else if ([field isEqual: @"Key"])
    {
      if ([value length] > 0)
        data = unhexify (value, error);
      else
        *error = 
          keyError (KEY_IO_BAD_HEX_DATA, @"Key data is empty", field);
    }
    
    if (*error)
      break;
  }
  
  if (!*error)
  {
    if (type == -1)
      *error = 
        keyError (KEY_IO_MISSING_FIELD, @"Missing “Access” field");
    else if (name == nil)
      *error = 
         keyError (KEY_IO_MISSING_FIELD, @"Missing “Name” field");
    else if (data == nil)
      *error = 
        keyError (KEY_IO_MISSING_FIELD, @"Missing “Key” field");
    else if (versionSeen == NO)
      *error = 
        keyError (KEY_IO_MISSING_FIELD, @"Missing “Version” field");
  }
  
  if (*error)
  {
    return nil;
  } else
  {
    return [NSMutableDictionary dictionaryWithObjectsAndKeys: 
              name, KeyFieldName,
              [NSNumber numberWithBool: type == KEY_TYPE_PRIVATE], 
              KeyFieldIsPrivate, data, KeyFieldData, nil];
  }
}

+ (NSString *) stringFromKey: (NSDictionary *) key;
{
  return 
    [NSString stringWithFormat: @"Version: 1.0\nName: %@\nAccess: %@\nKey: %@",
     [key valueForKey: @"Name"], 
     [[key valueForKey: @"Private"] intValue] == 1 ? @"Private" : @"Shared",
     hexify ([key valueForKey: @"Data"])];
  
}

BOOL readNameValue (NSString *line, 
                    NSString **returnName, 
                    NSString **returnValue)
{
  BOOL error = NO;
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
    *returnName = trim (field);
    *returnValue = trim (value);    
  } else
  {
    error = YES;
    
    *returnName = *returnValue = nil;
  }
  
  [field release];
  [value release];
  
  return !error;
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

NSString *hexify (NSData *data)
{
  NSMutableString *string = 
    [NSMutableString stringWithCapacity: [data length] * 2];
  
  for (NSUInteger i = 0; i < [data length]; i++)
  {
    unsigned char byte;
    
    [data getBytes: &byte range: NSMakeRange (i, 1)];
    
    [string appendFormat: @"%x", (byte >> 4)];
    [string appendFormat: @"%x", (byte & 0x0F)];
  }
  
  return string;
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
