#import "Preferences.h"

#import "utils.h"

NSString *PrefOnlineUserName        = @"OnlineUserName";
NSString *PrefOnlineUserUUID        = @"OnlineUserUUID";
NSString *PrefElvinURL              = @"ElvinURL";
NSString *PrefDefaultSendGroup      = @"DefaultSendGroup";
NSString *PrefTickerGroups          = @"TickerGroups";
NSString *PrefPresenceGroups        = @"PresenceGroups";
NSString *PrefPresenceBuddies       = @"PresenceBuddies";
NSString *PrefTickerSubscription    = @"TickerSubscription";
NSString *PrefPresenceColumnSorting = @"PresenceColumnSorting";
NSString *PrefElvinKeys             = @"Keys";

NSString *PresenceDefaultsDidChangeNotification = @"PresenceDefaultsChanged";
NSString *PresenceDefaultsWillChangeNotification = @"PresenceDefaultsWillChange";
NSString *ElvinDefaultsWillChangeNotification = @"ElvinDefaultsWillChange";
NSString *ElvinDefaultsDidChangeNotification = @"ElvinDefaultsDidChange";

NSString *defaultUserName ()
{ 
  NSString *device = [[UIDevice currentDevice] name];
  NSRange iphonePostfix = 
    [device rangeOfString: @"’s iPhone"];
  NSString *name;
  
  if (iphonePostfix.location == NSNotFound)
    name = @"Tinkle User";
  else
    name = [device substringToIndex: iphonePostfix.location];

  return [NSString stringWithFormat: @"%@@iPhone", name];
}

void registerUserDefaults ()
{
  NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
  NSMutableDictionary *defaults = [NSMutableDictionary dictionary];

  // assign user a UUID if needed
  if (![preferences objectForKey: PrefOnlineUserUUID])
    [preferences setObject: uuidString () forKey: PrefOnlineUserUUID];

  if (![preferences objectForKey: PrefOnlineUserName])
    [defaults setObject: defaultUserName () forKey: PrefOnlineUserName];
  
  [defaults setObject: @"elvin://public.elvin.org" forKey: PrefElvinURL];
  [defaults setObject: @"Chat" forKey: PrefDefaultSendGroup];  
  [defaults setObject: [NSArray arrayWithObjects: @"Chat", @"Test", @"News", nil] 
               forKey: PrefTickerGroups];
  [defaults setObject: [NSArray arrayWithObject: @"elvin"] 
            forKey: PrefPresenceGroups];
  [defaults setObject: [NSArray array] forKey: PrefPresenceBuddies];
  [defaults setObject: @"" forKey: PrefTickerSubscription];
  
  [preferences registerDefaults: defaults];
}