
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNCoreBluetoothSpec.h"

@interface CoreBluetooth : NSObject <NativeCoreBluetoothSpec>
#else
#import <React/RCTBridgeModule.h>

@interface CoreBluetooth : NSObject <RCTBridgeModule>
#endif

@end
