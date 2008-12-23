
static inline NSString *prefsString (NSString *name)
{
  return [[NSUserDefaults standardUserDefaults] stringForKey: name];
}