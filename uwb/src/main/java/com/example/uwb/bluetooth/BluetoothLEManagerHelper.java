package com.example.uwb.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.uwb.bluetooth.model.BluetoothLERemoteDevice;
import com.example.uwb.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 08:44
 */
public class BluetoothLEManagerHelper {

    private static final String TAG = BluetoothLEManagerHelper.class.getSimpleName();

    /// MK UWB Kit defined UUIDs
    protected static UUID serviceUUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    protected static UUID rxCharacteristicUUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    protected static UUID txCharacteristicUUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    protected static UUID descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private Listener mListener;

    // List with all connected Bluetooth LE Remote Devices
    private HashMap<String, BluetoothLERemoteDevice> mBluetoothLERemoteDeviceList = new HashMap<>();

    public interface Listener {
        void onBluetoothLEStateChanged(int state);

        void onBluetoothLEDeviceBonded(String name, String address);

        void onBluetoothLEDeviceScanned(String name, String address);

        void onBluetoothLEDeviceConnected(String name, String address);

        void onBluetoothLEDeviceDisconnected(String address);

        void onBluetoothLEDataReceived(String address, byte[] data);

    }

    public BluetoothLEManagerHelper(Context context) {
        this.mContext = context;

        this.mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();
        this.mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(bondStateChangeReceiver, filter);

        startPinBroadcastReceiver();
    }

    public void registerListener(Listener listener) {
        this.mListener = listener;

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(bluetoothStateChangeReceiver, filter);
    }

    public void unregisterListener() {
        this.mListener = null;
        try {
            mContext.unregisterReceiver(bondStateChangeReceiver);
            mContext.unregisterReceiver(bluetoothStateChangeReceiver);
            mContext.unregisterReceiver(pinAndBondingBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


    // Helper Methods

    public boolean isSupported() {
        return mBluetoothAdapter != null;
    }

    public boolean isEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public boolean isBondedDevice(String remoteAddress) {
        Log.d(TAG, "isBondedDevice: " + remoteAddress);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice bondedDevice : bondedDevices) {
                if (remoteAddress.equals(bondedDevice.getAddress())) {
                    return true;
                }
            }

            return false;
        } else {
            Log.e(TAG, "Missing required permission to scan for BLE devices");
            return false;
        }
    }

    public boolean startLeDeviceScan() {
        Log.d(TAG, "Bluetooth starting LE Scanning");

        if (mBluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner not initialized");
            return false;
        }

        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUUID)).build();
        filters.add(filter);

        ScanSettings.Builder settings = new ScanSettings.Builder();
        settings.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        settings.setReportDelay(0);

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Bluetooth SCAN successfully started");
            mBluetoothLeScanner.startScan(filters, settings.build(), scanCallback);
            return true;
        } else {
            Log.e(TAG, "Missing required permission to scan for BLE devices");
            return false;
        }
    }

    public boolean stopLeDeviceScan() {
        Log.d(TAG, "Bluetooth stopping LE Scanning");

        if (mBluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner not initialized");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Bluetooth SCAN successfully stopped");
            mBluetoothLeScanner.flushPendingScanResults(scanCallback);
            mBluetoothLeScanner.stopScan(scanCallback);
            return true;
        } else {
            Log.e(TAG, "Missing required permission to scan for BLE devices");
            return false;
        }
    }

    public boolean connect(String address) {
        Log.d(TAG, "Proceed to connect to device: " + address);

        if (mBluetoothAdapter == null || address == null) {
            Log.e(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.e(TAG, "Device not found. Unable to connect.");
            return false;
        } else {
            close(address);


            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                device.connectGatt(mContext, false, mGattCallback);
                return true;
            } else {
                Log.e(TAG, "Missing required permission to connect to device");
                return false;
            }
        }
    }

    public boolean transmit(String address, byte[] data) {
        Log.d(TAG, "Proceed to transmit to " + address + " Data: in Hex " + Utils.byteArrayToHexString(data) + " Data: in Hex " + Arrays.toString(Utils.hexStringtoByteArray(Utils.byteArrayToHexString(data))));

        BluetoothLERemoteDevice bluetoothLERemoteDevice = mBluetoothLERemoteDeviceList.get(address);
        if (bluetoothLERemoteDevice == null || bluetoothLERemoteDevice.getBluetoothGatt() == null || bluetoothLERemoteDevice.getRxCharacteristic() == null) {
            Log.e(TAG, "BluetoothGatt not initialized or uninitialized characteristic for this device.");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLERemoteDevice.getRxCharacteristic().setValue(data);
            bluetoothLERemoteDevice.getRxCharacteristic().setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

            return bluetoothLERemoteDevice.getBluetoothGatt().writeCharacteristic(bluetoothLERemoteDevice.getRxCharacteristic());
        } else {
            Log.e(TAG, "Missing required permission to write characteristic");
            return false;
        }
    }

    private void startPinBroadcastReceiver() {
        Log.d(TAG, "startPinBroadcastReceiver: ");
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        mContext.registerReceiver(pinAndBondingBroadcastReceiver, intentFilter);
    }

    BroadcastReceiver pinAndBondingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "pinAndBondingBroadcastReceiver: " + intent.getAction());
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothDevice.setPin("999999".getBytes());
                abortBroadcast();
            }
        }
    };

    // Device scan callback.
    private final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                onScan(result.getDevice().getName(), result.getDevice().getAddress());
            } else {
                Log.e(TAG, "Missing required permission to get scanned device info");
            }
        }
    };

    // Gatt callback
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "BluetoothGattCallback onConnectionStateChange. Status: " + status + " State: " + newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    BluetoothLERemoteDevice bluetoothLERemoteDevice = new BluetoothLERemoteDevice();
                    bluetoothLERemoteDevice.setBluetoothGatt(gatt);

                    // Add this device to the list of connected devices
                    mBluetoothLERemoteDeviceList.put(gatt.getDevice().getAddress(), bluetoothLERemoteDevice);

                    // Look for target Service
                    if (!discoverServices(gatt)) {
                        Log.e(TAG, "Failed to start discover services");
                        close(gatt.getDevice().getAddress());
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    close(gatt.getDevice().getAddress());
                    onDisconnect(gatt.getDevice().getAddress());
                }
            } else {
                close(gatt.getDevice().getAddress());
                onDisconnect(gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "BluetoothGattCallback onServicesDiscovered status: " + status);

            if (!getCharacteristics(gatt)) {
                Log.e(TAG, "Failed to start get characteristics");
                close(gatt.getDevice().getAddress());
            }

            if (!writeDescriptorEnableNotification(gatt)) {
                Log.e(TAG, "Failed to start write descriptor to enable notification");
                close(gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");

            if (!readCharacteristicData(gatt, characteristic)) {
                Log.e(TAG, "Failed to start read characteristic data");
                close(gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite status: " + status);

            // Validate status response
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (!updateMtu(gatt)) {
                    Log.e(TAG, "Failed to start update MTU");
                    close(gatt.getDevice().getAddress());
                }
            } else {
                Log.e(TAG, "Failed to write descriptor");
                close(gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "BluetoothGattCallback onCharacteristicWrite. Status: " + status);

            // Validate status response
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Failed to write characteristic");
                close(gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "BluetoothGattCallback onCharacteristicRead. Status: " + status);

            // Validate status response
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Failed to read characteristic");
                close(gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "BluetoothGattCallback onCharacteristicRead. Status: " + status);

            // Validate status response
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Failed to read descriptor");
                close(gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt,
                                 int mtu,
                                 int status) {
            Log.d(TAG, "BluetoothGattCallback onMtuChanged. Status: " + status);

            // Validate status response
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // We are done establishing the connection
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    onConnect(gatt.getDevice().getName(), gatt.getDevice().getAddress());
                } else {
                    Log.e(TAG, "Missing required permission to get scanned device info");
                }
            } else {
                Log.e(TAG, "Failed to update MTU");
                close(gatt.getDevice().getAddress());
            }
        }
    };

    public boolean close(String address) {
        Log.d(TAG, "Proceed to close connection with device " + address);

        BluetoothLERemoteDevice bluetoothLERemoteDevice = mBluetoothLERemoteDeviceList.get(address);
        if (bluetoothLERemoteDevice == null || bluetoothLERemoteDevice.getBluetoothGatt() == null) {
            Log.e(TAG, "BluetoothGatt not initialized or uninitialized characteristic for this device.");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLERemoteDevice.getBluetoothGatt().close();
            mBluetoothLERemoteDeviceList.remove(address);
            return true;
        } else {
            Log.e(TAG, "Missing required permission to close connection");
            return false;
        }
    }

    private boolean discoverServices(BluetoothGatt bluetoothGatt) {
        Log.d(TAG, "Proceed to discover services");

        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized.");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            return bluetoothGatt.discoverServices();
        } else {
            Log.e(TAG, "Missing required permission to discover services");
            return false;
        }
    }

    private boolean getCharacteristics(BluetoothGatt bluetoothGatt) {
        Log.d(TAG, "Proceed to get characteristics");

        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized.");
            return false;
        }

        BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
        if (service == null) {
            Log.e(TAG, "Service not found");
            return false;
        }

        BluetoothLERemoteDevice bluetoothLERemoteDevice = mBluetoothLERemoteDeviceList.get(bluetoothGatt.getDevice().getAddress());
        if (bluetoothLERemoteDevice == null) {
            Log.e(TAG, "BluetoothLERemoteDevice not found.");
            return false;
        }

        List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = service.getCharacteristics();


        for (int j = 0; j < bluetoothGattCharacteristics.size(); j++) {
            BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattCharacteristics.get(j);
            if (bluetoothGattCharacteristic.getUuid().equals(rxCharacteristicUUID)) {
                Log.i(TAG, "Write characteristic found, UUID is: " + bluetoothGattCharacteristic.getUuid().toString());
                bluetoothLERemoteDevice.setRxCharacteristic(bluetoothGattCharacteristic);
            } else if (bluetoothGattCharacteristic.getUuid().equals(txCharacteristicUUID)) {
                Log.i(TAG, "Notify characteristic found, UUID is " + bluetoothGattCharacteristic.getUuid().toString());
                bluetoothLERemoteDevice.setTxCharacteristic(bluetoothGattCharacteristic);
            }
        }

        return bluetoothLERemoteDevice.getRxCharacteristic() != null && bluetoothLERemoteDevice.getTxCharacteristic() != null;
    }

    private boolean writeDescriptorEnableNotification(BluetoothGatt bluetoothGatt) {
        Log.d(TAG, "Proceed to write descriptor to enable notification");

        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized.");
            return false;
        }

        BluetoothLERemoteDevice bluetoothLERemoteDevice = mBluetoothLERemoteDeviceList.get(bluetoothGatt.getDevice().getAddress());
        if (bluetoothLERemoteDevice == null) {
            Log.e(TAG, "BluetoothLERemoteDevice not found.");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            if (!bluetoothGatt.setCharacteristicNotification(bluetoothLERemoteDevice.getTxCharacteristic(), true)) {
                Log.e(TAG, "Failed setCharacteristicNotification txCharacteristic");
                return false;
            }

            BluetoothGattDescriptor descriptor = bluetoothLERemoteDevice.getTxCharacteristic().getDescriptor(descriptorUUID);
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                return bluetoothGatt.writeDescriptor(descriptor);
            } else {
                Log.e(TAG, "descriptor is null");
                return false;
            }
        } else {
            Log.e(TAG, "Missing required permission to write descriptor to enable notification");
            return false;
        }
    }

    private boolean readCharacteristicData(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (gatt == null || characteristic == null) {
            Log.e(TAG, "BluetoothGatt or BluetoothGattCharacteristic not initialized");
            return false;
        }

        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            Log.d(TAG, "Bluetooth LE Device onDataReceived 1: " + gatt.getDevice().getAddress() + " Data received: HEX String " + Utils.byteArrayToHexString(data));
            Log.d(TAG, "Bluetooth LE Device onDataReceived 2: " + gatt.getDevice().getAddress() + " Data received: Array " + Arrays.toString(data));
            onDataReceived(gatt.getDevice().getAddress(), data);
            return true;
        }

        return false;
    }

    private boolean updateMtu(BluetoothGatt bluetoothGatt) {
        Log.d(TAG, "Proceed to update MTU");

        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized.");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            return bluetoothGatt.requestMtu(247);
        } else {
            Log.e(TAG, "Missing required permission to update MTU");
            return false;
        }
    }


    private final BroadcastReceiver bondStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null && action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Bluetooth bond state changed to " + state + " for device " + device.getAddress());

                switch (state) {
                    case BluetoothDevice.BOND_NONE:
                        Log.i(TAG, "bondStateChangeReceiver: BOND_NONE ");
                    case BluetoothDevice.BOND_BONDING:
                        Log.i(TAG, "bondStateChangeReceiver: BOND_BONDING");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.i(TAG, "bondStateChangeReceiver: BOND_BONDED");
                        // Share bonded device info with the listener
                        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            onBonded(device.getName(), device.getAddress());
                        } else {
                            Log.e(TAG, "Missing required permission to get bonded device info");
                        }

                        break;
                }
            }
        }
    };

    private final BroadcastReceiver bluetoothStateChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                Log.d(TAG, "Bluetooth state changed to " + state);

                onStateChanged(state);
            }
        }
    };


    // Listener methods
    private void onBonded(String name, String address) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onBluetoothLEDeviceBonded(name, address);
            }
        });
    }

    private void onStateChanged(final int state) {
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onBluetoothLEStateChanged(state);
            }
        });
    }

    private void onScan(final String name, final String address) {
        Log.d(TAG, "onScan: Name" + name + " Address " + address);
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onBluetoothLEDeviceScanned(name, address);
            }
        });
    }

    private void onDisconnect(final String address) {
        Log.d(TAG, "onConnect: Address " + address);
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onBluetoothLEDeviceDisconnected(address);
            }
        });
    }

    private void onDataReceived(final String address, final byte[] data) {
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onBluetoothLEDataReceived(address, data);
            }
        });
    }

    private void onConnect(final String name, final String address) {
        Log.d(TAG, "onConnect: Name" + name + " Address " + address);
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onBluetoothLEDeviceConnected(name, address);
            }
        });
    }


}
