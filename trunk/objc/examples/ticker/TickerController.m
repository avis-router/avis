#import "TickerController.h"
#import "AppController.h"

@implementation TickerController

#define TICKER_SUBSCRIPTION \
  @"string (Message) && string (Group) && string (From)"

#define color(r, g, b) \
  [NSColor colorWithCalibratedRed: (r)/255.0 green: (g)/255.0 \
   blue: (b)/255.0 alpha: 1]

static NSAttributedString *attributedString (NSString *string, 
                                             NSDictionary *attrs)
{
  return 
    [[[NSAttributedString alloc] initWithString: string attributes: attrs] 
      autorelease];
}

- (void) handleNotify: (NSDictionary *) message
{
  NSDateFormatter *dateFormatter = [[NSDateFormatter new] autorelease];
  [dateFormatter setDateStyle: NSDateFormatterShortStyle];
  [dateFormatter setTimeStyle: NSDateFormatterMediumStyle];  

  // define reusable display attributes
  NSDictionary *dateAttrs = 
   [NSDictionary dictionaryWithObject: color (102, 102, 102) 
                               forKey: NSForegroundColorAttributeName];

  NSDictionary *fromAttrs = 
    [NSDictionary dictionaryWithObject: color (51, 51, 51) 
                                forKey: NSForegroundColorAttributeName];

  NSDictionary *messageAttrs = 
   [NSDictionary dictionaryWithObject: color (0, 0, 128) 
                               forKey: NSForegroundColorAttributeName];
  
  // build formatted message
  NSMutableAttributedString *displayedMessage = 
    [[NSMutableAttributedString new] autorelease]; 

  [displayedMessage appendAttributedString: 
    attributedString ([dateFormatter stringFromDate: [NSDate date]], 
                      dateAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", dateAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString ([message objectForKey: @"Group"], fromAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", fromAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString ([message objectForKey: @"From"], fromAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", fromAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString ([message objectForKey: @"Message"], messageAttrs)];
  
  [displayedMessage 
    appendAttributedString: attributedString (@"\n", messageAttrs)];
  
  // insert text
  NSRange endRange;
  endRange.location = [[tickerMessagesTextView textStorage] length];
  endRange.length = 0;
  
  [[tickerMessagesTextView textStorage] 
    replaceCharactersInRange: endRange
        withAttributedString: displayedMessage];

  // scroll to end
  // todo do not scroll if not at end when when we started
  endRange.location = [[tickerMessagesTextView textStorage] length];
  [tickerMessagesTextView scrollRangeToVisible: endRange];
}

- (void) awakeFromNib
{ 
  [appController 
    subscribe: TICKER_SUBSCRIPTION 
    withObject: self usingHandler: @selector (handleNotify:)];
}

/*
 * Delegate override to enable message text field to support TAB out but still
 * allow Enter/Return to insert new lines.
 */
- (BOOL) textView: (NSTextView *) textView doCommandBySelector: (SEL) selector
{
  if (selector == @selector (insertTab:))
  {
    if (([[NSApp currentEvent] modifierFlags] & NSAlternateKeyMask) != 0)
    {
      [textView insertTabIgnoringFieldEditor: self];
      
      return YES;
    } else
    {
      return NO;
    }
  } else if (selector == @selector (insertNewline:))
  {
    [textView insertNewlineIgnoringFieldEditor: self];
    
    return YES;
  }
  
  return NO;
}

- (IBAction) sendMessage: (id) sender
{
  [appController sendMessage: [[messageText textStorage] string] 
                     toGroup: [messageGroup stringValue]];
  
  [messageText setString: @""];
}

@end
