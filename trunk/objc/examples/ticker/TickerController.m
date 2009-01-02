#import "TickerController.h"

#import "AppController.h"
#import "ElvinConnection.h"

#import "utils.h"

#define TICKER_SUBSCRIPTION \
  @"string (Message) && string (Group) && string (From)"

#pragma mark PRIVATE Utility functions

static inline NSColor *color (float r, float g, float b)
{
  return [NSColor colorWithCalibratedRed: r/255.0 green: g/255.0 blue: b/255.0 
                                          alpha: 1];
}

static inline float bottomY (NSRect rect) 
{
  return rect.origin.y + rect.size.height;
}

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

@interface TickerController ()
  - (void) setConnectedStatus: (BOOL) connected;
  - (void) handleNotify: (NSDictionary *) message;
  - (void) handleElvinOpen: (void *) unused;
  - (void) handleElvinClose: (void *) unused;
  - (void) setAttachedURLPanelHidden: (BOOL) hidden;
  - (BOOL) textView: (NSTextView *) textView doCommandBySelector: (SEL) selector;
  - (BOOL) textView: (NSTextView *) textView
           clickedOnLink: (id) link atIndex: (unsigned) charIndex;
  - (void) emptyMessageCheckDidEnd: (NSAlert *) alert returnCode: (int) code
           contextInfo: (void *) contextInfo;
@end

@implementation TickerController

#pragma mark PRIVATE Cocoa overrides

- (id) initWithAppController: (AppController *) theAppController
{
  appController = theAppController;
  
  return [super initWithWindowNibName: @"TickerWindow"];
}

- (void) windowDidLoad
{
  [self setConnectedStatus: [appController.elvin isConnected]];
  
  [tickerMessagesTextView setLinkTextAttributes: [NSDictionary dictionary]];
  [self setAttachedURL: nil];
  
  [dragTarget 
    registerForDraggedTypes: [NSArray arrayWithObject: NSURLPboardType]];
  
  [appController.elvin
    subscribe: TICKER_SUBSCRIPTION 
    withDelegate: self usingSelector: @selector (handleNotify:)];    
  
  // listen for elvin open/close
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];

  [notifications addObserver: self selector: @selector (handleElvinOpen:)
    name: ElvinConnectionOpenedNotification object: nil]; 
    
  [notifications addObserver: self selector: @selector (handleElvinClose:)
    name: ElvinConnectionClosedNotification object: nil]; 
}

- (void) dealloc
{
  [[NSNotificationCenter defaultCenter] removeObserver: self]; 
  
  [super dealloc];
}

#pragma mark PRIVATE Delegates handling callback from Elvin

- (void) handleElvinOpen: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
    [self setConnectedStatus: YES];
  else
    [self performSelectorOnMainThread: @selector (handleElvinOpen:) 
          withObject: nil waitUntilDone: NO];
}

- (void) handleElvinClose: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
    [self setConnectedStatus: NO];
  else
    [self performSelectorOnMainThread: @selector (handleElvinClose:) 
          withObject: nil waitUntilDone: NO];
}

- (void) setConnectedStatus: (BOOL) connected
{
  [sendButton setEnabled: connected];

  [sendButton setToolTip: 
    connected ? nil : @"Cannot send: currently disconnected"];
}

- (void) handleNotify: (NSDictionary *) message
{
  NSRange range;
  
  range.location = [[tickerMessagesTextView textStorage] length];
  range.length = 0;

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
  NSDictionary *replyLinkAttrs = 
    [NSDictionary dictionaryWithObject:
     [MessageLink linkForMessage: message] forKey: NSLinkAttributeName];
     
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

  // start new line for all but first message
  if (range.location > 0)
  {
   [[tickerMessagesTextView textStorage] 
     replaceCharactersInRange: range 
     withAttributedString: attributedString (@"\n", dateAttrs)];
     
    range.location = [[tickerMessagesTextView textStorage] length];
  }
  
  // build formatted message
  NSMutableAttributedString *displayedMessage = 
    [[NSMutableAttributedString new] autorelease]; 

  [displayedMessage appendAttributedString: 
    attributedString ([dateFormatter stringFromDate: [NSDate date]], 
                      dateAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", dateAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString ([message objectForKey: @"Group"], groupAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", dateAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString ([message objectForKey: @"From"], fromAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", dateAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString ([message objectForKey: @"Message"], messageAttrs)];
  
  [displayedMessage addAttributes: replyLinkAttrs 
    range: NSMakeRange (0, [displayedMessage length])];
  
  NSURL *attachedLink = extractAttachedLink (message);
  
  if (attachedLink)
  {    
    NSDictionary *linkAttrs = 
      [NSDictionary dictionaryWithObjectsAndKeys:
       [NSColor blueColor], NSForegroundColorAttributeName, 
       [NSNumber numberWithBool: NO], NSUnderlineStyleAttributeName,
       attachedLink, NSLinkAttributeName, nil];

    [displayedMessage 
      appendAttributedString: attributedString (@" (", dateAttrs)];

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
      appendAttributedString: attributedString (@")", dateAttrs)];
  }
  
  // insert text

  [[tickerMessagesTextView textStorage] 
    replaceCharactersInRange: range withAttributedString: displayedMessage];

  [tickerMessagesTextView 
    setFont: [NSFont fontWithName: @"Lucida Grande" size: 11]];
  
  // scroll to end if that's how it was when we started
  if (wasScrolledToEnd)
  {
    range.location = [[tickerMessagesTextView textStorage] length];
    
    [tickerMessagesTextView scrollRangeToVisible: range];
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
  
  [appController.elvin 
    sendTickerMessage: message 
    fromSender: prefsString (@"OnlineUserName")
    toGroup: [messageGroup stringValue] 
    inReplyTo: replyToMessageId 
    attachedURL: [self attachedURL]
    sendPublic: [publicCheckbox state] == NSOnState];

  [self setAttachedURL: nil];
  
  [replyToMessageId release];
  replyToMessageId = nil;
  
  [messageText setString: @""];
  [publicCheckbox setState: NSOffState];
}

- (void) setAttachedURLPanelHidden: (BOOL) hidden
{
  if (hidden == [attachedUrlPanel isHidden])
    return;

  NSView *textContainerView = [[messageText superview] superview];
    
  [attachedUrlPanel setHidden: hidden];
                   
  for (NSView *subview in [attachedUrlPanel subviews])
    [[subview animator] setHidden: hidden];

  NSRect newFrame;
  
  // adjust message text area size
  if (hidden)
  {
    newFrame = [[textContainerView superview] frame];
    newFrame.origin.x = newFrame.origin.y = 0;
  } else
  {
    NSRect urlBounds = [attachedUrlPanel frame];
    
    newFrame = [textContainerView frame];

    newFrame.origin.y += urlBounds.size.height;
    newFrame.size.height -= urlBounds.size.height;
  }
  
  [[textContainerView animator] setFrame: newFrame];
}

- (IBAction) clearAttachedURL: (id) sender
{
  [self setAttachedURL: nil];
}

- (void) setAttachedURL: (NSURL *) url
{
  if (url)
  {
    NSMutableParagraphStyle *paraStyle = 
      [[NSParagraphStyle defaultParagraphStyle] mutableCopy];
      
    [paraStyle setLineBreakMode: NSLineBreakByTruncatingMiddle];
    
    NSDictionary *linkAttrs = 
      [NSDictionary dictionaryWithObjectsAndKeys:
       [NSColor blueColor], NSForegroundColorAttributeName, 
       [NSNumber numberWithBool: YES], NSUnderlineStyleAttributeName,
       [NSFont fontWithName: @"Lucida Grande" size: 11], NSFontAttributeName,
       paraStyle, NSParagraphStyleAttributeName,
       url, NSLinkAttributeName, nil];

    NSAttributedString *urlText = 
     [[[NSAttributedString alloc] 
       initWithString: [url absoluteString] attributes: linkAttrs] autorelease];

    [attachedUrlLabel setAttributedStringValue: urlText];

    [self setAttachedURLPanelHidden: NO];
  } else
  {    
    [self setAttachedURLPanelHidden: YES];

    [attachedUrlLabel setObjectValue: nil];
  }
}

- (NSURL *) attachedURL
{
  if ([attachedUrlPanel isHidden])
    return nil;
  else 
    return [NSURL URLWithString: [attachedUrlLabel stringValue]];
}

#pragma mark URL dragging destination

- (NSDragOperation) draggingEntered: (id <NSDraggingInfo>) sender
{
  if ([sender draggingSource] == self) 
    return NSDragOperationNone;
  else
    return NSDragOperationCopy;
}

- (BOOL) performDragOperation: (id <NSDraggingInfo>) sender
{
  NSPasteboard *pasteboard = [sender draggingPasteboard];
  
  [self setAttachedURL: [NSURL URLFromPasteboard: pasteboard]];
  
  return YES;
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

#pragma mark PRIVATE Methods handling "Empty Text" sheet

/*
 * Handles request for sheet location: locates the "empty text" sheet on
 * the text area itself rather than the top of the window.
 */
//- (NSRect) window: (NSWindow *) window willPositionSheet: (NSWindow *) sheet
//           usingRect: (NSRect) rect 
//{
//  NSRect fieldRect = [[[[messageText superview] superview] superview] frame];
//  
//  fieldRect.size.height = 0;
//  
//  return fieldRect;
//}

- (void) emptyMessageCheckDidEnd: (NSAlert *) alert returnCode: (int) code
         contextInfo: (void *) contextInfo
{
  [[messageText window] setDelegate: nil];
  
  // re-send request: when sender is self the checks are overridden
  if (code != NSAlertDefaultReturn)
    [self sendMessage: self];
}

@end
