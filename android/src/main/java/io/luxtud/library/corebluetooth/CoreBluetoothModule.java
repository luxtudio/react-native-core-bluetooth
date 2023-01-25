package io.luxtud.library.corebluetooth;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;

public class CoreBluetoothModule extends CoreBluetoothSpec {
  public static final String NAME = "CoreBluetooth";

  CoreBluetoothModule(ReactApplicationContext context) {
    super(context);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }
}
