#import "PresenceEntity.h"
#import "PresenceEntityNameCell.h"

@interface PresenceEntityNameCellFormatter : NSFormatter
{
}

@end

@implementation PresenceEntityNameCellFormatter

- (NSString *) stringForObjectValue: (id) value
{
  return [value isKindOfClass: [PresenceEntity class]] ? 
    ((PresenceEntity *)value).name : nil;
}

@end

@implementation PresenceEntityNameCell

- (void) awakeFromNib
{
  [super awakeFromNib];
  
  [self setFormatter: [PresenceEntityNameCellFormatter new]];
}

- (void) setObjectValue: (id <NSCopying>) value
{
  [super setObjectValue: value];

  PresenceStatus *status = ((PresenceEntity *)value).status;
  NSString *imageName;
  
  if (status.statusCode == ONLINE)
    imageName = @"lolly_green.png";
  else if (status.statusCode == OFFLINE)
    imageName = @"lolly_grey.png";
  else
    imageName = @"lolly_yellow.png";
  
  [self setImage: [NSImage imageNamed: imageName]];
}

@end
