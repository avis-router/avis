#import "utils.h"

void createUUID (char *uuid)
{
  CFUUIDRef cfUUID = CFUUIDCreate (kCFAllocatorDefault);
  CFStringRef cfUUIDString = CFUUIDCreateString (kCFAllocatorDefault, cfUUID);
  
  CFStringGetCString (cfUUIDString, uuid, UUID_STRING_LENGTH, 
                      kCFStringEncodingASCII);
  
  CFRelease (cfUUIDString);
  CFRelease (cfUUID);
}

NSString *uuidString ()
{
  char uuid [UUID_STRING_LENGTH];
  
  createUUID (uuid);
  
  return [NSString stringWithCString: uuid];
}

void randomiseBytes (unsigned char *bytes, NSUInteger length)
{
  static BOOL seeded = NO;
  
  if (!seeded)
  {
    srandomdev ();
    seeded = YES;
  }  
  
  for (NSUInteger i = 0; i < length; i++)
    bytes [i] = (unsigned char)(random () % 256);
}

NSString *trim (NSString *string)
{
  return [string stringByTrimmingCharactersInSet: 
    [NSCharacterSet whitespaceAndNewlineCharacterSet]];
}

NSError *makeError (NSString *domain, NSInteger code, NSString *message, 
                    NSString *recovery, ...)
{
  va_list args;
  va_start (args, recovery);
  
  NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
  
  [dictionary setObject: 
   [[[NSString alloc] initWithFormat: message arguments: args] autorelease] 
                             forKey: NSLocalizedDescriptionKey];
  
  va_end (args);
  
  if (recovery)
    [dictionary setObject: recovery forKey: NSLocalizedRecoverySuggestionErrorKey]; 
   
  return [NSError errorWithDomain: domain code: code userInfo: dictionary];
}