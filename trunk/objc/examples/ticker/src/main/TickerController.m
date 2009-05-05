#import "Growl/GrowlApplicationBridge.h"

#import "utils.h"

#import "TickerController.h"
#import "RolloverButton.h"
#import "ElvinConnection.h"
#import "Preferences.h"

#define TICKER_SUBSCRIPTION \
  @"string (Message) && string (Group) && string (From)"

#define MAX_GROWL_MESSAGE_LENGTH 200

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
  NSString * from;
  NSString * message;
  NSString * group;
  NSString * userAgent;
  NSURL    * url;
  BOOL       public;
  BOOL       secure;
  NSDate   * receivedAt;
}

@end

@implementation TickerMessage

+ (TickerMessage *) messageForNotification: (NSDictionary *) notification
{
  TickerMessage *message = [[TickerMessage new] autorelease];
  
  NSString *distribution = [notification valueForKey: @"Distribution"];
  
  message->messageId = [[notification valueForKey: @"Message-Id"] retain];
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
  [from release];
  [message release];
  [group release];
  [userAgent release];
  [url release];
  [receivedAt release];
  
  [super dealloc];
}

@end

#pragma mark -

@interface TickerController (PRIVATE)
  - (NSString *) fullTickerSubscription;
  - (void) handleNotify: (NSDictionary *) message;
  - (void) notifyGrowlOnTickerMessage: (TickerMessage *) message;
  - (void) notifyGrowlOnElvinStatusChange: (NSString *) status;
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

- (id) initWithElvin: (ElvinConnection *) theElvinConnection 
        subscription: (NSString *) theSubscription
{
  self = [super initWithWindowNibName: @"TickerWindow"];
  
  if (self)
  {
    elvin = theElvinConnection;
    self.subscription = theSubscription;
    
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

- (void) awakeFromNib
{
  [tickerMessagesTextView setString: @""];

  self.allowInsecure = YES;
  self.canSend = [elvin isConnected];
}

- (void) windowDidLoad
{
  replyButton.rolloverImage = [NSImage imageNamed: @"Reply_Rollover"];
    
  [tickerMessagesTextView setLinkTextAttributes: [NSDictionary dictionary]];
  
  self.attachedURL = nil;
  self.inReplyTo = nil;
  
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
  } else if (action == @selector (toggleSecure:)) 
  {
    [item setState: allowInsecure ? NSOffState : NSOnState];
    
    return YES; 
  } else
    return YES;
}

- (void) handleElvinOpen: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    self.canSend = YES;

    [self notifyGrowlOnElvinStatusChange: @"connected"];
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinOpen:) 
          withObject: nil waitUntilDone: NO];
  }
}

- (void) handleElvinClose: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    self.canSend = NO;
    
    [self notifyGrowlOnElvinStatusChange: @"disconnected"];
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinClose:) 
          withObject: nil waitUntilDone: NO];
  }
}

- (void) handleNotify: (NSDictionary *) ntfn
{
  TickerMessage *message = [TickerMessage messageForNotification: ntfn];

  // decide on whether we're scrolled to the end of the messages
  NSPoint containerOrigin = [tickerMessagesTextView textContainerOrigin];
  NSRect visibleRect = NSOffsetRect ([tickerMessagesTextView visibleRect], 
                                     -containerOrigin.x, -containerOrigin.y);
  NSRect tickerMessagesRect = [tickerMessagesTextView bounds];
  
  BOOL wasScrolledToEnd = 
    bottomY (visibleRect) == bottomY (tickerMessagesRect);

  NSDateFormatter *dateFormatter = [[NSDateFormatter new] autorelease];
  [dateFormatter setDateStyle: NSDateFormatterShortStyle];
  [dateFormatter setTimeStyle: NSDateFormatterMediumStyle];  

  // define display attributes
  NSDictionary *replyLinkAttrs = 
    [NSDictionary dictionaryWithObject: message forKey: NSLinkAttributeName];
     
  NSDictionary *lowlightAttrs = 
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

  NSDictionary *fontAttrs = 
    [NSDictionary dictionaryWithObject: 
       [NSFont fontWithName: @"Lucida Grande" size: 11] 
                     forKey: NSFontAttributeName];
  
  NSRange range;
  
  range.location = [[tickerMessagesTextView textStorage] length];
  range.length = 0;
  
  // start new line for all but first message
  if (range.location > 0)
  {
   [[tickerMessagesTextView textStorage] 
     replaceCharactersInRange: range 
     withAttributedString: attributedString (@"\n", lowlightAttrs)];
  }
  
  // build formatted message
  NSMutableAttributedString *displayedMessage = 
    [[NSMutableAttributedString new] autorelease]; 

  [displayedMessage appendAttributedString: 
    attributedString ([dateFormatter stringFromDate: message->receivedAt], 
                      lowlightAttrs)];
  
  [displayedMessage 
    appendAttributedString: attributedString (@": ", lowlightAttrs)];
  
  // save start of actual message (minus date)
  range.location = [displayedMessage length];

  [displayedMessage appendAttributedString: 
    attributedString (message->group, groupAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString (@": ", lowlightAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString (message->from, fromAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString (@": ", lowlightAttrs)];
  
  [displayedMessage appendAttributedString: 
    attributedString (message->message, messageAttrs)];
  
  // create link to message
  range.length = [displayedMessage length] - range.location;
  
  [displayedMessage addAttributes: replyLinkAttrs range: range];
  
  if (message->url)
  {    
    NSDictionary *linkAttrs = 
      [NSDictionary dictionaryWithObjectsAndKeys:
       [NSColor blueColor], NSForegroundColorAttributeName, 
       [NSNumber numberWithBool: NO], NSUnderlineStyleAttributeName,
       message->url, NSLinkAttributeName, nil];

    [displayedMessage 
      appendAttributedString: attributedString (@" (", lowlightAttrs)];

    if ([[message->url absoluteString] length] <= 70)
    {
      [displayedMessage appendAttributedString: 
        attributedString ([message->url absoluteString], linkAttrs)];
    } else
    {
      // display truncated URL
      [displayedMessage appendAttributedString: 
        attributedString ([message->url scheme], linkAttrs)];

      [displayedMessage appendAttributedString: 
        attributedString (@"://", linkAttrs)];
                  
      [displayedMessage appendAttributedString: 
        attributedString ([message->url host], linkAttrs)];

      [displayedMessage appendAttributedString: 
        attributedString (@"/...", linkAttrs)];
    }           
    
    [displayedMessage 
      appendAttributedString: attributedString (@")", lowlightAttrs)];
  }
  
  // set font
  [displayedMessage addAttributes: fontAttrs 
                            range: NSMakeRange (0, [displayedMessage length])];
  
  // append text
  [[tickerMessagesTextView textStorage] 
    replaceCharactersInRange: 
      NSMakeRange ([[tickerMessagesTextView textStorage] length], 0) 
    withAttributedString: displayedMessage];
  
  // scroll to end if that's how it was when we started
  if (wasScrolledToEnd)
  {
    // TODO: bug: very rarely, this segfaults. perhaps something to do with
    // insertion point?
    [tickerMessagesTextView displayIfNeeded];
    [tickerMessagesTextView scrollRangeToVisible: 
      NSMakeRange ([[tickerMessagesTextView textStorage] length], 0)];
  }
  
  // Growl can sometimes pause - do last
  [self notifyGrowlOnTickerMessage: message];
}

- (void) notifyGrowlOnElvinStatusChange: (NSString *) status
{
  NSString *message = 
    [NSString stringWithFormat: @"Elvin %@ (%@)", 
      status, [elvin elvinUrl]];
  
  [GrowlApplicationBridge
    notifyWithTitle: @"Elvin Connection"
    description: message  
    notificationName: @"Connection Status"
    iconData: nil priority: 1 isSticky: NO clickContext: nil];
}

- (void) notifyGrowlOnTickerMessage: (TickerMessage *) message
{
  NSString *description =
   [NSString stringWithFormat: @"%@: %@", message->from, message->message];
 
  // Growl should really handle long messages...
  if ([description length] > MAX_GROWL_MESSAGE_LENGTH)
  {
    description = 
      [NSString stringWithFormat: @"%@...", 
        [description substringToIndex: MAX_GROWL_MESSAGE_LENGTH]];
  }
  
  const NSString *userName = prefString (PrefOnlineUserName);
  NSString *type;
  int priority = 0;
  BOOL sticky = NO;
  
  if ([message->group isEqual: userName])
  {
    type = @"Personal Message";
    
    if (![message->from isEqual: userName])
    {
      priority = 1;
      sticky = YES;
    }
  } else
  {
    type = @"Ticker Message";
  }
  
  [GrowlApplicationBridge
    notifyWithTitle: type
    description: description notificationName: type
    iconData: nil priority: priority isSticky: sticky clickContext: nil];
}

#pragma mark -

- (NSString *) subscription
{
  return subscription;
}

- (void) setSubscription: (NSString *) newSubscription
{
  if (![newSubscription isEqual: subscription])
  {
    [subscription release];
    
    subscription = [newSubscription retain];
    
    NSString *fullSubscription;

    if ([subscription length] == 0)
    {
      fullSubscription = TICKER_SUBSCRIPTION;
    } else
    {
      fullSubscription = 
        [NSString stringWithFormat: @"%@ && (%@)", 
          TICKER_SUBSCRIPTION, subscription];
    }
    
    TRACE (@"Full ticker subscription: %@", fullSubscription);
    
    if (subscriptionContext)
    {
      [elvin resubscribe: subscriptionContext 
        usingSubscription: fullSubscription];
    } else
    {
      subscriptionContext = 
        [elvin subscribe: fullSubscription withDelegate: self 
          usingSelector: @selector (handleNotify:)];
    }
  }
}

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
  // TODO error on closed connection
  NSAssert (messageGroup != nil && messageText != nil, @"IB connection failure");
    
  NSString *message = [messageText string];

  // check for empty text
  if (sender != self && [trim (message) length] == 0)
  {
    // make self the delegate to put sheet under text editor
    // [[messageText window] setDelegate: self];
    
    NSBeginAlertSheet 
      (@"The message text is empty.", @"Don't Send", 
       @"Send Empty Message", nil,  [messageText window], self, 
       @selector (emptyMessageCheckDidEnd:returnCode:contextInfo:), nil, nil, 
       @"Send the message anyway?");
    
    return;
  }
  
  [elvin sendTickerMessage: message 
    fromSender: prefString (PrefOnlineUserName)
    toGroup: [messageGroup stringValue] 
    inReplyTo: self.inReplyTo 
    attachedURL: self.attachedURL
    sendPublic: self.allowPublic
    sendInsecure: self.allowInsecure];

  self.attachedURL = nil;
  self.inReplyTo = nil;
  self.allowPublic = NO;
  self.allowInsecure = YES;
  
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
  self.allowInsecure = YES;
}

- (IBAction) togglePublic: (id) sender
{
  self.allowPublic = !self.allowPublic;
}

- (IBAction) toggleSecure: (id) sender
{
  self.allowInsecure = !self.allowInsecure;
}

@synthesize inReplyTo;

@synthesize allowPublic;

@synthesize allowInsecure;

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
  self.attachedURL = [NSURL URLFromPasteboard: [sender draggingPasteboard]];
  
  return YES;
}

#pragma mark Text view delegates

/*
 * Delegate override to enable message text field to support TAB out but still
 * allow Enter/Return to insert new lines.
 */
- (BOOL) textView: (NSTextView *) textView doCommandBySelector: (SEL) command
{
  if (command == @selector (insertTab:))
  {
    if (([[NSApp currentEvent] modifierFlags] & NSAlternateKeyMask) != 0)
    {
      [textView insertTabIgnoringFieldEditor: self];
      
      return YES;
    } else
    {
      return NO;
    }
  } else if (command == @selector (insertNewline:))
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
    TickerMessage *message = link;
    
    [messageGroup setStringValue: message->group];
    
    self.inReplyTo = message->messageId;
    self.allowPublic = message->public;
    self.allowInsecure = !message->secure;
    
    [[messageText window] makeFirstResponder: messageText];

    return YES;
  } else
  {
    return NO;
  }
}

/**
 * Generate tooltips for links in the ticker text display. Delegated from 
 * TextViewWithLinks.
 */
- (NSString *) toolTipForLink: (id) link view: (NSTextView *) view
{
  if ([link isKindOfClass: [TickerMessage class]])
  {
    TickerMessage *message = link;
    
    return [NSString stringWithFormat: @"Public: %@\nSecure: %@\nClient: %@", 
              message->public ? @"Yes" : @"No",
              message->secure ? @"Yes" : @"No",
              message->userAgent ? message->userAgent : @"Unknown"];
  } else
  {
    return [link description];
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