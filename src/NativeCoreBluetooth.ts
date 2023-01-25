import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

interface GattServices {
  serviceIds: string[];
  characteristics: string[];
  descriptors: string[];
}

interface CBCharacteristicData {
  uuid: string;
  data: string;
  descriptors?: undefined;
}

interface CBDescriptorData {
  uuid: string;
  data: string;
}

export interface Spec extends TurboModule {
  startScan(): Promise<void>;
  startScanByCompanyId(companyIds: number[]): Promise<void>;
  stopScan(): Promise<void>;
  isDiscovering(): Promise<boolean>;
  checkAdvertisePermission(): Promise<boolean>;
  checkScanPermission(): Promise<boolean>;
  connect(identifier: string): void;
  disconnect(): void;
  discoverServices(): Promise<GattServices>;
  writeCharacteristic(
    serviceId: string,
    uuid: string,
    data: string
  ): Promise<void>;
  readCharacteristic(
    serviceId: string,
    uuid: string
  ): Promise<CBCharacteristicData>;
  writeDescriptor(
    serviceId: string,
    characteristicId: string,
    uuid: string,
    data: string
  ): Promise<void>;
  readDescriptor(
    serviceId: string,
    characteristicId: string,
    uuid: string
  ): Promise<CBDescriptorData>;
  requestAdvertisePermission(): Promise<void>;
  requestScanPermissions(): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('CoreBluetooth');
