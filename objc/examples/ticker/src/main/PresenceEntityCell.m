#import "PresenceEntityCell.h"

#import "PresenceEntity.h"

@implementation PresenceEntityCell

- (id) init
{
//  if (!(self = [super init]))
//    return nil;
 
    NSLog (@"init");
       
   cell = [[NSButtonCell alloc] initTextCell: @""];
   
   return self;
}

- (NSMethodSignature *) methodSignatureForSelector:(SEL)aSelector
{
  return [cell methodSignatureForSelector:aSelector];
}

- (void) forwardInvocation:(NSInvocation *)anInvocation
{
    [anInvocation setTarget:cell];
    [anInvocation invoke];
    return;
}

//#include <objc/objc-runtime.h>

//- (id) forward: (SEL) sel args: (marg_list) args
//- (id) forward:(SEL) sel :(marg_list) args
//{
//  NSLog (@"forward");
//  /*
//   * Check whether the recipient actually responds to the message. 
//   * This may or may not be desirable, for example, if a recipient
//   * in turn does not respond to the message, it might do forwarding
//   * itself.
//   */
//  if ([cell respondsToSelector:sel]) 
//     return [cell performv: sel : args];
//  else
//     return ["Recipient does not respond"];
//}

- (void) setObjectValue: (id < NSCopying >) value
{  
  PresenceEntity *entity = (PresenceEntity *)value;
 
  NSLog (@"set object value %@", [entity className]);
  
//  if (entity)
//    [self setStringValue: entity.name];
//  else 
//    [self setTitle: @"???"];
//  //[self setTitle: @"title"];
//  
//  [self setRepresentedObject: entity];
 [super setObjectValue: value];
 
 if (entity.name)
    [super setStringValue: entity.name];
}

- (id)valueForUndefinedKey:(NSString *)key
{
  NSLog (@"get value for undef key \"%@\"", key);
  return [self objectValue];
}

- (void)setValue:(id)value forUndefinedKey:(NSString *)key
{
  NSLog (@"set value %@ for key \"%@\"", [value className], key);
//  [self setObjectValue: value];
}

- (void)setValue:(id)value 
{
  NSLog (@"set value");
}

- (id) value
{
  NSLog (@"get value");
  
  return nil;
}

@end
