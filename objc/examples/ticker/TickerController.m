#import "TickerController.h"

/*
 * TODO: use example at 
 * http://developer.apple.com/samplecode/TextLinks/listing2.html
 * to add custom cursor to links. 
 * OR use NSView::addTrackingArea
 */

static NSURL *extractAttachedLink (NSDictionary *message)
{
  if ([[message objectForKey: @"MIME_TYPE"] isEqual: @"x-elvin/url"])
  {
    return [NSURL URLWithString: [message objectForKey: @"MIME_ARGS"]];
  } else if ([message objectForKey: @"Attachment"])
  {
    // TODO
    return nil;
  } else
  {
    return nil;
  }
}

#pragma mark PRIVATE MessageLink internal class definition

/*
 * Used to link original message group/ID with message links in the message 
 * view.
 */
@interface MessageLink : NSObject
{
  @public
  
  NSString * messageId;
  NSString * group;
  BOOL       public;
}

+ (MessageLink *) linkForMessage: (NSDictionary *) message;

@end

@implementation MessageLink

+ (MessageLink *) linkForMessage: (NSDictionary *) message
{
  MessageLink *link = [[MessageLink new] retain];
  
  NSString *distribution  = [message valueForKey: @"Distribution"];
  
  link->messageId = [[message valueForKey: @"Message-Id"] retain];
  link->group = [[message valueForKey: @"Group"] retain];
  link->public = 
    distribution != nil && [distribution caseInsensitiveCompare: @"world"] == 0;
  
  return link;
}

- (void) dealloc
{
  [super dealloc];
  
  [messageId release];
  [group release];
  
  messageId = nil;
  group = nil;
}

@end

@implementation TickerController

#define TICKER_SUBSCRIPTION \
  @"string (Message) && string (Group) && string (From)"

#define color(r, g, b) \
  [NSColor colorWithCalibratedRed: (r)/255.0 green: (g)/255.0 \
   blue: (b)/255.0 alpha: 1]

#define bottomY(rect) ((rect).origin.y + (rect).size.height)

#pragma mark PRIVATE Delegates handling callback from Elvin

static NSAttributedString *attributedString (NSString *string, 
                                             NSDictionary *attrs)
{
  return 
    [[[NSAttributedString alloc] initWithString: string attributes: attrs] 
      autorelease];
}

- (void) handleNotify: (NSDictionary *) message
{
  NSRange endRange;
  endRange.location = [[tickerMessagesTextView textStorage] length];
  endRange.length = 0;

  // decide on whether we're scrolled to the end of the messages
  NSPoint containerOrigin = [tickerMessagesTextView textContainerOrigin];
  NSRect visibleRect = NSOffsetRect ([tickerMessagesTextView visibleRect], 
                                     -containerOrigin.x, -containerOrigin.y);
  NSRect tickerMessagesRect = [tickerMessagesTextView bounds];
  
  bool wasScrolledToEnd = 
    bottomY (visibleRect) == bottomY (tickerMessagesRect);
  
  NSDateFormatter *dateFormatter = [[NSDateFormatter new] autorelease];
  [dateFormatter setDateStyle: NSDateFormatterShortStyle];
  [dateFormatter setTimeStyle: NSDateFormatterMediumStyle];  

  // define display attributes
  NSDictionary *dateAttrs = 
    [NSDictionary dictionaryWithObject: color (102, 102, 102) 
                               forKey: NSForegroundColorAttributeName];
  NSDictionary *groupAttrs = 
    [NSDictionary dictionaryWithObjectsAndKeys:
     color (48, 80, 10), NSForegroundColorAttributeName, 
     [MessageLink linkForMessage: message], NSLinkAttributeName, nil];

  NSDictionary *fromAttrs = 
    [NSDictionary dictionaryWithObject: color (86, 56, 12) 
                                forKey: NSForegroundColorAttributeName];

  NSDictionary *messageAttrs = 
    [NSDictionary dictionaryWithObject: color (0, 64, 128) 
                                forKey: NSForegroundColorAttributeName];

  // build formatted message
  NSMutableAttributedString *displayedMessage = 
    [[NSMutableAttributedString new] autorelease]; 

  // start new line for all but first message
  if (endRange.location != 0)
  {
    [displayedMessage 
      appendAttributedString: attributedString (@"\n", messageAttrs)];
  }

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
  
  NSURL *attachedLink = extractAttachedLink (message);
  
  if (attachedLink)
  {
    [displayedMessage 
      appendAttributedString: attributedString (@" (", messageAttrs)];
    
    NSDictionary *linkAttrs = 
      [NSDictionary dictionaryWithObjectsAndKeys:
       color (0, 0, 255), NSForegroundColorAttributeName, 
       [NSNumber numberWithBool: YES], NSUnderlineStyleAttributeName,
       attachedLink, NSLinkAttributeName, nil];
    
    [displayedMessage appendAttributedString: 
      attributedString ([attachedLink absoluteString], linkAttrs)];
    
    [displayedMessage 
      appendAttributedString: attributedString (@")", messageAttrs)];
  }
  
  // insert text

  [[tickerMessagesTextView textStorage] 
    replaceCharactersInRange: endRange
        withAttributedString: displayedMessage];

  [tickerMessagesTextView 
    setFont: [NSFont fontWithName: @"Lucida Grande" size: 11]];
  
  // scroll to end if that's how it was when we started
  if (wasScrolledToEnd)
  {
    endRange.location = [[tickerMessagesTextView textStorage] length];
    
    [tickerMessagesTextView scrollRangeToVisible: endRange];
  }
}

- (void) awakeFromNib
{ 
  [tickerMessagesTextView setLinkTextAttributes: [NSDictionary dictionary]];
  
  [appController.elvin
    subscribe: TICKER_SUBSCRIPTION 
    withDelegate: self usingSelector: @selector (handleNotify:)];
}

#pragma mark PRIVATE Delegates for text view

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

/*
 * Delegate handler for links for text views.
 */
- (BOOL) textView: (NSTextView *) textView
    clickedOnLink: (id) link atIndex: (unsigned) charIndex
{
  if ([link isKindOfClass: [MessageLink class]])
  {
    // handle clicks on links to messages to initiate a reply
    MessageLink *messageLink = link;
    
    [messageGroup setStringValue: messageLink->group];
    [publicCheckbox setState: (messageLink->public ? NSOnState : NSOffState)];
    
    [replyToMessageId release];
    replyToMessageId = [messageLink->messageId retain];
    
    [[messageText window] makeFirstResponder: messageText];

    return YES;
  } else
  {
    return NO;
  }
}

#pragma mark PUBLIC IB actions

- (IBAction) sendMessage: (id) sender
{
  NSString *message = [[messageText textStorage] string];

  // check for empty text
  if (sender != self && 
      [[message stringByTrimmingCharactersInSet: 
        [NSCharacterSet whitespaceAndNewlineCharacterSet]] length] == 0)
  {
    // allow our willPositionSheet to put sheet under text editor
    [[messageText window] setDelegate: self];
    
    NSBeginAlertSheet 
      (@"The ticker message text is empty.", @"Don't Send", 
       @"Send Empty Message", nil,  [messageText window], self, 
       @selector (emptyMessageCheckDidEnd:returnCode:contextInfo:), nil, nil, 
       @"Send the message anyway?");
    
    return;
  }
  
  [appController.elvin 
    sendTickerMessage: message toGroup: [messageGroup stringValue] 
    inReplyTo: replyToMessageId 
    sendPublic: [publicCheckbox state] == NSOnState];
  
  [replyToMessageId release];
  replyToMessageId = nil;
  
  [messageText setString: @""];
  [publicCheckbox setState: NSOffState];
}

#pragma mark PRIVATE Methods handling "Empty Text" sheet

/*
 * Handles request for sheet location: locates the "empty text" sheet on
 * the text area itself rather than the top of the window.
 */
- (NSRect) window: (NSWindow *) window willPositionSheet: (NSWindow *) sheet
        usingRect: (NSRect) rect 
{
  NSRect fieldRect = [[[messageText superview] superview] frame];
  
  fieldRect.size.height = 0;
  
  return fieldRect;
}

- (void) emptyMessageCheckDidEnd: (NSAlert *) alert returnCode: (int) code
                     contextInfo: (void *) contextInfo
{
  [[messageText window] setDelegate: nil];
  
  // re-send request: when sender is self the checks are overridden
  if (code != NSAlertDefaultReturn)
    [self sendMessage: self];
}

@end
