#import "Growl/GrowlApplicationBridge.h"

#import "TickerController.h"
#import "RolloverButton.h"

#import "AppController.h"
#import "ElvinConnection.h"

#import "utils.h"

#define TICKER_SUBSCRIPTION \
  @"string (Message) && string (Group) && string (From)"

#define MAX_GROWL_MESSAGE_LENTGH 200

#pragma mark -

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

#pragma mark -

@interface TickerMessage : NSObject
{
  @public
  
  NSString * messageId;
  NSString * group;
  NSString * userAgent;
  BOOL       public;
}

@end

@implementation TickerMessage

+ (TickerMessage *) messageForNotification: (NSDictionary *) notification
{
  TickerMessage *link = [[TickerMessage new] retain];
  
  NSString *distribution = [notification valueForKey: @"Distribution"];
  
  link->messageId = [[notification valueForKey: @"Message-Id"] retain];
  link->group = [[notification valueForKey: @"Group"] retain];
  link->public = 
    distribution != nil && [distribution caseInsensitiveCompare: @"world"] == 0;
  link->userAgent = [[notification valueForKey: @"User-Agent"] retain];
  
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

/**
 * TODO: this is for tooltip display. Should make this an interface or 
 * somesuch.
 */
- (NSString *) description
{
  return [NSString stringWithFormat: 
           @"Public: %@\nClient: %@", 
           public ? @"yes" : @"no", 
           userAgent ? userAgent : @"Unknown"];
}

@end

#pragma mark -

@interface TickerController ()
//  - (void) setConnectedStatus: (BOOL) connected;
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

- (id) initWithAppController: (AppController *) theAppController
{
  self = [super initWithWindowNibName: @"TickerWindow"];
  
  if (self)
  {
    appController = theAppController;
      
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
  
  return self;
}

- (void) dealloc
{
  [[NSNotificationCenter defaultCenter] removeObserver: self]; 
  
  [super dealloc];
}

#pragma mark -

- (void) windowDidLoad
{
  replyButton.rolloverImage = [NSImage imageNamed: @"Reply_Rollover"];
  
  self.canSend = [appController.elvin isConnected];
  
  [tickerMessagesTextView setLinkTextAttributes: [NSDictionary dictionary]];
  
  [self setAttachedURL: nil];
  [self setInReplyTo: nil];
  
  [dragTarget 
    registerForDraggedTypes: [NSArray arrayWithObject: NSURLPboardType]];  
}

- (BOOL) validateMenuItem: (NSMenuItem *) item
{
  SEL action = [item action];
  
  if (action == @selector (clearAttachedURL:))
    return self.attachedURL != nil;
  else if (action == @selector (clearReply:))
    return self.inReplyTo != nil;
  else if (action == @selector (sendMessage:))
    return self.canSend;
  else if (action == @selector (togglePublic:)) 
  {
    [item setState: allowPublic ? NSOnState : NSOffState];
    
    return YES; 
  } else
    return YES;
}

- (void) handleElvinOpen: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
    self.canSend = YES;
  else
    [self performSelectorOnMainThread: @selector (handleElvinOpen:) 
          withObject: nil waitUntilDone: NO];
}

- (void) handleElvinClose: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
    self.canSend = NO;
  else
    [self performSelectorOnMainThread: @selector (handleElvinClose:) 
          withObject: nil waitUntilDone: NO];
}

- (void) notifyGrowl: (NSDictionary *) ntfn
{
  NSString *message =
   [NSString stringWithFormat: @"%@: %@", 
    [ntfn objectForKey: @"From"], [ntfn objectForKey: @"Message"]];
 
  // Growl should really handle long messages but...
  if ([message length] > MAX_GROWL_MESSAGE_LENTGH)
  {
    message = [NSString stringWithFormat: @"%@...", 
                [message substringToIndex: MAX_GROWL_MESSAGE_LENTGH]];
  }
  
  NSString *userName = prefsString (@"OnlineUserName");
  NSString *type;
  int priority = 0;
  BOOL sticky = NO;
  
  if ([[ntfn objectForKey: @"Group"] isEqual: userName])
  {
    type = @"Personal Message";
    
    if (![[ntfn objectForKey: @"From"] isEqual: userName])
    {
      priority = 1;
      sticky = YES;
    }
  } else
  {
    type = @"Ticker Message";
  }
  
  [GrowlApplicationBridge
    notifyWithTitle: @"Message received"
    description: message notificationName: type
    iconData: nil priority: priority isSticky: sticky clickContext: nil];
}

- (void) handleNotify: (NSDictionary *) ntfn
{
  [self notifyGrowl: ntfn];
  
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
     [TickerMessage messageForNotification: ntfn] forKey: NSLinkAttributeName];
     
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

  NSRange range;
  
  range.location = [[tickerMessagesTextView textStorage] length];
  range.length = 0;
  
  // start new line for all but first message
  if (range.location > 0)
  {
   [[tickerMessagesTextView textStorage] 
     replaceCharactersInRange: range 
     withAttributedString: attributedString (@"\n", dateAttrs)];
  }
  
  // build formatted message
  NSMutableAttributedString *displayedMessage = 
    [[NSMutableAttributedString new] autorelease]; 

  [displayedMessage appendAttributedString: 
    attributedString ([dateFormatter stringFromDate: [NSDate date]], 
                      dateAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", dateAttrs)];
  
  // save start of actual message (minus date)
  range.location = [displayedMessage length];

  [displayedMessage appendAttributedString: 
    attributedString ([ntfn objectForKey: @"Group"], groupAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", dateAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString ([ntfn objectForKey: @"From"], fromAttrs)];
  
  [displayedMessage appendAttributedString: attributedString (@": ", dateAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString ([ntfn objectForKey: @"Message"], messageAttrs)];
  
  // create link to message
  range.length = [displayedMessage length] - range.location;
  
  [displayedMessage addAttributes: replyLinkAttrs range: range];
  
  NSURL *attachedLink = extractAttachedLink (ntfn);
  
  if (attachedLink)
  {    
    NSDictionary *linkAttrs = 
      [NSDictionary dictionaryWithObjectsAndKeys:
       [NSColor blueColor], NSForegroundColorAttributeName, 
       [NSNumber numberWithBool: NO], NSUnderlineStyleAttributeName,
       attachedLink, NSLinkAttributeName, nil];

    [displayedMessage 
      appendAttributedString: attributedString (@" (", dateAttrs)];

    if ([[attachedLink absoluteString] length] <= 40)
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
  
  // append text
  [[tickerMessagesTextView textStorage] 
    replaceCharactersInRange: 
      NSMakeRange ([[tickerMessagesTextView textStorage] length], 0) 
    withAttributedString: displayedMessage];

  [tickerMessagesTextView 
    setFont: [NSFont fontWithName: @"Lucida Grande" size: 11]];
  
  // scroll to end if that's how it was when we started
  if (wasScrolledToEnd)
  {
    [tickerMessagesTextView scrollRangeToVisible: 
      NSMakeRange ([[tickerMessagesTextView textStorage] length], 0)];
  }
}

#pragma mark -

@dynamic canSend;

- (BOOL) canSend
{
  return canSend;
}

- (void) setCanSend: (BOOL) newValue
{
  canSend = newValue;
  
  [sendButton setEnabled: canSend];
  
  [sendButton setToolTip: 
   canSend ? nil : @"Cannot send: currently disconnected"];
}

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
    inReplyTo: self.inReplyTo 
    attachedURL: self.attachedURL
    sendPublic: self.allowPublic];

  self.attachedURL = nil;
  self.inReplyTo = nil;
  self.allowPublic = NO;
  
  [messageText setString: @""];
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
  self.attachedURL = nil;
}

@dynamic attachedURL;

- (void) setAttachedURL: (NSURL *) url
{
  if (url)
  {
    NSMutableParagraphStyle *paraStyle = 
      [[[NSParagraphStyle defaultParagraphStyle] mutableCopy] autorelease];
      
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

- (IBAction) clearReply: (id) sender
{
  self.inReplyTo = nil;
  self.allowPublic = NO;
}

- (IBAction) togglePublic: (id) sender
{
  self.allowPublic = !self.allowPublic;
}

@synthesize inReplyTo;

@synthesize allowPublic;

#pragma mark -

#pragma mark URL D&D

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

#pragma mark Text view delegates

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
  if ([link isKindOfClass: [TickerMessage class]])
  {
    // handle clicks on links to messages to initiate a reply
    TickerMessage *messageLink = link;
    
    [messageGroup setStringValue: messageLink->group];
    
    self.inReplyTo = messageLink->messageId;
    self.allowPublic = messageLink->public;
    
    [[messageText window] makeFirstResponder: messageText];

    return YES;
  } else
  {
    return NO;
  }
}

#pragma mark "Empty Text" sheet delegates

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
