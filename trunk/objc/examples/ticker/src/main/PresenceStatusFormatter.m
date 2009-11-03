#import "PresenceStatusFormatter.h"

#import "RelativeDateFormatter.h"
#import "PresenceEntity.h"

static inline NSColor *color (float r, float g, float b)
{
  return [NSColor colorWithCalibratedRed: r/255.0 green: g/255.0 blue: b/255.0 
                                   alpha: 1];
}

static NSAttributedString *attributedString (NSString *string, 
                                             NSDictionary *attrs)
{
  return 
  [[[NSAttributedString alloc] initWithString: string attributes: attrs] 
   autorelease];
}

@implementation PresenceStatusFormatter

- (id) init
{
  if (!(self = [super init]))
    return nil;
  
  durationFormatter = [[RelativeDateFormatter alloc] init];
  
  return self;
}

- (void) dealloc
{
  [durationFormatter release];
  
  [super dealloc];
}

- (NSString *) stringForObjectValue: (id) value
{
  if (![value isKindOfClass: [PresenceEntity class]])
    return nil;

  return [value name];
}

- (NSAttributedString *) attributedStringForObjectValue: (id) value
    withDefaultAttributes: (NSDictionary *) defaultAttrs
{
  PresenceStatus *status = value; 
  NSDictionary *statusAttrs;
  
  if (status.statusCode == ONLINE)
  {
    statusAttrs = 
      [NSDictionary dictionaryWithObject: color (0, 128, 64) 
                                  forKey: NSForegroundColorAttributeName];
  } else if (status.statusCode == MAYBE_UNAVAILABLE || 
             status.statusCode == COFFEE)
  {
    statusAttrs = 
      [NSDictionary dictionaryWithObject: color (217, 153, 0) 
                                  forKey: NSForegroundColorAttributeName];
  } else
  {
    statusAttrs = 
      [NSDictionary dictionaryWithObject: color (102, 102, 102) 
                                  forKey: NSForegroundColorAttributeName];
    
  }
  
  NSFont *baseFont = [defaultAttrs valueForKey: NSFontAttributeName];
  NSFont *durationFont = 
    [[NSFontManager sharedFontManager] 
      convertFont: baseFont toSize: ([baseFont pointSize] - 2)];
  
  NSDictionary *durationAttrs = 
    [NSDictionary dictionaryWithObjectsAndKeys: 
     color (153, 153, 153), NSForegroundColorAttributeName,
     durationFont, NSFontAttributeName, nil];
  
  NSMutableAttributedString *string = 
    [[NSMutableAttributedString new] autorelease]; 
  
  [string appendAttributedString: attributedString (status.statusText, statusAttrs)];
  [string appendAttributedString: attributedString (@" (", durationAttrs)];
  [string appendAttributedString: 
    attributedString ([durationFormatter stringForObjectValue: status.changedAt], durationAttrs)];
  [string appendAttributedString: attributedString (@")", durationAttrs)];
  
  NSMutableParagraphStyle *paraStyle = 
    [[[NSParagraphStyle defaultParagraphStyle] mutableCopy] autorelease];
  
  [paraStyle setLineBreakMode: NSLineBreakByTruncatingTail];
  
  NSDictionary *paraAttrs = 
    [NSDictionary dictionaryWithObject: 
      paraStyle forKey: NSParagraphStyleAttributeName];
  
  [string addAttributes: paraAttrs range: NSMakeRange (0, [string length])];
  
  return string;
}

@end
