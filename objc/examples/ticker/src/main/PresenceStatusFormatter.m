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
  
//  NSDictionary *unavailableAttrs = 
//    [NSDictionary dictionaryWithObject: color (102, 102, 102) 
//                                forKey: NSForegroundColorAttributeName];
  
  NSDictionary *availableAttrs = 
    [NSDictionary dictionaryWithObject: color (0, 128, 64) 
                                forKey: NSForegroundColorAttributeName];
  
  NSDictionary *durationAttrs = 
    [NSDictionary dictionaryWithObject: color (153, 153, 153) 
                                forKey: NSForegroundColorAttributeName];
  
  NSMutableAttributedString *string = 
    [[NSMutableAttributedString new] autorelease]; 
  
  [string appendAttributedString: attributedString (status.statusText, availableAttrs)];
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
