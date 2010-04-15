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

- (BOOL) getObjectValue: (id *) obj forString: (NSString *) string 
         errorDescription: (NSString  **) error
{
  return NO;
}

- (NSString *) stringForObjectValue: (id) value
{
  if (![value isKindOfClass: [PresenceStatus class]])
    return nil;
 
  // generate plain text version of attributedStringForObjectValue
  NSAttributedString *strValue = 
    [self attributedStringForObjectValue: value 
       withDefaultAttributes: 
         [NSDictionary dictionaryWithObjectsAndKeys: 
           [NSFont systemFontOfSize: 12], NSFontAttributeName,
           [NSColor selectedTextColor], NSForegroundColorAttributeName, nil]];
  
  return [strValue string];
}

- (NSAttributedString *) attributedStringForObjectValue: (id) value
    withDefaultAttributes: (NSDictionary *) defaultAttrs
{
  if (![value isKindOfClass: [PresenceStatus class]])
    return nil;
  
  PresenceStatus *status = value; 
  NSFont *baseFont = [defaultAttrs valueForKey: NSFontAttributeName];
  NSFont *durationFont = 
    [[NSFontManager sharedFontManager] 
      convertFont: baseFont toSize: ([baseFont pointSize] - 2)];
    
  NSColor *baseForeground = 
    [defaultAttrs valueForKey: NSForegroundColorAttributeName];
  
  BOOL selectedRow = 
    [[baseForeground colorNameComponent] isEqual: 
       @"alternateSelectedControlTextColor"];
  
  NSDictionary *durationAttrs = 
    [NSDictionary dictionaryWithObjectsAndKeys: 
     selectedRow ? baseForeground : color (153, 153, 153), NSForegroundColorAttributeName,
     durationFont, NSFontAttributeName, nil];
  
  NSMutableAttributedString *string = 
    [[NSMutableAttributedString new] autorelease]; 
  
  [string appendAttributedString: attributedString (status.statusText, defaultAttrs)];
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
