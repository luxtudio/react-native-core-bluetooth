import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package '@luxtudio/core-bluetooth' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// @ts-expect-error
const isTurboModuleEnabled = global.__turboModuleProxy != null;

const CoreBluetoothModule = isTurboModuleEnabled
  ? require('./NativeCoreBluetooth').default
  : NativeModules.CoreBluetooth;

const CoreBluetooth = CoreBluetoothModule
  ? CoreBluetoothModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function multiply(a: number, b: number): Promise<number> {
  return CoreBluetooth.multiply(a, b);
}
