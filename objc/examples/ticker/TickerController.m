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
  NSDictionary *groupAttrs = 
    [NSDictionary dictionaryWithObject: color (48, 80, 10) 
                               forKey: NSForegroundColorAttributeName];
  
  NSDictionary *fromAttrs = 
    [NSDictionary dictionaryWithObject: color (86, 56, 12) 
                                forKey: NSForegroundColorAttributeName];

  NSDictionary *messageAttrs = 
    [NSDictionary dictionaryWithObject: color (0, 64, 128) 
                                forKey: NSForegroundColorAttributeName];
  
  // build formatted message
  NSMutableAttributedString *displayedMessage = 
    [[NSMutableAttributedString new] autorelease]; 

  [displayedMessage appendAttributedString: 
    attributedString ([dateFormatter stringFromDate: [NSDate date]], 
                      dateAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", dateAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString ([message objectForKey: @"Group"], groupAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", groupAttrs)];
  
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

  [tickerMessagesTextView 
    setFont: [NSFont fontWithName: @"Lucida Sans" size: 11]];
  
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

- (void) emptyMessageCheckDidEnd: (NSAlert *) alert returnCode: (int) code
                     contextInfo: (void *) contextInfo
{
  // re-send request: when sender is self the checks are overridden
  if (code != NSAlertDefaultReturn)
    [self sendMessage: self];
}

- (IBAction) sendMessage: (id) sender
{
  NSString *message = [[messageText textStorage] string];
  
  // check for empty text
  if (sender != self && 
      [[message stringByTrimmingCharactersInSet: 
        [NSCharacterSet whitespaceAndNewlineCharacterSet]] length] == 0)
  {
    NSAlert *alert = 
      [NSAlert alertWithMessageText: @"The ticker message text is empty." 
       defaultButton: @"Don't Send" alternateButton: @"Send Empty Message"
       otherButton: nil 
       informativeTextWithFormat: @"Send the message anyway?"];
    
    [alert beginSheetModalForWindow: [messageText window] 
           modalDelegate: self 
           didEndSelector: 
             @selector (emptyMessageCheckDidEnd:returnCode:contextInfo:) 
           contextInfo: nil];
            
    return;
  }
  
  [appController sendMessage: message toGroup: [messageGroup stringValue]];
  
  [messageText setString: @""];
}

@end
