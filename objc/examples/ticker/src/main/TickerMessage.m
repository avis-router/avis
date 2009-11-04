#import "TickerMessage.h"

#import "ElvinConnection.h"

#import "utils.h"

#define MIME_URI_PREFIX @"MIME-Version: 1.0\r\nContent-Type: text/uri-list\r\n\r\n"

static NSURL *extractAttachedLink (NSDictionary *message)
{
  if ([message objectForKey: @"Attachment"])
  {
    // hack to extract text/uri-list attachments
    NSString *attachment = [message objectForKey: @"Attachment"];
    NSRange payloadDelimiter = [attachment rangeOfString: @"\r\n\r\n"];
    
    if ([attachment rangeOfString: @"MIME-Version: 1.0"].length &&
        [attachment rangeOfString: @"Content-Type: text/uri-list"].length &&
        payloadDelimiter.length)
    {
      return [NSURL URLWithString: 
               trim ([[message objectForKey: @"Attachment"] 
                 substringFromIndex: NSMaxRange (payloadDelimiter)])];
    } else
    {
      NSLog (@"Don't know how to parse ticker attachment:\n%@", attachment);
      
      return nil;
    }
  } else if ([[message objectForKey: @"MIME_TYPE"] isEqual: @"x-elvin/url"])
  {
    return [NSURL URLWithString: [message objectForKey: @"MIME_ARGS"]];
  } else
  {
    return nil;
  }
}

@implementation TickerMessage

+ (TickerMessage *) messageForNotification: (NSDictionary *) notification
{
  TickerMessage *message = [[TickerMessage new] autorelease];
  
  NSString *distribution = [notification valueForKey: @"Distribution"];
  
  message->messageId = [[notification valueForKey: @"Message-Id"] retain];
  message->threadId = [[notification valueForKey: @"Thread-Id"] retain];
  message->from = [[notification valueForKey: @"From"] retain];
  message->message = [[notification valueForKey: @"Message"] retain];
  message->group = [[notification valueForKey: @"Group"] retain];
  message->public = 
    distribution != nil && [distribution caseInsensitiveCompare: @"world"] == 0;
  message->userAgent = [[notification valueForKey: @"User-Agent"] retain];
  message->url = [extractAttachedLink (notification) retain];
  message->receivedAt = [[NSDate date] retain];
  message->secure = [ElvinConnection wasReceivedSecure: notification];
  
  return message;
}

- (void) dealloc
{
  [messageId release];
  [threadId release];
  [from release];
  [message release];
  [group release];
  [userAgent release];
  [url release];
  [receivedAt release];
  
  [super dealloc];
}

@end
