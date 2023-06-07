package com.example.uwb;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.uwb.RangingCapabilities;
import androidx.core.uwb.RangingResult;


import com.example.uwb.bluetooth.BluetoothLEManagerHelper;
import com.example.uwb.location.LocationManagerHelper;
import com.example.uwb.manager.UwbManagerHelper;
import com.example.uwb.model.Accessory;
import com.example.uwb.model.Position;
import com.example.uwb.model.SelectAccessoriesDialogItem;
import com.example.uwb.oob.OoBTlvHelper;
import com.example.uwb.oob.model.UwbDeviceConfigData;
import com.example.uwb.oob.model.UwbPhoneConfigData;
import com.example.uwb.permissions.PermissionHelper;
import com.example.uwb.sharedpreference.PreferenceStorageHelper;
import com.example.uwb.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 06:03
 */
public class UwbRangingController implements UwbRangingView.Listener, BluetoothLEManagerHelper.Listener, UwbManagerHelper.Listener {

    public static final String TAG = "UwbRangingController";
    private List<Accessory> mAccessoriesList = new ArrayList<>();

    private final BluetoothLEManagerHelper mBluetoothLEManagerHelper;
    private final PreferenceStorageHelper mPreferenceStorageHelper;
    private final UwbManagerHelper mUwbManagerHelper;
    private final LocationManagerHelper mLocationManagerHelper;
    private final PermissionHelper mPermissionHelper;


    private Timer timerBleExpiredAccessories;
    private HashMap<String, Timer> mTimerAccessoriesConnectList = new HashMap<>();
    private HashMap<String, Timer> mTimerAccessoriesLegacyOoBSupportList = new HashMap<>();

    private List<SelectAccessoriesDialogItem> mSelectAccessoryList = new ArrayList<>();

    public UwbRangingController(BluetoothLEManagerHelper mBluetoothLEManagerHelper,
                                PreferenceStorageHelper mPreferenceStorageHelper,
                                UwbManagerHelper mUwbManagerHelper,
                                LocationManagerHelper mLocationManagerHelper, PermissionHelper permissionHelper) {
        this.mBluetoothLEManagerHelper = mBluetoothLEManagerHelper;
        this.mPreferenceStorageHelper = mPreferenceStorageHelper;
        this.mUwbManagerHelper = mUwbManagerHelper;
        this.mLocationManagerHelper = mLocationManagerHelper;
        this.mPermissionHelper = permissionHelper;
    }

    private UWBCallBack uwbCallBack;

    public interface UWBCallBack {
        void uwbRangingStarted();

        void uwbRangingResult(int distance, int angle, int elevation);

        void uwbRangingCapabilities(boolean distanceSupported, boolean angleSupported, boolean elevationSupported);

        void uwbRangingComplete();

        void uwbRangingError();

        void uwbConnectDeviceDetails(String name, String mac, String alias);
    }

    public void registerUwbCallBack(UWBCallBack callBack) {
        this.uwbCallBack = callBack;
    }


    public void initListener() {
        Log.d(TAG, "initListener: *************************************************");
        mBluetoothLEManagerHelper.registerListener(this);
        mUwbManagerHelper.registerListener(this);
    }

    public void deinit() {
        Log.d(TAG, "deinit: ");

        mBluetoothLEManagerHelper.unregisterListener();
        mUwbManagerHelper.unregisterListener();

        bleStopDeviceScan();
        bleClose();
        uwbClose();
        cancelTimerBleExpiredAccessories();
        cancelTimerBleConnect();
        cancelTimerAccessoriesLegacyOoBSupport();
    }


    // UwbRangingView.Listener
    @Override
    public void onBackPressed() {

    }

    @Override
    public void onSelectAccessoryButtonClicked() {
        Log.d(TAG, "onSelectAccessoryButtonClicked: ");
        bleClose();
        uwbClose();
        cancelTimerBleExpiredAccessories();
        cancelTimerBleConnect();
        cancelTimerAccessoriesLegacyOoBSupport();

        //showing paring info as user is interest in it
        if (mPreferenceStorageHelper.getShowPairingInfo()) {
           // TO show the paring info
        } else {
            uwbBleStartScan();
        }

    }

    @Override
    public void onSelectedAccessoryRemoveClicked() {
        Log.d(TAG, "onSelectedAccessoryRemoveClicked: ");
        Accessory accessory = getAccessoryFromIndex(0);
        if (accessory != null) {
            bleClose(accessory);
            uwbClose(accessory);
            cancelTimerBleConnect(accessory);
            cancelTimerAccessoriesLegacyOoBSupport(accessory);

            // Remove accessory from list
            mAccessoriesList.remove(accessory);
        }
    }

    @Override
    public void onSelectedAccessorySelectAccessoryClicked() {
        Log.d(TAG, "onSelectedAccessorySelectAccessoryClicked: ");
        uwbBleStartScan();
    }


    // utility methods
    private boolean bleClose() {
        Log.d(TAG, "bleClose: ");
        for (Accessory accessory : mAccessoriesList) {
            mBluetoothLEManagerHelper.close(accessory.getMac());
        }
        return true;
    }

    private boolean uwbClose() {
        Log.d(TAG, "uwbClose: ");
        for (Accessory accessory : mAccessoriesList) {
            mUwbManagerHelper.close(accessory.getMac());
        }
        return true;
    }

    private void cancelTimerBleExpiredAccessories() {
        Log.d(TAG, "cancelTimerBleExpiredAccessories: ");
        if (timerBleExpiredAccessories != null) {
            timerBleExpiredAccessories.purge();
            timerBleExpiredAccessories.cancel();
        }
    }

    private void cancelTimerBleConnect() {
        Log.d(TAG, "cancelTimerBleConnect: ");
        for (Timer timerAccessoryConnect : mTimerAccessoriesConnectList.values()) {
            timerAccessoryConnect.purge();
            timerAccessoryConnect.cancel();
        }

        mTimerAccessoriesConnectList.clear();
    }

    private void cancelTimerAccessoriesLegacyOoBSupport() {
        Log.d(TAG, "cancelTimerAccessoriesLegacyOoBSupport: ");
        for (Timer timerAccessoryLegacyOoBSupport : mTimerAccessoriesLegacyOoBSupportList.values()) {
            timerAccessoryLegacyOoBSupport.purge();
            timerAccessoryLegacyOoBSupport.cancel();
        }

        mTimerAccessoriesConnectList.clear();
    }

    public void uwbBleStartScan() {
        Log.d(TAG, "uwbBleStartScan: ");
        bleClose();
        uwbClose();
        cancelTimerBleExpiredAccessories();
        cancelTimerBleConnect();
        cancelTimerAccessoriesLegacyOoBSupport();

        mSelectAccessoryList.clear();

        if (bleStartDeviceScan()) {
            // Start timer to handle expired accessories
            //startTimerBleExpiredAccessories();
        }
    }

    // ble start scan
    private boolean bleStartDeviceScan() {
        Log.d(TAG, "bleStartDeviceScan: " + checkPermissions());

        if (!checkPermissions()) {
            // Permission missing
            Log.d(TAG, "bleStartDeviceScan: Permission missing");
            //return false;
        }

        if (!mBluetoothLEManagerHelper.isSupported()) {
            //BLUETOOTH NOT SUPPORTED
            return false;
        }

        if (!mUwbManagerHelper.isSupported()) {
            // UWB NOT SUPPORTED
            return false;
        }

        if (!mLocationManagerHelper.isSupported()) {
            //LOCATION NOT SUPPORTED
            return false;
        }

        if (!mBluetoothLEManagerHelper.isEnabled()) {
            //BLUETOOTH NOT ENABLED
            return false;
        }

        if (!mUwbManagerHelper.isEnabled()) {
            //UWB NOT ENABLED
            return false;
        }

        if (!mLocationManagerHelper.isEnabled()) {
            //LOCATION NOT ENABLED
            return false;
        }

        Log.d(TAG,"BLE_SCAN_START");
        return mBluetoothLEManagerHelper.startLeDeviceScan();
    }

    private boolean bleStopDeviceScan() {
        Log.d(TAG, "BLE_SCAN_STOP ");
        return mBluetoothLEManagerHelper.stopLeDeviceScan();
    }


    private boolean checkPermissions() {
        Log.d(TAG, "checkPermissions: ");
        return mPermissionHelper.hasPermission(Manifest.permission.BLUETOOTH) &&
                mPermissionHelper.hasPermission(Manifest.permission.BLUETOOTH_ADMIN) &&
                mPermissionHelper.hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                mPermissionHelper.hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                mPermissionHelper.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                mPermissionHelper.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                mPermissionHelper.hasPermission(Manifest.permission.UWB_RANGING);
    }

    private void startTimerBleExpiredAccessories() {
        Log.d(TAG, "startTimerBleExpiredAccessories: ");
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                removeBleExpiredAccessories();
            }
        };

        timerBleExpiredAccessories = new Timer();
        timerBleExpiredAccessories.schedule(tt, 1000, 1000);
    }

    private void removeBleExpiredAccessories() {
        Log.d(TAG, "removeBleExpiredAccessories: ");
        List<SelectAccessoriesDialogItem> expiredAccessories = new ArrayList<>();
        for (SelectAccessoriesDialogItem item : mSelectAccessoryList) {
            if ((item.getTimeStamp() + 5000) < System.currentTimeMillis()) {
                expiredAccessories.add(item);
            }
        }

        if (!expiredAccessories.isEmpty()) {
            mSelectAccessoryList.removeAll(expiredAccessories);
            //updateSelectAccessoriesDialog();
        }
    }

    private Accessory getAccessoryFromIndex(int index) {
        Log.d(TAG, "getAccessoryFromIndex: ");
        if (mAccessoriesList == null || mAccessoriesList.size() == 0) {
            return null;
        }

        if (mAccessoriesList.size() < index) {
            return null;
        }

        return mAccessoriesList.get(index);
    }


    private boolean bleClose(Accessory accessory) {
        Log.d(TAG, "bleClose: ");
        mBluetoothLEManagerHelper.close(accessory.getMac());
        //log(LoggerHelper.LogEvent.LOG_EVENT_BLE_DEV_DISCONNECTED, accessory);
        return true;
    }

    private boolean uwbClose(Accessory accessory) {
        Log.d(TAG, "uwbClose: ");
        mUwbManagerHelper.close(accessory.getMac());
        //log(LoggerHelper.LogEvent.LOG_EVENT_UWB_RANGING_PEER_DISCONNECTED, accessory);
        return true;
    }

    private void cancelTimerBleConnect(Accessory accessory) {
        Log.d(TAG, "cancelTimerBleConnect: ");
        Timer timerAccessoryConnect = mTimerAccessoriesConnectList.get(accessory.getMac());
        if (timerAccessoryConnect != null) {
            timerAccessoryConnect.purge();
            timerAccessoryConnect.cancel();
        }

        mTimerAccessoriesConnectList.remove(accessory.getMac());
    }

    private void cancelTimerAccessoriesLegacyOoBSupport(Accessory accessory) {
        Log.d(TAG, "cancelTimerAccessoriesLegacyOoBSupport: ");
        Timer timerAccessoryLegacyOoBSupport = mTimerAccessoriesLegacyOoBSupportList.get(accessory.getMac());
        if (timerAccessoryLegacyOoBSupport != null) {
            timerAccessoryLegacyOoBSupport.purge();
            timerAccessoryLegacyOoBSupport.cancel();
        }

        mTimerAccessoriesLegacyOoBSupportList.remove(accessory.getMac());
    }


    // BluetoothLEManagerHelper
    @Override
    public void onBluetoothLEStateChanged(int state) {
        Log.d(TAG, "onBluetoothLEStateChanged: ");
        // No need to handle all other states, only state on to state off transaction
        if (state == BluetoothAdapter.STATE_OFF) {
            // Close all sessions
            bleClose();
            uwbClose();
            cancelTimerBleExpiredAccessories();
            cancelTimerBleConnect();
            cancelTimerAccessoriesLegacyOoBSupport();

            // Clear list with ongoing connections
            mAccessoriesList.clear();
        }
    }

    @Override
    public void onBluetoothLEDeviceBonded(String name, String address) {
        Log.d(TAG, "onBluetoothLEDeviceBonded: ");
        Accessory accessory = null;
        accessory = new Accessory(name, address, null);

        // Ignore if already connected
        for (Accessory connectedAccessory : mAccessoriesList) {
            if (connectedAccessory.getMac().equals(accessory.getMac())) {
                return;
            }
        }

        bleClose();
        uwbClose();

        // clear list with ongoing connections
        mAccessoriesList.clear();

        // show selected accessory info to user
        bleConnectToDevice(accessory);
        Log.d(TAG, "onBluetoothLEDeviceBonded: bleConnectToDevice");

    }

    @Override
    public void onBluetoothLEDeviceScanned(String name, String address) {
        Log.d(TAG, "onBluetoothLEDeviceScanned: Name " + name + " " + address);
        Accessory accessory; //mDatabaseStorageHelper.getAliasedAccessory(address);
        accessory = new Accessory(name, address, null);


        // If Accessory Name or Accesory Mac address is null or empty, ignore the notification
        if (accessory.getMac() == null || accessory.getMac().isEmpty()
                || accessory.getName() == null || accessory.getName().isEmpty()) {
            Log.d(TAG, "onBluetoothLEDeviceScanned: If Accessory Name or Accessory Mac address is null or empty");
            return;
        }

        // When a device is already connected and user wants to switch to another device, ignore it
        for (Accessory connectedAccessory : mAccessoriesList) {
            if (connectedAccessory.getMac().equals(accessory.getMac())) {
                return;
            }
        }

        // Suitable for more complex routines like don't show devices already tracked
        for (SelectAccessoriesDialogItem selectAccessory : mSelectAccessoryList) {
            if (selectAccessory.getMac().equals(accessory.getMac())) {
                selectAccessory.setTimeStamp(System.currentTimeMillis());
                return;
            }
        }

        boolean isBondedDevice = mBluetoothLEManagerHelper.isBondedDevice(accessory.getMac());
        SelectAccessoriesDialogItem selectAccessoriesDialogItem = new SelectAccessoriesDialogItem(accessory.getName(), accessory.getMac(), accessory.getAlias(), System.currentTimeMillis(), isBondedDevice, false);
        mSelectAccessoryList.add(selectAccessoriesDialogItem);

        bleConnectToDevice(accessory);
        Log.d(TAG, "onBluetoothLEDeviceScanned: Accessory"+accessory.toString());

    }

    @Override
    public void onBluetoothLEDeviceConnected(String name, String address) {
        Log.d(TAG, "onBluetoothLEDeviceConnected: ");
        Accessory accessory = null; //mDatabaseStorageHelper.getAliasedAccessory(address);
        if (accessory == null) {
            accessory = new Accessory(name, address, null);
        }

        mAccessoriesList.add(accessory);

        cancelTimerBleConnect(accessory);

        // Let's proceed with the UWB session configuration
        transmitStartUwbRangingConfiguration(accessory);

        Accessory finalAccessory = accessory;

        new Handler(Looper.getMainLooper()).post(() -> {
            if (uwbCallBack != null) {
                uwbCallBack.uwbConnectDeviceDetails(finalAccessory.getName(), finalAccessory.getMac(), finalAccessory.getAlias());
            }
        });


    }

    @Override
    public void onBluetoothLEDeviceDisconnected(String address) {
        Log.d(TAG, "onBluetoothLEDeviceDisconnected: ");
        Accessory accessory = getAccessoryFromBluetoothLeAddress(address);
        if (accessory == null) {
            Log.e(TAG, "Unexpected Bluetooth LE address");
            return;
        }

        // Close sessions
        bleClose(accessory);
        uwbClose(accessory);
        cancelTimerBleConnect(accessory);
        cancelTimerAccessoriesLegacyOoBSupport(accessory);

        // Remove from list
        mAccessoriesList.remove(accessory);

        // Display dialog to inform about lost connection

    }

    @Override
    public void onBluetoothLEDataReceived(String address, byte[] data) {
        Log.d(TAG, "onBluetoothLEDataReceived: ");
        Accessory accessory = getAccessoryFromBluetoothLeAddress(address);

        if (accessory == null) {
            Log.e(TAG, "Unexpected Bluetooth LE address");
            return;
        }

        byte messageId = data[0];
        if (messageId == OoBTlvHelper.MessageId.uwbDeviceConfigurationData.getValue()) {

            cancelTimerAccessoriesLegacyOoBSupport(accessory);

            byte[] deviceConfigData = OoBTlvHelper.getTagValue(data, OoBTlvHelper.MessageId.uwbDeviceConfigurationData.getValue());

            startRanging(accessory, deviceConfigData);
        } else if (messageId == OoBTlvHelper.MessageId.uwbDidStart.getValue()) {
            uwbRangingSessionStarted(accessory);
        } else if (messageId == OoBTlvHelper.MessageId.uwbDidStop.getValue()) {
            uwbRangingSessionStopped(accessory);
        } else {
            throw new IllegalArgumentException("Unexpected value");
        }

    }


    //UwbManagerHelper Listener
    @Override
    public void onRangingStarted(String address, UwbPhoneConfigData uwbPhoneConfigData) {
        Log.d(TAG, "onRangingStarted: ");

        new Handler(Looper.getMainLooper()).post(() -> {
            if (uwbCallBack != null) {
                uwbCallBack.uwbRangingStarted();
            }
        });


        // OnRanging started then we need to stop the scanning
        bleStopDeviceScan();

        Accessory accessory = getAccessoryFromBluetoothLeAddress(address);
        if (accessory == null) {
            Log.e(TAG, "Unexpected Bluetooth LE address");
            return;
        }

        transmitUwbPhoneConfigData(accessory, uwbPhoneConfigData);
    }

    @Override
    public void onRangingResult(String address, RangingResult rangingResult) {
        Accessory accessory = getAccessoryFromBluetoothLeAddress(address);
        if (accessory == null) {
            Log.e(TAG, "Unexpected Bluetooth LE address");
            return;
        }

        if (rangingResult instanceof RangingResult.RangingResultPosition) {
            RangingResult.RangingResultPosition rangingResultPosition = (RangingResult.RangingResultPosition) rangingResult;
            if (rangingResultPosition.getPosition().getDistance() != null
                    && rangingResultPosition.getPosition().getAzimuth() != null) {

                float distance = rangingResultPosition.getPosition().getDistance().getValue();
                float azimuth = rangingResultPosition.getPosition().getAzimuth().getValue();

                // Elevation might be null in some Android phones
                if (rangingResultPosition.getPosition().getElevation() != null) {
                    float elevation = rangingResultPosition.getPosition().getElevation().getValue();
                    Log.d(TAG, "onRangingResult: " + "Distance: " + (int) (distance * 100) + ", Azimuth Angle: " + (int) azimuth + ", Elevation " + (int) elevation);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (uwbCallBack != null) {
                            uwbCallBack.uwbRangingResult((int) (distance * 100), (int) azimuth, (int) elevation);
                        }
                    });

                    Position position = new Position(distance, azimuth,elevation);

                } else {
                    Log.d(TAG, "Distance" + (int) (distance * 100) + ", Azimuth Angle " + (int) azimuth);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (uwbCallBack != null) {
                            uwbCallBack.uwbRangingResult((int) (distance * 100), (int) azimuth, 0);
                        }
                    });

                }

                Position position = new Position(distance, azimuth);
                //updateSelectedAccessoryPosition(accessory, position);
            }
        } else if (rangingResult instanceof RangingResult.RangingResultPeerDisconnected) {
            bleClose(accessory);
            uwbClose(accessory);
            cancelTimerBleConnect(accessory);
            cancelTimerAccessoriesLegacyOoBSupport(accessory);

            // Remove the accessory from the list
            mAccessoriesList.remove(accessory);

            // Display dialog to inform about lost connection
        }

    }

    @Override
    public void onRangingError(Throwable error) {
        Log.d(TAG, "onRangingError: "+error.getMessage());
        // Close sessions
        bleClose();
        uwbClose();
        cancelTimerBleExpiredAccessories();
        cancelTimerBleConnect();
        cancelTimerAccessoriesLegacyOoBSupport();

        new Handler(Looper.getMainLooper()).post(() -> {
            if (uwbCallBack != null) {
                uwbCallBack.uwbRangingError();
            }
        });


        // Clear list with ongoing connections
        mAccessoriesList.clear();

        //UWB_RANGING_ERROR);
        //new Handler(Looper.getMainLooper()).post(() -> displayRangingError(error));
    }

    @Override
    public void onRangingComplete() {
        Log.d(TAG, "onRangingComplete: ");
        new Handler(Looper.getMainLooper()).post(() -> {
            if (uwbCallBack != null) {
                uwbCallBack.uwbRangingComplete();
            }
        });

    }

    @Override
    public void onRangingCapabilities(RangingCapabilities rangingCapabilities) {
        Log.d(TAG, "onRangingCapabilities: " +
                " Angle Supported" + rangingCapabilities.isAzimuthalAngleSupported() +
                " Distance Supported" + rangingCapabilities.isDistanceSupported() +
                " Elevation Supported" + rangingCapabilities.isElevationAngleSupported()
        );

        new Handler(Looper.getMainLooper()).post(() -> {
            if (uwbCallBack != null) {
                uwbCallBack.uwbRangingCapabilities(rangingCapabilities.isAzimuthalAngleSupported()
                        , rangingCapabilities.isDistanceSupported()
                        , rangingCapabilities.isElevationAngleSupported());

            }
        });


    }


    private void bleConnectToDevice(Accessory accessory) {
        Log.d(TAG, "bleConnectToDevice: Name: " + accessory.getName() + " Address " + accessory.getMac());
        mBluetoothLEManagerHelper.connect(accessory.getMac());
        //clearAccessoryPosition();
        startTimerBleConnect(accessory);
    }

    private void startTimerBleConnect(Accessory accessory) {
        Log.d(TAG, "startTimerBleConnect: ");
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                bleConnectTimeout(accessory.getMac());
            }
        };

        Timer timerAccessoryConnect = new Timer();
        timerAccessoryConnect.schedule(tt, 5000);
        mTimerAccessoriesConnectList.put(accessory.getMac(), timerAccessoryConnect);
    }

    private void bleConnectTimeout(String remoteAddress) {
        Log.d(TAG, "bleConnectTimeout: ");
        // Need to remove the timeout that is linked to this remoteAddress key
        Accessory tempAccessory = new Accessory(null, remoteAddress, null);
        cancelTimerBleConnect(tempAccessory);

        //showSelectAccessoryText();
    }

    private boolean transmitStartUwbRangingConfiguration(Accessory accessory) {
        Log.d(TAG, "transmitStartUwbRangingConfiguration: ");
        byte[] startUwbRangingConfigurationTlv = OoBTlvHelper.buildTlv(
                OoBTlvHelper.MessageId.initialize.getValue()
        );

        startTimerAccessoriesLegacyOoBSupport(accessory);

        return mBluetoothLEManagerHelper.transmit(accessory.getMac(), startUwbRangingConfigurationTlv);
    }

    private void startTimerAccessoriesLegacyOoBSupport(final Accessory accessory) {
        Log.d(TAG, "startTimerAccessoriesLegacyOoBSupport: ");
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "Legacy OoB support timeout fired!");
                legacyOoBSupportTimeout(accessory.getMac());
            }
        };

        Timer timerAccessoryLegacyOoBSupport = new Timer();
        timerAccessoryLegacyOoBSupport.schedule(tt, 2000);
        mTimerAccessoriesLegacyOoBSupportList.put(accessory.getMac(), timerAccessoryLegacyOoBSupport);
    }

    private void legacyOoBSupportTimeout(String remoteAddress) {
        Log.d(TAG, "legacyOoBSupportTimeout: ");
        Accessory accessory = getAccessoryFromBluetoothLeAddress(remoteAddress);
        if (accessory != null) {
            transmitLegacyStartUwbRangingConfiguration(accessory);
            cancelTimerAccessoriesLegacyOoBSupport(accessory);
        }
    }

    private Accessory getAccessoryFromBluetoothLeAddress(String address) {
        for (Accessory accessory : mAccessoriesList) {
            if (accessory.getMac().equals(address)) {
                return accessory;
            }
        }

        return null;
    }

    private boolean transmitLegacyStartUwbRangingConfiguration(Accessory accessory) {
        Log.d(TAG, "transmitLegacyStartUwbRangingConfiguration: ");
        byte[] legacyStartUwbRangingConfigurationTlv = OoBTlvHelper.buildTlv(
                OoBTlvHelper.MessageId.initialize.getValue(),
                Utils.byteToByteArray(OoBTlvHelper.DevTypeLegacy.android.getValue())
        );

        return mBluetoothLEManagerHelper.transmit(accessory.getMac(), legacyStartUwbRangingConfigurationTlv);
    }

    private boolean startRanging(Accessory accessory, byte[] deviceConfigData) {
        Log.d(TAG, "Start ranging with accessory: " + accessory.getMac());
        final UwbDeviceConfigData uwbDeviceConfigData = UwbDeviceConfigData.fromByteArray(deviceConfigData);
        return mUwbManagerHelper.startRanging(accessory.getMac(), uwbDeviceConfigData);
    }

    private boolean stopRanging(Accessory accessory) {
        Log.d(TAG, "Stop ranging with accessory: " + accessory.getMac());
        return mUwbManagerHelper.stopRanging(accessory.getMac());
    }

    private void uwbRangingSessionStarted(Accessory accessory) {
        Log.d(TAG, "Ranging started with accessory: " + accessory.getMac());
        //log(LoggerHelper.LogEvent.LOG_EVENT_UWB_RANGING_START);
    }

    private void uwbRangingSessionStopped(Accessory accessory) {
        Log.d(TAG, "Ranging stopped with accessory: " + accessory.getMac());
        //log(LoggerHelper.LogEvent.LOG_EVENT_UWB_RANGING_STOP);
    }

    private boolean transmitUwbPhoneConfigData(Accessory accessory, UwbPhoneConfigData uwbPhoneConfigData) {
        Log.d(TAG, "transmitUwbPhoneConfigData: ");
        byte[] transmitUwbPhoneConfigDataTlv = OoBTlvHelper.buildTlv(
                OoBTlvHelper.MessageId.uwbPhoneConfigurationData.getValue(),
                uwbPhoneConfigData.toByteArray());

        return mBluetoothLEManagerHelper.transmit(accessory.getMac(), transmitUwbPhoneConfigDataTlv);
    }

    private boolean transmitUwbRangingStop(Accessory accessory) {
        Log.d(TAG, "transmitUwbRangingStop: ");
        byte[] transmitUwbRangingStopTlv = OoBTlvHelper.buildTlv(
                OoBTlvHelper.MessageId.stop.getValue());

        return mBluetoothLEManagerHelper.transmit(accessory.getMac(), transmitUwbRangingStopTlv);
    }

}
