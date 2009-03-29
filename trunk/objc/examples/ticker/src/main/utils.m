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

NSString *trim (NSString *string)
{
  return [string stringByTrimmingCharactersInSet: 
    [NSCharacterSet whitespaceAndNewlineCharacterSet]];
}