package io.luxtud.library.corebluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

import java.util.List;
import java.util.UUID;

abstract class ICoreBluetooth {

  private final static String TAG = "CoreBluetooth";

  private BluetoothAdapter mBluetoothAdapter;

  private final Context mContext;

  ICoreBluetooth(Context context) {
    mContext = context;
    BluetoothManager mBluetoothManager =
      (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

    // check if BluetoothManager is null
    // initialize BluetoothAdapter from BluetoothManager
    if (mBluetoothManager != null) {
      mBluetoothAdapter = mBluetoothManager.getAdapter();
      if (mBluetoothAdapter == null) {
        Log.i(TAG, "Unable to initialize BluetoothAdapter");
      }
    } else {
      Log.i(TAG, "Unable to initialize BluetoothManager");
    }
  }

  abstract void startScan(List<ScanFilter> filters, Promise promise);

  abstract boolean isDiscovering();

  abstract void stopScan(Promise promise);

  abstract void connect(BluetoothDevice device, Promise promise);

  abstract void disconnect(Promise promise);

  abstract void discoverServices(Promise promise);

  abstract void writeCharacteristic(UUID serviceId, UUID uuid, byte[] data, Promise promise);

  abstract void readCharacteristic(UUID serviceId, UUID uuid, Promise promise);

  abstract void setNotifyCharacteristic(UUID serviceId, UUID uuid, boolean enable, Callback callback);

  abstract void writeDescriptor(UUID serviceId, UUID characteristicId, UUID uuid, byte[] data, Promise promise);

  abstract void readDescriptor(UUID serviceId, UUID characteristicId, UUID uuid, Promise promise);

  abstract void requestAdvertisePermission(Activity activity);

  abstract void requestScanPermission(Activity activity);

  protected String[] getAdvertisePermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return new String[]{
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
        android.Manifest.permission.BLUETOOTH_CONNECT
      };
    } else {
      return new String[]{};
    }
  }

  protected BluetoothAdapter getBluetoothAdapter() {
    return mBluetoothAdapter;
  }

  protected String[] getScanPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return new String[]{
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.ACCESS_FINE_LOCATION
      };
    } else {
      return new String[]{
        android.Manifest.permission.ACCESS_FINE_LOCATION
      };
    }
  }

  protected boolean checkAdvertisePermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return ActivityCompat.checkSelfPermission(
        mContext,
        android.Manifest.permission.BLUETOOTH_ADVERTISE
      ) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  protected boolean checkScanPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return ActivityCompat.checkSelfPermission(
        mContext,
        android.Manifest.permission.BLUETOOTH_SCAN
      ) == PackageManager.PERMISSION_GRANTED;
    } else {
      return ActivityCompat.checkSelfPermission(
        mContext,
        android.Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED;
    }
  }
}
