package com.example.uwb.manager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.uwb.RangingCapabilities;
import androidx.core.uwb.RangingParameters;
import androidx.core.uwb.RangingResult;
import androidx.core.uwb.UwbAddress;
import androidx.core.uwb.UwbClientSessionScope;
import androidx.core.uwb.UwbComplexChannel;
import androidx.core.uwb.UwbControleeSessionScope;
import androidx.core.uwb.UwbControllerSessionScope;
import androidx.core.uwb.UwbDevice;
import androidx.core.uwb.UwbManager;
import androidx.core.uwb.rxjava3.UwbClientSessionScopeRx;
import androidx.core.uwb.rxjava3.UwbManagerRx;

import com.example.uwb.model.UwbRemoteDevice;
import com.example.uwb.oob.model.UwbDeviceConfigData;
import com.example.uwb.oob.model.UwbPhoneConfigData;
import com.example.uwb.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 06:14
 */
public class UwbManagerHelper {
    private static final String TAG = UwbManagerHelper.class.getSimpleName();

    private static final int CONTROLLER_ROLE = 0x00;
    private static final int CONTROLEE_ROLE = 0x01;

    public static Map<String, Integer> uwbRoleMap;

    static {
        uwbRoleMap = new HashMap<>();
        uwbRoleMap.put("Controller", CONTROLLER_ROLE);
        uwbRoleMap.put("Controllee", CONTROLEE_ROLE);
    }

    // Default UWB Ranging Configuration Parameters
    public static final int UWB_CHANNEL = 9;
    public static final int UWB_PREAMBLE_INDEX = 10;
    public static final int PREFERRED_UWB_PHONE_ROLE = CONTROLEE_ROLE;
    public static final int PREFERRED_UWB_PROFILE_ID = RangingParameters.UWB_CONFIG_ID_1;

    //configuration parameters
    private int mUwbChannel = UWB_CHANNEL;
    private int mUwbPreambleIndex = UWB_PREAMBLE_INDEX;
    private int mPreferredUwbProfileId = PREFERRED_UWB_PROFILE_ID;
    private int mPreferredUwbPhoneRole = PREFERRED_UWB_PHONE_ROLE;

    private Context mContext;
    private UwbManager mUwbManager = null;

    private Listener mListener = null;

    // List with all the active uwb ranging sessions
    private HashMap<String, UwbRemoteDevice> mUwbRemoteDeviceList = new HashMap<>();

    public interface Listener {
        void onRangingCapabilities(RangingCapabilities rangingCapabilities);

        void onRangingStarted(String address, UwbPhoneConfigData uwbPhoneConfigData);

        void onRangingResult(String address, RangingResult rangingResult);

        void onRangingError(Throwable error);

        void onRangingComplete();
    }


    public UwbManagerHelper(Context context) {
        this.mContext = context;


        if (isUwbSupported()) {
            mUwbManager = UwbManager.createInstance(mContext);
        }
    }

    // create the uwb manager is supported by this device
    private boolean isUwbSupported(){
        PackageManager packageManager = mContext.getPackageManager();
        return packageManager.hasSystemFeature("android.hardware.uwb");
    }

    public void registerListener(Listener listener) {
        this.mListener = listener;
    }

    public void unregisterListener() {
        this.mListener = null;
    }

    public boolean isSupported() {
        return mUwbManager != null;
    }

    public boolean isEnabled() {
        return true;
    }

    public void setUwbChannel(int uwbChannel) {
        mUwbChannel = uwbChannel;
    }

    public void setUwbPreambleIndex(int uwbPreambleIndex) {
        mUwbPreambleIndex = uwbPreambleIndex;
    }

    public void setPreferredUwbRole(String uwbRole) {
        // Update preferred role given that provided key exists
        Integer hashUwbRole = uwbRoleMap.get(uwbRole);
        if (hashUwbRole != null) {
            mPreferredUwbPhoneRole = hashUwbRole;
        }
    }


    public void setPreferredUwbPhoneRole(String uwbRole) {
        Integer hashUwbRole = uwbRoleMap.get(uwbRole);
        if (hashUwbRole != null) {
            mPreferredUwbPhoneRole = hashUwbRole;
        }
    }

    public void setPreferredUwbProfileId(int uwbConfigType) {
        mPreferredUwbProfileId = uwbConfigType;
    }

    public boolean startRanging(String remoteAddress, UwbDeviceConfigData uwbDeviceConfigData) {

        if (mUwbManager == null) {
            Log.e(TAG, "UWB Manager is not available in this device");
            return false;
        }

        if (remoteAddress == null || remoteAddress.isEmpty()) {
            Log.e(TAG, "remote address is not set");
            return false;
        }

        if (uwbDeviceConfigData == null) {
            Log.e(TAG, "uwbDeviceConfigData is not set");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.UWB_RANGING) == PackageManager.PERMISSION_GRANTED) {
            Thread t = new Thread(() -> {

                byte uwbDeviceRangingRole = selectUwbDeviceRangingRole(uwbDeviceConfigData.getSupportedDeviceRangingRoles());
                Log.d(TAG, "Uwb device supported ranging roles: " + uwbDeviceConfigData.getSupportedDeviceRangingRoles() + ", selected role for UWB device: " + uwbDeviceRangingRole);

                byte uwbProfileId = selectUwbProfileId(uwbDeviceConfigData.getSupportedUwbProfileIds());
                Log.d(TAG, "Uwb device supported UWB profile IDs: " + uwbDeviceConfigData.getSupportedUwbProfileIds() + ", selected UWB profile ID: " + uwbProfileId);

                try {
                    UwbAddress localAddress;
                    UwbComplexChannel uwbComplexChannel;
                    UwbControleeSessionScope uwbControleeSessionScope = null;
                    UwbControllerSessionScope uwbControllerSessionScope = null;

                    if (uwbDeviceRangingRole == CONTROLLER_ROLE) {
                        Log.d(TAG, "Android device will act as Controlee!");
                        Single<UwbControleeSessionScope> controleeSessionScopeSingle = UwbManagerRx.controleeSessionScopeSingle(mUwbManager);
                        uwbControleeSessionScope = controleeSessionScopeSingle.blockingGet();

                        // For Controlee role, the uwbComplexChannel is assigned by the app
                        localAddress = uwbControleeSessionScope.getLocalAddress();
                        uwbComplexChannel = new UwbComplexChannel(mUwbChannel, mUwbPreambleIndex);
                    } else {
                        Log.d(TAG, "Android device will act as Controller!");
                        Single<UwbControllerSessionScope> controllerSessionScopeSingle = UwbManagerRx.controllerSessionScopeSingle(mUwbManager);
                        uwbControllerSessionScope = controllerSessionScopeSingle.blockingGet();

                        // For Controller role, the uwbComplexChannel is assigned by the system
                        localAddress = uwbControllerSessionScope.getLocalAddress();
                        uwbComplexChannel = uwbControllerSessionScope.getUwbComplexChannel();
                    }

                    // Assign a random Session ID
                    int sessionId = new Random().nextInt();

                    // Remote UWB device
                    UwbAddress uwbAddress = new UwbAddress(Utils.revert(uwbDeviceConfigData.getDeviceMacAddress()));
                    UwbDevice uwbDevice = new UwbDevice(uwbAddress);
                    List<UwbDevice> listUwbDevices = new ArrayList<>();
                    listUwbDevices.add(uwbDevice);

                    // logs for UWB device data
                    Log.v(TAG, "startRanging: ***************************************************************");
                    Log.v(TAG, "SpecVerMajor " + uwbDeviceConfigData.getSpecVerMajor());
                    Log.v(TAG, "SpecVerMinor " + uwbDeviceConfigData.getSpecVerMinor());
                    Log.v(TAG, "ChipId " + Utils.byteArrayToHexString(uwbDeviceConfigData.getChipId()));
                    Log.v(TAG, "ChipFwVersion " + Utils.byteArrayToHexString(uwbDeviceConfigData.getChipFwVersion()));
                    Log.v(TAG, "MwVersion " + Utils.byteArrayToHexString(uwbDeviceConfigData.getMwVersion()));
                    Log.v(TAG, "SupportedUwbProfileIds " + uwbDeviceConfigData.getSupportedUwbProfileIds());
                    Log.v(TAG, "SupportedDeviceRangingRoles " + uwbDeviceConfigData.getSupportedDeviceRangingRoles());
                    Log.v(TAG, "DeviceMacAddress " + Utils.byteArrayToHexString(uwbDeviceConfigData.getDeviceMacAddress()));
                    Log.v(TAG, "startRanging: ***************************************************************");


                    // Logs for debugging
                    Log.d(TAG, "UWB SessionId: " + sessionId);
                    Log.d(TAG, "UWB Local Address: " + localAddress);
                    Log.d(TAG, "UWB Remote Address: " + uwbDevice.getAddress());
                    Log.d(TAG, "UWB Channel: " + uwbComplexChannel.getChannel());
                    Log.d(TAG, "UWB Preamble Index: " + uwbComplexChannel.getPreambleIndex());

                    Log.d(TAG, "Configure ranging parameters for Profile ID: " + uwbProfileId);


                    RangingParameters rangingParameters = new RangingParameters(
                            uwbProfileId,
                            sessionId,
                            null,
                            uwbComplexChannel,
                            listUwbDevices,
                            RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
                    );

                    Flowable<RangingResult> rangingResultFlowable;
                    if (uwbDeviceRangingRole == CONTROLLER_ROLE) {
                        Log.d(TAG, "Configure controlee flowable");
                        rangingResultFlowable =
                                UwbClientSessionScopeRx.rangingResultsFlowable(uwbControleeSessionScope,
                                        rangingParameters);
                    } else {
                        Log.d(TAG, "Configure controller flowable");
                        rangingResultFlowable =
                                UwbClientSessionScopeRx.rangingResultsFlowable(uwbControllerSessionScope,
                                        rangingParameters);
                    }

                    // Consume ranging results from Flowable using Disposable
                    Disposable disposable = rangingResultFlowable
                            .delay(100, TimeUnit.MILLISECONDS)
                            .subscribeWith(new DisposableSubscriber<RangingResult>() {
                                @Override
                                public void onStart() {
                                    request(1);
                                }

                                @Override
                                public void onNext(RangingResult rangingResult) {
                                    String address = getAddressFromUwbDevice(rangingResult.getDevice());
                                    if (address != null) {
                                        onRangingResult(address, rangingResult);
                                    }

                                    request(1);
                                }

                                @Override
                                public void onError(Throwable error) {
                                    Log.d(TAG, "Disposable onError: "+error.getMessage());
                                    onRangingError(error);
                                }

                                @Override
                                public void onComplete() {
                                    onRangingComplete();
                                }
                            });

                    // Add the new device to the list, use Bluetooth LE address as key
                    UwbRemoteDevice uwbRemoteDevice = new UwbRemoteDevice(uwbDevice, disposable);
                    mUwbRemoteDeviceList.put(remoteAddress, uwbRemoteDevice);

                    // Create ShareableData with configured UWB Session params
                    UwbPhoneConfigData uwbPhoneConfigData = new UwbPhoneConfigData();
                    uwbPhoneConfigData.setSpecVerMajor((short) 0x0100);
                    uwbPhoneConfigData.setSpecVerMinor((short) 0x0000);
                    uwbPhoneConfigData.setSessionId(sessionId);
                    uwbPhoneConfigData.setPreambleIndex((byte) uwbComplexChannel.getPreambleIndex());
                    uwbPhoneConfigData.setChannel((byte) uwbComplexChannel.getChannel());
                    uwbPhoneConfigData.setProfileId(uwbProfileId);
                    uwbPhoneConfigData.setDeviceRangingRole((byte) (1 << uwbDeviceRangingRole));
                    uwbPhoneConfigData.setPhoneMacAddress(Utils.revert(localAddress.getAddress()));

                    // Send the UWB ranging session configuration data back to the listener
                    onRangingStarted(remoteAddress, uwbPhoneConfigData);
                } catch (Exception e) {
                    Log.e(TAG, "UWB Ranging configuration exception: " + e.getMessage());
                    onRangingError(e);
                }
            });

            t.start();
            return true;
        } else {
            Log.e(TAG, "Missing required permission to start UWB ranging");
            return false;
        }
    }

    public boolean stopRanging(String remoteAddress) {
        Log.d(TAG, "Proceeed to stop connection with device " + remoteAddress);

        try {
            UwbRemoteDevice uwbRemoteDevice = mUwbRemoteDeviceList.get(remoteAddress);
            if (uwbRemoteDevice == null || uwbRemoteDevice.getDisposable() == null) {
                Log.e(TAG, "UWB Ranging session not started or disposable not initialized.");
                return false;
            }

            uwbRemoteDevice.getDisposable().dispose();
            mUwbRemoteDeviceList.remove(remoteAddress);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception while closing UWB Ranging session: " + e.getMessage());
        }

        return false;
    }

    public boolean close(String remoteAddress) {
        Log.d(TAG, "Proceeed to close connection with device " + remoteAddress);

        try {
            UwbRemoteDevice uwbRemoteDevice = mUwbRemoteDeviceList.get(remoteAddress);
            if (uwbRemoteDevice == null || uwbRemoteDevice.getDisposable() == null) {
                Log.e(TAG, "UWB Ranging session not started or disposable not initialized.");
                return false;
            }

            uwbRemoteDevice.getDisposable().dispose();
            mUwbRemoteDeviceList.remove(remoteAddress);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception while closing UWB Ranging session: " + e.getMessage());
        }

        return false;
    }

    public boolean getRangingCapabilities() {
        if (mUwbManager == null) {
            Log.e(TAG, "UWB Manager is not available in this device");
            return false;
        }

        Thread t = new Thread(() -> {
            try {
                Single<UwbClientSessionScope> uwbClientSessionScopeSingle = UwbManagerRx.clientSessionScopeSingle(mUwbManager);
                UwbClientSessionScope uwbClientSessionScope = uwbClientSessionScopeSingle.blockingGet();
                onRangingCapabilities(uwbClientSessionScope.getRangingCapabilities());
            } catch (Exception e) {
                Log.e(TAG, "Exception while getting UWB Ranging capabilities: " + e.getMessage());
                onRangingCapabilities(null);
            }
        });

        t.start();
        return true;
    }

    private byte selectUwbDeviceRangingRole(int supportedUwbDeviceRangingRoles) {
        // First try to use preferred Uwb Role selection
        if (mPreferredUwbPhoneRole == CONTROLLER_ROLE && (((supportedUwbDeviceRangingRoles >> CONTROLLER_ROLE) & 1) != 0)) {
            return CONTROLEE_ROLE;
        } else if (mPreferredUwbPhoneRole == CONTROLEE_ROLE && (((supportedUwbDeviceRangingRoles >> CONTROLLER_ROLE) & 1) != 0)) {
            return CONTROLLER_ROLE;
        }

        if (((supportedUwbDeviceRangingRoles >> CONTROLLER_ROLE) & 1) != 0) {
            return CONTROLEE_ROLE;
        } else if (((supportedUwbDeviceRangingRoles >> CONTROLEE_ROLE) & 1) != 0) {
            return CONTROLLER_ROLE;
        }

        return CONTROLLER_ROLE;
    }

    private byte selectUwbProfileId(int supportedUwbProfileIds) {
        // First try to use preferred Uwb Profile ID selection
        if (BigInteger.valueOf(supportedUwbProfileIds).testBit(mPreferredUwbProfileId)) {
            return (byte) mPreferredUwbProfileId;
        }

        if (BigInteger.valueOf(supportedUwbProfileIds).testBit(RangingParameters.UWB_CONFIG_ID_1)) {
            return (byte) RangingParameters.UWB_CONFIG_ID_1;
        } else if (BigInteger.valueOf(supportedUwbProfileIds).testBit(RangingParameters.UWB_CONFIG_ID_2)) {
            return (byte) RangingParameters.UWB_CONFIG_ID_2;
        } else if (BigInteger.valueOf(supportedUwbProfileIds).testBit(RangingParameters.UWB_CONFIG_ID_3)) {
            return (byte) RangingParameters.UWB_CONFIG_ID_3;
        }

        return 0;
    }

    private String getAddressFromUwbDevice(UwbDevice uwbDevice) {
        for (Map.Entry<String, UwbRemoteDevice> entry : mUwbRemoteDeviceList.entrySet()) {
            String key = entry.getKey();
            UwbRemoteDevice uwbRemoteDevice = entry.getValue();

            if (uwbRemoteDevice.getUwbDevice().getAddress().toString().equals(uwbDevice.getAddress().toString())) {
                return key;
            }
        }

        return null;
    }

    // Listener callbacks
    private void onRangingError(final Throwable error) {
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onRangingError(error);
            }
        });
    }

    private void onRangingResult(final String address, final RangingResult rangingResult) {
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onRangingResult(address, rangingResult);
            }
        });
    }

    private void onRangingComplete() {
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onRangingComplete();
            }
        });
    }

    private void onRangingStarted(final String address, final UwbPhoneConfigData uwbPhoneConfigData) {
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onRangingStarted(address, uwbPhoneConfigData);
            }
        });
    }

    private void onRangingCapabilities(final RangingCapabilities rangingCapabilities) {
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onRangingCapabilities(rangingCapabilities);
            }
        });
    }
}

