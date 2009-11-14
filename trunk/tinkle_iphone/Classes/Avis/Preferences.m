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

static NSString *computerName ()
{
  NSDictionary *systemPrefs = 
    [NSDictionary dictionaryWithContentsOfFile: 
      @"/Library/Preferences/SystemConfiguration/preferences.plist"];
      
  NSString *computerName = 
    [systemPrefs valueForKeyPath: @"System.System.ComputerName"];
    
  return computerName ? computerName : @"sticker";
}

void registerUserDefaults ()
{
  NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
  NSMutableDictionary *defaults = [NSMutableDictionary dictionary];

  // assign user a UUID if needed
  if (![preferences objectForKey: PrefOnlineUserUUID])
    [preferences setObject: uuidString () forKey: PrefOnlineUserUUID];

  if (![preferences objectForKey: PrefOnlineUserName])
  {
    [defaults setObject: [NSString stringWithFormat: @"%@@%@", 
                          NSFullUserName (), computerName ()] 
      forKey: PrefOnlineUserName];
  }
  
  [defaults setObject: @"elvin://public.elvin.org" forKey: PrefElvinURL];
  [defaults setObject: @"Chat" forKey: PrefDefaultSendGroup];  
  [defaults setObject: [NSArray arrayWithObject: @"Chat"] 
               forKey: PrefTickerGroups];
  [defaults setObject: [NSArray arrayWithObject: @"elvin"] 
            forKey: PrefPresenceGroups];
  [defaults setObject: [NSArray array] forKey: PrefPresenceBuddies];
  [defaults setObject: @"" forKey: PrefTickerSubscription];
  
  [preferences registerDefaults: defaults];
}
