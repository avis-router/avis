#import <Foundation/Foundation.h>

#define UUID_STRING_LENGTH 100

#ifdef LOG_TRACE
  #define TRACE(m, a...) NSLog (m, a)
#else
  #define TRACE(m, a...) 
#endif

void createUUID (char *uuid);

NSString *uuidString ();

void randomiseBytes (unsigned char *bytes, NSUInteger length);

NSString *trim (NSString *string);

NSError *makeError (NSString *domain, NSInteger code, NSString *message, 
                    NSString *recovery, ...);
