#import <Cocoa/Cocoa.h>

@class AppController;

@interface PresenceController : NSWindowController 
{
  AppController * appController;
}

- (id) initWithAppController: (AppController *) theAppController;

@property (readonly, assign) IBOutlet AppController * appController;

@end
