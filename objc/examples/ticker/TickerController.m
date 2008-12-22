#import "TickerController.h"

/*
 * TODO: use example at 
 * http://developer.apple.com/samplecode/TextLinks/listing2.html
 * to add custom cursor to links. 
 * OR use NSView::addTrackingArea
 */

#define TICKER_SUBSCRIPTION \
  @"string (Message) && string (Group) && string (From)"

#define color(r, g, b) \
  [NSColor colorWithCalibratedRed: (r)/255.0 green: (g)/255.0 \
   blue: (b)/255.0 alpha: 1]

#define bottomY(rect) ((rect).origin.y + (rect).size.height)

#pragma mark PRIVATE Utility functions

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

static NSAttributedString *attributedString (NSString *string, 
                                             NSDictionary *attrs)
{
  return 
    [[[NSAttributedString alloc] initWithString: string attributes: attrs] 
      autorelease];
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

@end

@implementation MessageLink

+ (MessageLink *) linkForMessage: (NSDictionary *) message
{
  MessageLink *link = [[MessageLink new] retain];
  
  NSString *distribution = [message valueForKey: @"Distribution"];
  
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

#pragma mark PRIVATE Delegates handling callback from Elvin

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
    NSDictionary *linkAttrs = 
      [NSDictionary dictionaryWithObjectsAndKeys:
       [NSColor blueColor], NSForegroundColorAttributeName, 
       [NSNumber numberWithBool: YES], NSUnderlineStyleAttributeName,
       attachedLink, NSLinkAttributeName, nil];

    [displayedMessage 
      appendAttributedString: attributedString (@" (", messageAttrs)];

    if ([[attachedLink path] length] <= 40)
    {
      [displayedMessage appendAttributedString: 
        attributedString ([attachedLink absoluteString], linkAttrs)];
    } else
    {
      // display truncated URL
      [displayedMessage appendAttributedString: 
        attributedString ([attachedLink scheme], linkAttrs)];

      [displayedMessage appendAttributedString: 
        attributedString (@"://", linkAttrs)];
                  
      [displayedMessage appendAttributedString: 
        attributedString ([attachedLink host], linkAttrs)];

      [displayedMessage appendAttributedString: 
        attributedString (@"/...", linkAttrs)];
    }           
    
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
    
  [attachedUrlLabel setObjectValue: nil];
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

#pragma mark PUBLIC methods

- (IBAction) sendMessage: (id) sender
{
  NSString *message = [[messageText textStorage] string];

  // check for empty text
  if (sender != self && 
      [[message stringByTrimmingCharactersInSet: 
        [NSCharacterSet whitespaceAndNewlineCharacterSet]] length] == 0)
  {
    // make self the delegate to put sheet under text editor
    [[messageText window] setDelegate: self];
    
    NSBeginAlertSheet 
      (@"The ticker message text is empty.", @"Don't Send", 
       @"Send Empty Message", nil,  [messageText window], self, 
       @selector (emptyMessageCheckDidEnd:returnCode:contextInfo:), nil, nil, 
       @"Send the message anyway?");
    
    return;
  }
  
  NSURL *attachedURL = [NSURL URLWithString: [attachedUrlLabel stringValue]];
  
  [appController.elvin 
    sendTickerMessage: message toGroup: [messageGroup stringValue] 
    inReplyTo: replyToMessageId 
    attachedURL: attachedURL
    sendPublic: [publicCheckbox state] == NSOnState];
  
  // clear URL
  [self setAttachedURL: nil];
  
  [replyToMessageId release];
  replyToMessageId = nil;
  
  [messageText setString: @""];
  [publicCheckbox setState: NSOffState];
}

- (void) setAttachedURL: (NSURL *) url
{
  id textContainerView = [[messageText superview] superview];
    
  if (url)
  {
    NSDictionary *linkAttrs = 
      [NSDictionary dictionaryWithObjectsAndKeys:
       [NSColor blueColor], NSForegroundColorAttributeName, 
       [NSNumber numberWithBool: YES], NSUnderlineStyleAttributeName,
       [NSFont userFontOfSize: 11], NSFontAttributeName,
        url, NSLinkAttributeName, nil];

    NSMutableAttributedString *urlText = 
     [[NSMutableAttributedString new] autorelease]; 

    [urlText appendAttributedString: 
      attributedString ([url absoluteString], linkAttrs)];
      
    NSRect messageTextBounds = [textContainerView frame];
    NSRect urlBounds = [attachedUrlLabel frame];

    messageTextBounds.size.height -= urlBounds.size.height;
    messageTextBounds.origin.y += urlBounds.size.height;
    
    [textContainerView setFrame: messageTextBounds];
    [textContainerView setNeedsDisplay: YES];

    [attachedUrlLabel setAttributedStringValue: urlText];
    [attachedUrlLabel setHidden: NO];
  } else
  {
    NSRect frame = [[textContainerView superview] frame];
    frame.origin.x = frame.origin.y = 0;
    
    [attachedUrlLabel setHidden: YES];
    [attachedUrlLabel setObjectValue: nil];
    
    [textContainerView setFrame: frame];
    [textContainerView setNeedsDisplay: YES];
  }
}

#pragma mark PRIVATE Methods handling "Empty Text" sheet

/*
 * Handles request for sheet location: locates the "empty text" sheet on
 * the text area itself rather than the top of the window.
 */
- (NSRect) window: (NSWindow *) window willPositionSheet: (NSWindow *) sheet
        usingRect: (NSRect) rect 
{
  NSRect fieldRect = [[[[messageText superview] superview] superview] frame];
  
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
