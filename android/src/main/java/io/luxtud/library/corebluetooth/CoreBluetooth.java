package io.luxtud.library.corebluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.SparseArray;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.collection.LongSparseArray;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CoreBluetooth extends ICoreBluetooth {
  public static final int PERMISSION_SCAN_RESULT_CODE = 5001;
  public static final int PERMISSION_ADVERTISE_RESULT_CODE = 5002;

  private final Context mContext;

  private BluetoothGatt mGatt;
  private final BluetoothGattCallback mBluetoothGattCallback;
  private final ScanCallback mScanCallback;

  private Map<String, BluetoothDevice> devices;
  private Map<String, Callback> callbacks;
  private List<Integer> companyIds;
  private DeviceEventManagerModule.RCTDeviceEventEmitter mEmitter;

  private Promise mConnectPromise;
  private Promise mDisconnectPromise;
  private Promise mDiscoverServicesPromise;
  private Promise mReadCharacteristicPromise;
  private Promise mWriteCharacteristicPromise;
  private Promise mReadDescriptorPromise;
  private Promise mWriteDescriptorPromise;

  CoreBluetooth(Context context) {
    super(context);

    mContext = context;
    devices = new HashMap<>();
    callbacks = new HashMap<>();
    companyIds = new ArrayList<>();
    mBluetoothGattCallback = createBluetoothGattCallback();
    mScanCallback = createBluetoothScanCallback();
    mReadCharacteristicPromise = null;
    mWriteCharacteristicPromise = null;
    mReadDescriptorPromise = null;
    mWriteDescriptorPromise = null;
  }

  public void setCompanyIds(List<Integer> companyIds) {
    this.companyIds = companyIds;
  }

  public void setEmitter(DeviceEventManagerModule.RCTDeviceEventEmitter emitter) {
    mEmitter = emitter;
  }

  public BluetoothDevice getDevice(String identifier) {
    return devices.get(identifier);
  }

  private void clearDevices() {
    devices = new HashMap<>();
  }

  private void clearCallbacks() {
    callbacks = new HashMap<>();
  }

  @Override
  @SuppressLint("MissingPermission")
  void startScan(List<ScanFilter> filters, Promise promise) {
    if (getBluetoothAdapter() == null) {
      promise.reject("E_BLUETOOTH_ADAPTER_NOT_INITIALIZED", "Unable to initialize BluetoothAdapter.");
    }

    if (checkScanPermission()) {
      if (getBluetoothAdapter().isDiscovering()) {
        promise.reject("E_BLUETOOTH_HAS_SCANNED", "Bluetooth already scanned.");
      }

      ScanSettings scanSettings = new ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build();
      if (companyIds.size() > 0) {
        for (Integer companyId : companyIds) {
          filters.add(new ScanFilter.Builder().setManufacturerData(companyId, new byte[] {}, new byte[] {}).build());
        }
      }

      getBluetoothAdapter().getBluetoothLeScanner().startScan(filters, scanSettings, mScanCallback);

      promise.resolve(null);
    } else {
      promise.reject("E_BLUETOOTH_PERMISSION", "Scan permission not granted.");
    }
  }

  @Override
  @SuppressLint("MissingPermission")
  boolean isDiscovering() {
    if (getBluetoothAdapter() != null) {
      return getBluetoothAdapter().isDiscovering();
    }
    return false;
  }

  @Override
  @SuppressLint("MissingPermission")
  void stopScan(Promise promise) {
    if (getBluetoothAdapter() == null) {
      promise.reject("E_BLUETOOTH_ADAPTER_NOT_INITIALIZED", "Unable to initialize BluetoothAdapter.");
    }

    if (checkScanPermission()) {
      getBluetoothAdapter().getBluetoothLeScanner().stopScan(mScanCallback);
      Toast.makeText(mContext, "블루투스 스캔이 중지되었습니다.", Toast.LENGTH_SHORT).show();
      clearDevices();

      promise.resolve(null);
    } else {
      promise.reject("E_BLUETOOTH_PERMISSION", "Scan permission not granted.");
    }
  }

  @Override
  @SuppressLint("MissingPermission")
  void connect(BluetoothDevice device, Promise promise) {
    if (!checkScanPermission()) {
      promise.reject("E_BLUETOOTH_PERMISSION", "Scan permission not granted.");
      return;
    }

    mConnectPromise = promise;
    device.connectGatt(mContext, false, mBluetoothGattCallback);
  }

  @Override
  @SuppressLint("MissingPermission")
  void disconnect(Promise promise) {
    if (checkScanPermission() && mGatt != null) {
      mDisconnectPromise = promise;
      mGatt.disconnect();
    } else {
      promise.reject("E_BLUETOOTH_DISCONNECT", "Bluetooth not connected.");
    }
  }

  @Override
  @SuppressLint("MissingPermission")
  void discoverServices(Promise promise) {
    if (checkScanPermission() && mGatt != null) {
      mDiscoverServicesPromise = promise;
      mGatt.discoverServices();
    } else {
      promise.reject("E_BLUETOOTH_DISCOVER", "Bluetooth not connected.");
    }
  }

  @Override
  @SuppressLint("MissingPermission")
  void writeCharacteristic(UUID serviceId, UUID uuid, byte[] data, Promise promise) {
    if (checkScanPermission() && mGatt != null) {
      BluetoothGattService service = mGatt.getService(serviceId);
      if (service != null) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
        if (characteristic != null) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mGatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
          } else {
            characteristic.setValue(data);
            mGatt.writeCharacteristic(characteristic);
          }
          mWriteCharacteristicPromise = promise;
        } else {
          promise.reject("E_BLUETOOTH_CHARACTERISTIC_NOT_FOUND", "Characteristic not found.");
        }
      } else {
        promise.reject("E_BLUETOOTH_SERVICE_NOT_FOUND", "Service not found.");
      }
    }
  }

  @Override
  @SuppressLint("MissingPermission")
  void readCharacteristic(UUID serviceId, UUID uuid, Promise promise) {
    if (checkScanPermission() && mGatt != null) {
      BluetoothGattService service = mGatt.getService(serviceId);
      if (service != null) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
        if (characteristic != null) {
          mGatt.readCharacteristic(characteristic);
          mReadCharacteristicPromise = promise;
        } else {
          promise.reject("E_BLUETOOTH_CHARACTERISTIC_NOT_FOUND", "Characteristic not found.");
        }
      } else {
        promise.reject("E_BLUETOOTH_SERVICE_NOT_FOUND", "Service not found.");
      }
    }
  }

  @Override
  @SuppressLint("MissingPermission")
  void setNotifyCharacteristic(UUID serviceId, UUID uuid, boolean enable, Callback callback) {
    if (checkScanPermission() && mGatt != null) {
      BluetoothGattService service = mGatt.getService(serviceId);
      if (service != null) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
        if (characteristic != null) {
          mGatt.setCharacteristicNotification(characteristic, enable);
          if (enable) {
            callbacks.put(uuid.toString(), callback);
          } else {
            callbacks.remove(uuid.toString());
          }
        }
      }
    }
  }

  @Override
  @SuppressLint("MissingPermission")
  void writeDescriptor(UUID serviceId, UUID characteristicId, UUID uuid, byte[] data, Promise promise) {
    if (checkScanPermission() && mGatt != null) {
      BluetoothGattService service = mGatt.getService(serviceId);
      if (service != null) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicId);
        if (characteristic != null) {
          BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
          if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              mGatt.writeDescriptor(descriptor, data);
            } else {
              descriptor.setValue(data);
              mGatt.writeDescriptor(descriptor);
            }
            mWriteDescriptorPromise = promise;
          } else {
            promise.reject("E_BLUETOOTH_DESCRIPTOR_NOT_FOUND", "Descriptor not found.");
          }
        } else {
          promise.reject("E_BLUETOOTH_CHARACTERISTIC_NOT_FOUND", "Characteristic not found.");
        }
      } else {
        promise.reject("E_BLUETOOTH_SERVICE_NOT_FOUND", "Service not found.");
      }
    }
  }

  @Override
  @SuppressLint("MissingPermission")
  void readDescriptor(UUID serviceId, UUID characteristicId, UUID uuid, Promise promise) {
    if (checkScanPermission() && mGatt != null) {
      BluetoothGattService service = mGatt.getService(serviceId);
      if (service != null) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicId);
        if (characteristic != null) {
          BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
          if (descriptor != null) {
            mGatt.readDescriptor(descriptor);
            mReadDescriptorPromise = promise;
          } else {
            promise.reject("E_BLUETOOTH_DESCRIPTOR_NOT_FOUND", "Descriptor not found.");
          }
        } else {
          promise.reject("E_BLUETOOTH_CHARACTERISTIC_NOT_FOUND", "Characteristic not found.");
        }
      } else {
        promise.reject("E_BLUETOOTH_SERVICE_NOT_FOUND", "Service not found.");
      }
    }
  }

  @Override
  void requestAdvertisePermission(Activity activity) {
    if (!checkAdvertisePermission()) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ActivityCompat.requestPermissions(
          activity,
          getAdvertisePermissions(),
          PERMISSION_ADVERTISE_RESULT_CODE
        );
      }
    }
  }

  @Override
  void requestScanPermission(Activity activity) {
    if (!checkScanPermission()) {
      ActivityCompat.requestPermissions(
        activity,
        getScanPermissions(),
        PERMISSION_SCAN_RESULT_CODE
      );
    }
  }

  protected BluetoothGattCallback createBluetoothGattCallback() {
    return new BluetoothGattCallback() {
      @Override
      public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        switch (newState) {
          case BluetoothProfile.STATE_CONNECTED:
            if (status == BluetoothGatt.GATT_SUCCESS) {
              mGatt = gatt;
              if (mConnectPromise != null) {
                mConnectPromise.resolve(null);
              }
            } else {
              mGatt = null;
              clearCallbacks();
              if (mConnectPromise != null) {
                mConnectPromise.reject("E_BLUETOOTH_CONNECTION_ERROR", "Connection error.");
              }
            }
            mConnectPromise = null;
            break;
          case BluetoothProfile.STATE_DISCONNECTED:
            if (status == BluetoothGatt.GATT_SUCCESS) {
              mGatt = null;
              clearCallbacks();
              if (mDisconnectPromise != null) {
                mDisconnectPromise.resolve(null);
              }
            } else {
              if (mDisconnectPromise != null) {
                mDisconnectPromise.reject("E_BLUETOOTH_DISCONNECT_ERROR", "Disconnect error.");
              }
            }
            mDisconnectPromise = null;
            break;
        }
      }

      @Override
      public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
          // resolve promise within data is map of params
          // params is map with key 'serviceIds' and 'characteristics' and 'descriptors'
          // uuid is service uuid and characteristics is array of characteristics
          // each characteristic is characteristic uuid string
          // each descriptor is descriptor uuid string
          // MUST BE check if promise is not null
          if (mDiscoverServicesPromise != null) {
            WritableMap params = Arguments.createMap();
            WritableArray serviceIds = Arguments.createArray();
            WritableArray characteristics = Arguments.createArray();
            WritableArray descriptors = Arguments.createArray();
            for (BluetoothGattService service : gatt.getServices()) {
              serviceIds.pushString(service.getUuid().toString());
              for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                characteristics.pushString(characteristic.getUuid().toString());
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                  descriptors.pushString(descriptor.getUuid().toString());
                }
              }
            }
            params.putArray("serviceIds", serviceIds);
            params.putArray("characteristics", characteristics);
            params.putArray("descriptors", descriptors);
            mDiscoverServicesPromise.resolve(params);
            mDiscoverServicesPromise = null;
          }
        } else {
          // MUST BE check if promise is not null
          if (mDiscoverServicesPromise != null) {
            mDiscoverServicesPromise.reject("E_BLUETOOTH_DISCOVER_SERVICES_FAILED", "Discover services failed.");
            mDiscoverServicesPromise = null;
          }
        }
      }

      @Override
      @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
      public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          // resolve promise data is map
          // with key 'uuid' and 'data'
          // and uuid is characteristic uuid and data is characteristic value
          WritableMap map = Arguments.createMap();
          map.putString("uuid", characteristic.getUuid().toString());
          map.putString("data", Base64.encodeToString(value, Base64.DEFAULT));
          mReadCharacteristicPromise.resolve(map);
        } else {
          mReadCharacteristicPromise.reject("E_BLUETOOTH_READ_CHARACTERISTIC_FAILED", "Read characteristic failed.");
        }
      }

      @Override
      public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          // resolve promise data is map
          // with key 'uuid' and 'data'
          // and uuid is characteristic uuid and data is characteristic value
          WritableMap map = Arguments.createMap();
          map.putString("uuid", characteristic.getUuid().toString());
          map.putString("data", Base64.encodeToString(characteristic.getValue(), Base64.DEFAULT));
          mReadCharacteristicPromise.resolve(map);
        } else {
          mReadCharacteristicPromise.reject("E_BLUETOOTH_READ_CHARACTERISTIC_FAILED", "Read characteristic failed.");
        }
        mReadCharacteristicPromise = null;
      }

      @Override
      public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          mWriteCharacteristicPromise.resolve(null);
        } else {
          mWriteCharacteristicPromise.reject("E_BLUETOOTH_WRITE_CHARACTERISTIC_FAILED", "Write characteristic failed.");
        }
        mWriteCharacteristicPromise = null;
      }

      @Override
      @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
      public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
        // send event 'CharacteristicChanged' with data is map to react native
        // with key 'uuid' and 'data'
        // and uuid is characteristic uuid and data is characteristic value
        WritableMap map = Arguments.createMap();
        map.putString("uuid", characteristic.getUuid().toString());
        map.putString("data", Base64.encodeToString(value, Base64.DEFAULT));

        Callback callback = callbacks.get(characteristic.getUuid().toString());
        if (callback != null) {
          callback.invoke(map);
        }
      }

      @Override
      public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // send event 'CharacteristicChanged' with data is map to react native
        // with key 'uuid' and 'data'
        // and uuid is characteristic uuid and data is characteristic value
        WritableMap map = Arguments.createMap();
        map.putString("uuid", characteristic.getUuid().toString());
        map.putString("data", Base64.encodeToString(characteristic.getValue(), Base64.DEFAULT));

        Callback callback = callbacks.get(characteristic.getUuid().toString());
        if (callback != null) {
          callback.invoke(map);
        }
      }

      @Override
      @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
      public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          // resolve promise data is map
          // with key 'uuid' and 'data'
          // and uuid is descriptor uuid and data is descriptor value
          WritableMap map = Arguments.createMap();
          map.putString("uuid", descriptor.getUuid().toString());
          map.putString("data", Base64.encodeToString(value, Base64.DEFAULT));
          mReadDescriptorPromise.resolve(map);
        } else {
          mReadDescriptorPromise.reject("E_BLUETOOTH_READ_DESCRIPTOR_FAILED", "Read descriptor failed.");
        }
        mReadDescriptorPromise = null;
      }

      @Override
      public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          // resolve promise data is map
          // with key 'uuid' and 'data'
          // and uuid is descriptor uuid and data is descriptor value
          WritableMap map = Arguments.createMap();
          map.putString("uuid", descriptor.getUuid().toString());
          map.putString("data", Base64.encodeToString(descriptor.getValue(), Base64.DEFAULT));
          mReadDescriptorPromise.resolve(map);
        } else {
          mReadDescriptorPromise.reject("E_BLUETOOTH_READ_DESCRIPTOR_FAILED", "Read descriptor failed.");
        }
        mReadDescriptorPromise = null;
      }

      @Override
      public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          mWriteDescriptorPromise.resolve(null);
        } else {
          mWriteDescriptorPromise.reject("E_BLUETOOTH_WRITE_DESCRIPTOR_FAILED", "Write descriptor failed.");
        }
        mWriteDescriptorPromise = null;
      }
    };
  }

  protected ScanCallback createBluetoothScanCallback() {
    return new ScanCallback() {
      @Override
      public void onScanResult(int callbackType, ScanResult result) {
        BluetoothDevice newDevice = result.getDevice();
        ScanRecord record = result.getScanRecord();
        String identifier = UUID.randomUUID().toString();
        String deviceName = record.getDeviceName();
        int RSSI = result.getRssi();
        int TxPowerLevel = record.getTxPowerLevel() == Integer.MIN_VALUE ? 0 : record.getTxPowerLevel();

        boolean is_duplicated = false;
        for (Map.Entry<String, BluetoothDevice> entry : devices.entrySet()) {
          if (entry.getValue().getAddress().equals(newDevice.getAddress())) {
            is_duplicated = true;
            identifier = entry.getKey();
            break;
          }
        }

        if (!is_duplicated) {
          devices.put(identifier, newDevice);
        }

        WritableMap params = Arguments.createMap();
        params.putString("identifier", identifier);
        params.putString("name", deviceName);
        params.putInt("RSSI", RSSI);
        params.putInt("TxPowerLevel", TxPowerLevel);
        params.putNull("ManufacturerSpecificData");

        // get ManufacturerSpecificData if not null with company identifier as companyId
        // and base64 encoded data as bytes if not null
        // SparseArray to React Native Map
        SparseArray<byte[]> manufacturerData = record.getManufacturerSpecificData();
        if (manufacturerData != null) {
          WritableMap manufacturerDataMap = Arguments.createMap();
          for (int i = 0; i < manufacturerData.size(); i++) {
            int companyId = manufacturerData.keyAt(i);
            byte[] bytes = manufacturerData.get(companyId);
            if (companyIds != null) {
              if (companyIds.contains(companyId) && bytes != null) {
                manufacturerDataMap.putString(String.valueOf(companyId), Base64.encodeToString(bytes, Base64.DEFAULT));
              }
            } else {
              if (bytes != null) {
                manufacturerDataMap.putString(String.valueOf(companyId), Base64.encodeToString(bytes, Base64.DEFAULT));
              }
            }
          }
          params.putMap("ManufacturerSpecificData", manufacturerDataMap);
        }

        // emit event check if not null
        if (mEmitter != null) {
          mEmitter.emit("FoundBLEDevice", params);
        }
      }
    };
  }
}
