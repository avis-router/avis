#import "utils.h"

#import "TickerController.h"
#import "TickerMessage.h"
#import "RolloverButton.h"
#import "ElvinConnection.h"
#import "PresenceController.h"
#import "PresenceEntity.h"
#import "Preferences.h"

#define MAX_RECENT_MESSAGES 100

NSString * TickerMessageReceivedNotification = 
  @"TickerMessageReceivedNotification";

NSString * TickerMessageStartedEditingNotification = 
  @"TickerMessageStartedEditingNotification";

NSString * TickerMessageStoppedEditingNotification = 
  @"TickerMessageStoppedEditingNotification";

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

static NSURL *urlFromClipboard ()
{
  NSPasteboard *pasteboard = [NSPasteboard generalPasteboard];
  
  NSURL *url = [NSURL URLFromPasteboard: pasteboard];
  
  if (!url)
    url = [NSURL URLWithString: [pasteboard stringForType: NSStringPboardType]];
  
  return url;
}

static NSAttributedString *attributedString (NSString *string, 
                                             NSDictionary *attrs)
{
  return 
    [[[NSAttributedString alloc] initWithString: string attributes: attrs] 
      autorelease];
}

#pragma mark -

@interface TickerController (PRIVATE)
  - (NSString *) fullTickerSubscription;
  - (void) handleNotify: (NSDictionary *) message;
  - (void) handleElvinOpen: (void *) unused;
  - (void) handleElvinClose: (void *) unused;
  - (void) setAttachedURLPanelHidden: (BOOL) hidden;
  - (BOOL) textView: (NSTextView *) textView doCommandBySelector: (SEL) selector;
  - (BOOL) textView: (NSTextView *) textView
           clickedOnLink: (id) link atIndex: (unsigned) charIndex;
  - (void) emptyMessageCheckDidEnd: (NSAlert *) alert returnCode: (int) code
           contextInfo: (void *) contextInfo;
  - (void) handleSubscribeError: (NSError *) error;
  - (void) mouseEnteredLink: (NSRange) linkRange ofTextView: (NSTextView *) view;
  - (void) mouseExitedLink: (NSRange) linkRange ofTextView: (NSTextView *) view;
  - (id) linkAtIndex: (NSUInteger) index;
  - (NSArray *) visibleMessageLinkRanges;
  - (void) addRecentMessage: (TickerMessage *) message;
  - (TickerMessage *) findRecentMessage: (NSString *) messageId;
@end

@implementation TickerController

- (id) initWithElvin: (ElvinConnection *) theElvinConnection 
        subscription: (NSString *) theSubscription
{
  self = [super initWithWindowNibName: @"TickerWindow"];
  
  if (!self)
    return nil;
  
  elvin = theElvinConnection;
  recentMessages = 
    [[[NSMutableArray alloc] initWithCapacity: MAX_RECENT_MESSAGES] retain];
  self.subscription = theSubscription;
  
  return self;
}

- (void) dealloc
{
  [[NSNotificationCenter defaultCenter] removeObserver: self]; 
  
  [recentMessages release];
  
  [super dealloc];
}

- (void) addRecentMessage: (TickerMessage *) message
{
  while ([recentMessages count] >= MAX_RECENT_MESSAGES)
    [recentMessages removeObjectAtIndex: 0];
  
  [recentMessages addObject: message];
}

- (TickerMessage *) findRecentMessage: (NSString *) messageId
{
  if (messageId)
  {
    for (TickerMessage *message in [recentMessages reverseObjectEnumerator])
    {
      if ([message->messageId isEqual: messageId])
        return message;
    }
  }
  
  return nil;
}

- (TickerMessage *) findRecentMessageFromOther
{
  NSString *user = prefString (PrefOnlineUserName);
  
  for (TickerMessage *message in [recentMessages reverseObjectEnumerator])
  {
    if (![message->from isEqual: user])
      return message;
  }
  
  return nil;
}

#pragma mark -

- (void) awakeFromNib
{
  [tickerMessagesTextView setString: @""];

  self.allowInsecure = YES;
  self.canSend = [elvin isConnected];
  
  tickerIsEditing = NO;
    
  [tickerGroupsController 
    setSortDescriptors: 
     [NSArray arrayWithObject: 
       [[NSSortDescriptor alloc] initWithKey: @"" ascending: YES
        selector: @selector (caseInsensitiveCompare:)]]];
}

- (void) windowDidLoad
{
  [tickerMessagesTextView setLinkTextAttributes: [NSDictionary dictionary]];
  
  self.attachedURL = nil;
  self.inReplyTo = nil;
  
  NSNotificationCenter *notifications = [NSNotificationCenter defaultCenter];
  
  [notifications addObserver: self selector: @selector (handleElvinOpen:)
                        name: ElvinConnectionOpenedNotification object: nil]; 
  
  [notifications addObserver: self selector: @selector (handleElvinClose:)
                        name: ElvinConnectionClosedNotification object: nil]; 
  
  [notifications addObserver: self selector: @selector (handlePresenceUserDoubleClick:)
                        name: PresenceUserWasDoubleClicked object: nil];
  [notifications addObserver:self selector: @selector (tickerBeganEditing:)
                        name: NSTextDidBeginEditingNotification 
                      object: messageText];
  [notifications addObserver:self selector: @selector (tickerEndedEditing:)
                        name: NSTextDidEndEditingNotification 
                      object: messageText];
  
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
  else if (action == @selector (replyToLastMessage:))
    return [self findRecentMessageFromOther] != nil;
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
  } else if (action == @selector (pasteURL:))
  {
    return urlFromClipboard () != nil;
  } else
    return YES;
}

- (void) handleElvinOpen: (void *) unused
{
  if ([[NSThread currentThread] isMainThread])
  {
    self.canSend = YES;
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
  } else
  {
    [self performSelectorOnMainThread: @selector (handleElvinClose:) 
                           withObject: nil waitUntilDone: NO];
  }
}

- (void) handlePresenceUserDoubleClick: (NSNotification *) notification
{
  PresenceEntity *user = [[notification userInfo] valueForKey: @"user"];
  
  [messageGroup setStringValue: user.name];
  [[self window] makeFirstResponder: messageText];
  [[self window] makeKeyAndOrderFront: self];
}

- (void) tickerBeganEditing: (NSNotification *) notification
{
  if (!tickerIsEditing)
  {
    tickerIsEditing = YES;
    
    [[NSNotificationCenter defaultCenter] 
       postNotificationName: TickerMessageStartedEditingNotification 
       object: self userInfo: nil];
  }
}

- (void) tickerEndedEditing: (NSNotification *) notification
{
  if (tickerIsEditing)
  {
    tickerIsEditing = NO;
    
    [[NSNotificationCenter defaultCenter] 
       postNotificationName: TickerMessageStoppedEditingNotification 
       object: self userInfo: nil];
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
  [dateFormatter setDateStyle: NSDateFormatterLongStyle];
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
  
  // save start of actual message
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
    
  // URL
  if (message->url)
  {    
    NSDictionary *linkAttrs = 
      [NSDictionary dictionaryWithObjectsAndKeys:
       [NSColor blueColor], NSForegroundColorAttributeName, 
       [NSNumber numberWithBool: NO], NSUnderlineStyleAttributeName,
       message->url, NSLinkAttributeName, nil];

    [displayedMessage 
      appendAttributedString: attributedString (@" <", lowlightAttrs)];

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
        attributedString (@"/…", linkAttrs)];
    }           
    
    [displayedMessage 
      appendAttributedString: attributedString (@">", lowlightAttrs)];
  }
  
  // date
  TickerMessage *parentMessage = [self findRecentMessage: message->inReplyTo];

  [displayedMessage appendAttributedString: 
    attributedString (@" (", lowlightAttrs)];

  if (parentMessage)
  {
    [displayedMessage 
      appendAttributedString: attributedString (@"↪ ", lowlightAttrs)];
    [displayedMessage 
      appendAttributedString: attributedString (parentMessage->from, lowlightAttrs)];
    [displayedMessage 
      appendAttributedString: attributedString (@", ", lowlightAttrs)];
  }
  
  [displayedMessage appendAttributedString: 
    attributedString ([dateFormatter stringFromDate: message->receivedAt], 
                      lowlightAttrs)];
  [displayedMessage appendAttributedString: 
    attributedString (@")", lowlightAttrs)];

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
    [tickerMessagesTextView displayIfNeeded];
    [tickerMessagesTextView scrollRangeToVisible: 
      NSMakeRange ([[tickerMessagesTextView textStorage] length], 0)];
  }
 
  [self addRecentMessage: message];
  
  [[NSNotificationCenter defaultCenter] 
     postNotificationName: TickerMessageReceivedNotification object: self
     userInfo: [NSDictionary dictionaryWithObject: message forKey: @"message"]];
}

- (void) handleSubscribeError: (NSError *) error
{
  [[messageText window] presentError: error]; 
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
    
    TRACE (@"Full ticker subscription: %@", subscription);
    
    if (subscriptionContext)
    {
      [elvin resubscribe: subscriptionContext 
        usingSubscription: subscription];
    } else
    {
      subscriptionContext = 
        [elvin subscribe: subscription withDelegate: self 
          onNotify: @selector (handleNotify:) 
           onError: @selector (handleSubscribeError:)];
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
  
  NSString *group = [messageGroup stringValue];
  
  [elvin sendTickerMessage: message 
    fromSender: prefString (PrefOnlineUserName)
    toGroup: group 
    inReplyTo: self.inReplyTo 
    attachedURL: self.attachedURL
    sendPublic: self.allowPublic
    sendInsecure: self.allowInsecure];

  self.attachedURL = nil;
  self.inReplyTo = nil;
  self.allowPublic = NO;
  self.allowInsecure = YES;
  
  [messageText setString: @""];

  // flip focus from/to message text to fire start/end editing events
  if ([[messageText window] firstResponder] == messageText)
  {
    [[messageGroup window] makeFirstResponder: messageGroup];
    [[messageText window] makeFirstResponder: messageText];
  }
  
  // add group to groups pref if not there
  NSArray *groups = prefArray (PrefTickerGroups);
  
  if ([group rangeOfString: @"@"].location == NSNotFound &&
      ![groups containsObject: group])
  {
    NSMutableArray *newGroups = [NSMutableArray arrayWithArray: groups];
    
    [newGroups addObject: group];
    
    [[NSUserDefaults standardUserDefaults] 
       setObject: newGroups forKey: PrefTickerGroups];
  }
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

- (IBAction) pasteURL: (id) sender
{
  NSURL *url = urlFromClipboard ();
  
  if (url)
    self.attachedURL = url;
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

- (void) replyToMessage: (TickerMessage *) message
{
  [messageGroup setStringValue: message->group];
  
  self.inReplyTo = message;
  self.allowPublic = message->public;
  self.allowInsecure = !message->secure;
  
  [[messageText window] makeFirstResponder: messageText];
}

- (IBAction) replyToLastMessage: (id) sender
{
  [self replyToMessage: [self findRecentMessageFromOther]];
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

+ (NSSet *) keyPathsForValuesAffectingInReplyToTooltip
{
  return [NSSet setWithObject: @"inReplyTo"];
}

/**
 * Tooltip for the inReplyTo property. Bound to the reply indicator icon.
 */
- (NSString *) inReplyToTooltip
{
  if (inReplyTo)
  {
    NSDateFormatter *dateFormatter = [[NSDateFormatter new] autorelease];
    
    [dateFormatter setDateStyle: NSDateFormatterNoStyle];
    [dateFormatter setTimeStyle: NSDateFormatterMediumStyle];  
    
    return [NSString stringWithFormat: 
            @"Send as reply to %@'s message received at %@", 
            inReplyTo->from, 
            [dateFormatter stringFromDate: inReplyTo->receivedAt]];
  } else
  {
    return nil;
  }
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
    [self replyToMessage: link];
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

- (void) mouseEnteredLink: (NSRange) linkRange ofTextView: (NSTextView *) view
{
  NSTextStorage *text = [tickerMessagesTextView textStorage];
  NSArray *linkRanges = [self visibleMessageLinkRanges];
  
  NSDictionary *threadAttributes = 
    [NSDictionary dictionaryWithObject: color (255, 234, 168)
                                forKey: NSBackgroundColorAttributeName];
  
  TickerMessage *active = [self linkAtIndex: linkRange.location];
  
  // walk forwards down chain to leaf message
  for (NSValue *range in linkRanges)
  {
    TickerMessage *message = 
      [self linkAtIndex: [range rangeValue].location];
    
    if ([message->inReplyTo isEqual: active->messageId])
      active = message;
  }
  
  // go back down chain, highlighting messages in thread
  for (NSValue *range in [linkRanges reverseObjectEnumerator])
  {
    TickerMessage *message = 
      [self linkAtIndex: [range rangeValue].location];
    
    if (message == active || [active->inReplyTo isEqual: message->messageId])
    {
      [text addAttributes: threadAttributes range: [range rangeValue]];
      
      active = message;
    } else
    {
      [text removeAttribute: NSBackgroundColorAttributeName 
                      range: [range rangeValue]];
    }
  }
}

- (void) mouseExitedLink: (NSRange) linkRange ofTextView: (NSTextView *) view
{
  // TODO: scrolled messages won't be affected by this
  NSTextStorage *text = [tickerMessagesTextView textStorage];
  NSArray *linkRanges = [self visibleMessageLinkRanges];
  
  for (NSValue *range in linkRanges)
  {
    [text removeAttribute: NSBackgroundColorAttributeName 
                    range: [range rangeValue]];
  }  
}

/**
 * The link attribute (if any) at the given index in the text view.
 */
- (id) linkAtIndex: (NSUInteger) index
{
  NSTextStorage *text = [tickerMessagesTextView textStorage];
  
  return [text attribute: NSLinkAttributeName atIndex: index 
          effectiveRange: nil];     
}

/**
 * The list of ranges covering TickerMessage links in the visible part
 * of the text view.
 */
- (NSArray *) visibleMessageLinkRanges
{
  NSMutableArray *ranges = [[NSMutableArray new] autorelease];
  
  NSPoint containerOrigin = [tickerMessagesTextView textContainerOrigin];
  NSRect visibleRect = 
  NSOffsetRect ([tickerMessagesTextView visibleRect], 
                -containerOrigin.x, -containerOrigin.y);
  
  NSRange visibleGlyphRange = 
    [[tickerMessagesTextView layoutManager] 
       glyphRangeForBoundingRect: visibleRect 
       inTextContainer: [tickerMessagesTextView textContainer]];
  
  NSRange visibleCharRange = 
    [[tickerMessagesTextView layoutManager] 
       characterRangeForGlyphRange: visibleGlyphRange actualGlyphRange: NULL];
  
  NSRange attrsRange = NSMakeRange (visibleCharRange.location, 0);
  
  while (NSMaxRange (attrsRange) < NSMaxRange (visibleCharRange)) 
  {
    id linkObject = 
      [[tickerMessagesTextView textStorage] 
          attribute: NSLinkAttributeName
          atIndex: NSMaxRange (attrsRange)
          longestEffectiveRange: &attrsRange inRange: visibleCharRange];
    
    if ([linkObject isKindOfClass: [TickerMessage class]])
      [ranges addObject: [NSValue valueWithRange: attrsRange]];
  }
  
  return ranges;
}

#pragma mark "Empty Text" sheet delegates

- (void) emptyMessageCheckDidEnd: (NSAlert *) alert returnCode: (int) code
         contextInfo: (void *) contextInfo
{
  [[messageText window] setDelegate: nil];
  
  // re-send request: when sender is self the checks are overridden
  if (code != NSAlertDefaultReturn)
    [self sendMessage: self];
}

@end
