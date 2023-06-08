package com.example.uwb

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingResult
import com.example.uwb.bluetooth.BluetoothLEManagerHelper
import com.example.uwb.manager.UwbManagerHelper
import com.example.uwb.model.Accessory
import com.example.uwb.model.Position
import com.example.uwb.model.SelectAccessoriesDialogItem
import com.example.uwb.oob.OoBTlvHelper
import com.example.uwb.oob.model.UwbDeviceConfigData
import com.example.uwb.oob.model.UwbPhoneConfigData
import com.example.uwb.sharedpreference.PreferenceStorageHelper
import com.example.uwb.utils.Utils
import com.example.uwb.uwbcontrol.UwbRangingView
import java.util.Timer
import java.util.TimerTask

/**
 * Created by Ajay Vamsee on 6/8/2023.
 * Time : 18:28
 */
class WaveGlide(private val context: Context) : UwbRangingView.Listener,
    BluetoothLEManagerHelper.Listener, UwbManagerHelper.Listener {
    companion object {
        private val TAG = WaveGlide::class.simpleName
    }

    private val mBluetoothLEManagerHelper: BluetoothLEManagerHelper = BluetoothLEManagerHelper(context)
    private val mPreferenceStorageHelper: PreferenceStorageHelper = PreferenceStorageHelper(context)
    private val mUwbManagerHelper: UwbManagerHelper = UwbManagerHelper(context)

    private val mAccessoriesList: MutableList<Accessory> = ArrayList()
    private val mTimerAccessoriesConnectList = HashMap<String, Timer>()
    private val mTimerAccessoriesLegacyOoBSupportList = HashMap<String, Timer>()
    private val mSelectAccessoryList: MutableList<SelectAccessoriesDialogItem> = ArrayList()


    private var timerBleExpiredAccessories: Timer? = null

    private var uwbCallBack: UWBCallBack? = null

    interface UWBCallBack {
        fun uwbRangingStarted()

        fun uwbRangingResult(distance: Int, angle: Int, elevation: Int)

        fun uwbRangingCapabilities(
            distanceSupported: Boolean,
            angleSupported: Boolean,
            elevationSupported: Boolean
        )

        fun uwbRangingComplete()

        fun uwbRangingError()

        fun uwbConnectDeviceDetails(name: String?, mac: String?, alias: String?)
    }

    fun registerUwbCallBack(callBack: UWBCallBack?) {
        uwbCallBack = callBack
    }

    fun startProcess() {
        Log.d(TAG,
            "initListener: *************************************************"
        )
        mBluetoothLEManagerHelper.registerListener(this)
        mUwbManagerHelper.registerListener(this)

        bleStartDeviceScan()
    }

    fun stopProcess() {
        Log.d(TAG, "deinit: ")
        mBluetoothLEManagerHelper.unregisterListener()
        mUwbManagerHelper.unregisterListener()
        bleStopDeviceScan()
        bleClose()
        uwbClose()
        cancelTimerBleExpiredAccessories()
        cancelTimerBleConnect()
        cancelTimerAccessoriesLegacyOoBSupport()
    }

    // UwbRangingView.Listener
    override fun onBackPressed() {}

    override fun onSelectAccessoryButtonClicked() {
        Log.d(TAG, "onSelectAccessoryButtonClicked: ")
        bleClose()
        uwbClose()
        cancelTimerBleExpiredAccessories()
        cancelTimerBleConnect()
        cancelTimerAccessoriesLegacyOoBSupport()

        //showing paring info as user is interest in it
        if (mPreferenceStorageHelper.showPairingInfo) {
            // TO show the paring info
        } else {
            uwbBleStartScan()
        }
    }

    override fun onSelectedAccessoryRemoveClicked() {
        Log.d(TAG, "onSelectedAccessoryRemoveClicked: ")
        val accessory: Accessory? = getAccessoryFromIndex(0)
        accessory?.let {
            bleClose(it)
            uwbClose(it)
            cancelTimerBleConnect(it)
            cancelTimerAccessoriesLegacyOoBSupport(it)

            // Remove accessory from list
            mAccessoriesList.remove(it)
        }

    }

    override fun onSelectedAccessorySelectAccessoryClicked() {
        Log.d(TAG, "onSelectedAccessorySelectAccessoryClicked: ")
        uwbBleStartScan()
    }


    // utility methods
    private fun bleClose(): Boolean {
        Log.d(TAG, "bleClose: ")
        for (accessory in mAccessoriesList) {
            mBluetoothLEManagerHelper.close(accessory.mac)
        }
        return true
    }

    private fun uwbClose(): Boolean {
        Log.d(TAG, "uwbClose: ")
        for (accessory in mAccessoriesList) {
            mUwbManagerHelper.close(accessory.mac)
        }
        return true
    }

    private fun cancelTimerBleExpiredAccessories() {
        Log.d(TAG, "cancelTimerBleExpiredAccessories: ")
        if (timerBleExpiredAccessories != null) {
            timerBleExpiredAccessories?.purge()
            timerBleExpiredAccessories?.cancel()
        }
    }

    private fun cancelTimerBleConnect() {
        Log.d(TAG, "cancelTimerBleConnect: ")
        for (timerAccessoryConnect in mTimerAccessoriesConnectList.values) {
            timerAccessoryConnect.purge()
            timerAccessoryConnect.cancel()
        }
        mTimerAccessoriesConnectList.clear()
    }

    private fun cancelTimerAccessoriesLegacyOoBSupport() {
        Log.d(TAG, "cancelTimerAccessoriesLegacyOoBSupport: ")
        for (timerAccessoryLegacyOoBSupport in mTimerAccessoriesLegacyOoBSupportList.values) {
            timerAccessoryLegacyOoBSupport.purge()
            timerAccessoryLegacyOoBSupport.cancel()
        }
        mTimerAccessoriesConnectList.clear()
    }

    private fun uwbBleStartScan() {
        Log.d(TAG, "uwbBleStartScan: ")
        bleClose()
        uwbClose()
        cancelTimerBleExpiredAccessories()
        cancelTimerBleConnect()
        cancelTimerAccessoriesLegacyOoBSupport()
        mSelectAccessoryList.clear()
        bleStartDeviceScan()
        // Start timer to handle expired accessories
        startTimerBleExpiredAccessories()
    }

    // ble start scan
    private fun bleStartDeviceScan(): Boolean {
        Log.d(TAG, "bleStartDeviceScan: ")
        if (!mBluetoothLEManagerHelper.isSupported) {
            //BLUETOOTH NOT SUPPORTED
            Log.d(TAG, "Bluetooth is not supported")
            return false
        }
        if (!mUwbManagerHelper.isSupported) {
            // UWB NOT SUPPORTED
            Log.d(TAG, "UWB is not supported")
            return false
        }
        if (!mBluetoothLEManagerHelper.isEnabled) {
            //BLUETOOTH NOT ENABLED
            Log.d(TAG, "Bluetooth is not enabled")
            return false
        }
        if (!mUwbManagerHelper.isEnabled) {
            //UWB NOT ENABLED
            Log.d(TAG, "UWB is not enabled")
            return false
        }
        Log.d(TAG, "BLE_SCAN_START")
        return mBluetoothLEManagerHelper.startLeDeviceScan()
    }

    private fun bleStopDeviceScan(): Boolean {
        Log.d(TAG, "BLE_SCAN_STOP ")
        return mBluetoothLEManagerHelper.stopLeDeviceScan()
    }

    private fun startTimerBleExpiredAccessories() {
        Log.d(TAG, "startTimerBleExpiredAccessories: ")
        val tt: TimerTask = object : TimerTask() {
            override fun run() {
                removeBleExpiredAccessories()
            }
        }
        timerBleExpiredAccessories = Timer()
        timerBleExpiredAccessories?.schedule(tt, 1000, 1000)
    }

    private fun removeBleExpiredAccessories() {
        Log.d(TAG, "removeBleExpiredAccessories: ")
        val expiredAccessories: MutableList<SelectAccessoriesDialogItem> = java.util.ArrayList()
        for (item in mSelectAccessoryList) {
            if (item.timeStamp + 5000 < System.currentTimeMillis()) {
                expiredAccessories.add(item)
            }
        }
        if (expiredAccessories.isNotEmpty()) {
            mSelectAccessoryList.removeAll(expiredAccessories)
            //updateSelectAccessoriesDialog();
        }
    }

    private fun getAccessoryFromIndex(index: Int): Accessory? {
        Log.d(TAG, "getAccessoryFromIndex:")
        if (mAccessoriesList.isEmpty() || index < 0 || index >= mAccessoriesList.size) {
            return null
        }
        return mAccessoriesList[index]
    }


    private fun bleClose(accessory: Accessory): Boolean {
        Log.d(TAG, "bleClose: ")
        mBluetoothLEManagerHelper.close(accessory.mac)
        return true
    }

    private fun uwbClose(accessory: Accessory): Boolean {
        Log.d(TAG, "uwbClose: ")
        mUwbManagerHelper.close(accessory.mac)
        return true
    }

    private fun cancelTimerBleConnect(accessory: Accessory) {
        Log.d(TAG, "cancelTimerBleConnect: ")
        val timerAccessoryConnect: Timer? = mTimerAccessoriesConnectList.remove(accessory.mac)
        timerAccessoryConnect?.apply {
            timerAccessoryConnect.purge()
            timerAccessoryConnect.cancel()
        }

    }

    private fun cancelTimerAccessoriesLegacyOoBSupport(accessory: Accessory) {
        Log.d(TAG, "cancelTimerAccessoriesLegacyOoBSupport: ")
        val timerAccessoryLegacyOoBSupport: Timer? = mTimerAccessoriesLegacyOoBSupportList.remove(accessory.mac)
        timerAccessoryLegacyOoBSupport?.apply {
            purge()
            cancel()
        }
    }

    // BluetoothLEManagerHelper
    override fun onBluetoothLEStateChanged(state: Int) {
        Log.d(TAG, "onBluetoothLEStateChanged: ")
        // No need to handle all other states, only state on to state off transaction from MK
        // Need too handle if other states as well
        when (state) {
            BluetoothAdapter.STATE_OFF -> {
                // Close all sessions
                bleClose()
                uwbClose()
                cancelTimerBleExpiredAccessories()
                cancelTimerBleConnect()
                cancelTimerAccessoriesLegacyOoBSupport()

                // Clear list with ongoing connections
                mAccessoriesList.clear()
            }

            BluetoothAdapter.STATE_ON -> {
                Log.d(TAG, "STATE_ON")
            }

            BluetoothAdapter.STATE_CONNECTED -> {
                Log.d(TAG, "STATE_CONNECTED")
            }

            BluetoothAdapter.STATE_DISCONNECTED -> {
                Log.d(TAG, "STATE_DISCONNECTED")
            }
        }
    }

    override fun onBluetoothLEDeviceBonded(name: String?, address: String?) {
        Log.d(TAG, "onBluetoothLEDeviceBonded: ")
        val accessory = Accessory(name, address, null)

        // Ignore if already connected
        if(mAccessoriesList.any{ it.mac == accessory.mac}){
            return
        }

        bleClose()
        uwbClose()

        // clear list with ongoing connections
        mAccessoriesList.clear()

        // show selected accessory info to user
        bleConnectToDevice(accessory)
        Log.d(TAG, "onBluetoothLEDeviceBonded: bleConnectToDevice")
    }

    override fun onBluetoothLEDeviceScanned(name: String, address: String) {
        Log.d(TAG, "onBluetoothLEDeviceScanned: Name $name $address")
        val accessory: Accessory =
            Accessory(name, address, null) //mDatabaseStorageHelper.getAliasedAccessory(address);


        // If Accessory Name or Accessory Mac address is null or empty, ignore the notification
        if (accessory.mac == null || accessory.mac.isEmpty() || accessory.name == null || accessory.name.isEmpty()) {
            Log.d(
                TAG,
                "onBluetoothLEDeviceScanned: If Accessory Name or Accessory Mac address is null or empty"
            )
            return
        }

        // When a device is already connected and user wants to switch to another device, ignore it
        for (connectedAccessory in mAccessoriesList) {
            if (connectedAccessory.mac == accessory.mac) {
                return
            }
        }

        // Suitable for more complex routines like don't show devices already tracked
        for (selectAccessory in mSelectAccessoryList) {
            if (selectAccessory.mac == accessory.mac) {
                selectAccessory.timeStamp = System.currentTimeMillis()
                return
            }
        }
        val isBondedDevice: Boolean = mBluetoothLEManagerHelper.isBondedDevice(accessory.mac)
        val selectAccessoriesDialogItem = SelectAccessoriesDialogItem(
            accessory.name,
            accessory.mac,
            accessory.alias,
            System.currentTimeMillis(),
            isBondedDevice,
            false
        )
        mSelectAccessoryList.add(selectAccessoriesDialogItem)
        bleConnectToDevice(accessory)
        Log.d(TAG, "onBluetoothLEDeviceScanned:$accessory")
    }

    override fun onBluetoothLEDeviceConnected(name: String?, address: String?) {
        Log.d(TAG, "onBluetoothLEDeviceConnected: ")
        val accessory: Accessory =
            Accessory(name, address, null) //mDatabaseStorageHelper.getAliasedAccessory(address);
        mAccessoriesList.add(accessory)
        cancelTimerBleConnect(accessory)

        // Let's proceed with the UWB session configuration
        transmitStartUwbRangingConfiguration(accessory)
        Handler(Looper.getMainLooper()).post {
            uwbCallBack?.uwbConnectDeviceDetails(
                accessory.name,
                accessory.mac,
                accessory.alias
            )
        }
    }

    override fun onBluetoothLEDeviceDisconnected(address: String?) {
        Log.d(TAG, "onBluetoothLEDeviceDisconnected:")
        val accessory: Accessory? = getAccessoryFromBluetoothLeAddress(address!!)

        accessory?.run {
            // close session
            bleClose(this)
            uwbClose(this)
            cancelTimerBleConnect(this)
            cancelTimerAccessoriesLegacyOoBSupport(this)

            // Remove from list

            mAccessoriesList.remove(this)
        } ?: Log.w(TAG, "Accessory not found for address: $address")

    }


    override fun onBluetoothLEDataReceived(address: String?, data: ByteArray) {
        Log.d(TAG, "onBluetoothLEDataReceived: ")
        val accessory: Accessory? = address?.let { getAccessoryFromBluetoothLeAddress(it) }
        accessory ?: run {
            Log.e(TAG, "Unexpected Bluetooth LE address")
            return
        }
        when (data[0]) {
            OoBTlvHelper.MessageId.uwbDeviceConfigurationData.value -> {
                cancelTimerAccessoriesLegacyOoBSupport(accessory)
                val deviceConfigData = OoBTlvHelper.getTagValue(
                    data,
                    OoBTlvHelper.MessageId.uwbDeviceConfigurationData.value
                )
                startRanging(accessory, deviceConfigData)
            }

            OoBTlvHelper.MessageId.uwbDidStart.value -> {
                uwbRangingSessionStarted(accessory)
            }

            OoBTlvHelper.MessageId.uwbDidStop.value -> {
                uwbRangingSessionStopped(accessory)
            }

            else -> throw IllegalArgumentException("Unexpected value")
        }

    }

    //UwbManagerHelper Listener
    override fun onRangingStarted(address: String?, uwbPhoneConfigData: UwbPhoneConfigData?) {
        Log.d(TAG, "onRangingStarted: ")
        Handler(Looper.getMainLooper()).postDelayed({
            uwbCallBack?.uwbRangingStarted()
        }, 0)

        // OnRanging started then we need to stop the scanning
        bleStopDeviceScan()
        val accessory: Accessory? = address?.let { getAccessoryFromBluetoothLeAddress(it) }
        if (accessory == null) {
            Log.e(TAG, "Unexpected Bluetooth LE address")
            return
        }
        transmitUwbPhoneConfigData(accessory, uwbPhoneConfigData!!)
    }

    override fun onRangingResult(address: String?, rangingResult: RangingResult) {
        val accessory: Accessory? = address?.let { getAccessoryFromBluetoothLeAddress(it) }
        if (accessory == null) {
            Log.e(TAG, "Unexpected Bluetooth LE address")
            return
        }
        if (rangingResult is RangingResult.RangingResultPosition) {
            val rangingResultPosition: RangingResult.RangingResultPosition =
                rangingResult as RangingResult.RangingResultPosition
            if (rangingResultPosition.position.distance != null
                && rangingResultPosition.position.azimuth != null
            ) {
                val distance: Float = rangingResultPosition.position.distance!!.value
                val azimuth: Float = rangingResultPosition.position.azimuth!!.value

                // Elevation might be null in some Android phones
                if (rangingResultPosition.position.elevation != null) {
                    val elevation: Float? = rangingResultPosition.position.elevation?.value
                    Log.d(
                        TAG,
                        "onRangingResult: " + "Distance: " + (distance * 100).toInt() + ", Azimuth Angle: " + azimuth.toInt() + ", Elevation " + elevation?.toInt()
                    )
                    Handler(Looper.getMainLooper()).post {
                        elevation?.toInt()?.let {
                            uwbCallBack?.uwbRangingResult(
                                (distance * 100).toInt(),
                                azimuth.toInt(),
                                it
                            )
                        }
                    }
                    val position = elevation?.let { Position(distance, azimuth, it) }
                } else {
                    Log.d(
                        TAG,
                        "Distance" + (distance * 100).toInt() + ", Azimuth Angle " + azimuth.toInt()
                    )
                    Handler(Looper.getMainLooper()).post {
                        uwbCallBack?.uwbRangingResult((distance * 100).toInt(), azimuth.toInt(), 0)
                    }
                }
                val position = Position(distance, azimuth)
                //updateSelectedAccessoryPosition(accessory, position);
            }
        } else if (rangingResult is RangingResult.RangingResultPeerDisconnected) {
            bleClose(accessory)
            uwbClose(accessory)
            cancelTimerBleConnect(accessory)
            cancelTimerAccessoriesLegacyOoBSupport(accessory)

            // Remove the accessory from the list
            mAccessoriesList.remove(accessory)

            // Display dialog to inform about lost connection
        }
    }

    override fun onRangingError(error: Throwable) {
        Log.d(TAG, "onRangingError: " + error.message)
        // Close sessions
        bleClose()
        uwbClose()
        cancelTimerBleExpiredAccessories()
        cancelTimerBleConnect()
        cancelTimerAccessoriesLegacyOoBSupport()
        Handler(Looper.getMainLooper()).post {
            uwbCallBack?.uwbRangingError()
        }


        // Clear list with ongoing connections
        mAccessoriesList.clear()

        //UWB_RANGING_ERROR);
        //new Handler(Looper.getMainLooper()).post(() -> displayRangingError(error));
    }

    override fun onRangingComplete() {
        Log.d(TAG, "onRangingComplete: ")
        Handler(Looper.getMainLooper()).post {
            uwbCallBack?.uwbRangingComplete()
        }
    }

    override fun onRangingCapabilities(rangingCapabilities: RangingCapabilities) {
        Log.d(
            TAG, "onRangingCapabilities: " +
                    " Angle Supported" + rangingCapabilities.isAzimuthalAngleSupported +
                    " Distance Supported" + rangingCapabilities.isDistanceSupported +
                    " Elevation Supported" + rangingCapabilities.isElevationAngleSupported
        )
        Handler(Looper.getMainLooper()).post {
            uwbCallBack?.uwbRangingCapabilities(
                rangingCapabilities.isAzimuthalAngleSupported,
                rangingCapabilities.isDistanceSupported,
                rangingCapabilities.isElevationAngleSupported
            )
        }
    }


    private fun bleConnectToDevice(accessory: Accessory) {
        Log.d(
            TAG,
            "bleConnectToDevice: Name: " + accessory.name + " Address " + accessory.mac
        )
        mBluetoothLEManagerHelper.connect(accessory.mac)
        //clearAccessoryPosition();
        startTimerBleConnect(accessory)
    }

    private fun startTimerBleConnect(accessory: Accessory) {
        Log.d(TAG, "startTimerBleConnect: ")
        val tt: TimerTask = object : TimerTask() {
            override fun run() {
                bleConnectTimeout(accessory.mac)
            }
        }
        val timerAccessoryConnect = Timer()
        timerAccessoryConnect.schedule(tt, 5000)
        mTimerAccessoriesConnectList.put(accessory.mac, timerAccessoryConnect)
    }

    private fun bleConnectTimeout(remoteAddress: String) {
        Log.d(TAG, "bleConnectTimeout: ")
        // Need to remove the timeout that is linked to this remoteAddress key
        val tempAccessory = Accessory(null, remoteAddress, null)
        cancelTimerBleConnect(tempAccessory)

        //showSelectAccessoryText();
    }

    private fun transmitStartUwbRangingConfiguration(accessory: Accessory): Boolean {
        Log.d(TAG, "transmitStartUwbRangingConfiguration: ")
        val startUwbRangingConfigurationTlv = OoBTlvHelper.buildTlv(
            OoBTlvHelper.MessageId.initialize.value
        )
        startTimerAccessoriesLegacyOoBSupport(accessory)
        return mBluetoothLEManagerHelper.transmit(accessory.mac, startUwbRangingConfigurationTlv)
    }

    private fun startTimerAccessoriesLegacyOoBSupport(accessory: Accessory) {
        Log.d(TAG, "startTimerAccessoriesLegacyOoBSupport: ")
        val tt: TimerTask = object : TimerTask() {
            override fun run() {
                Log.d(TAG, "Legacy OoB support timeout fired!")
                legacyOoBSupportTimeout(accessory.mac)
            }
        }
        val timerAccessoryLegacyOoBSupport = Timer()
        timerAccessoryLegacyOoBSupport.schedule(tt, 2000)
        mTimerAccessoriesLegacyOoBSupportList.put(accessory.mac, timerAccessoryLegacyOoBSupport)
    }

    private fun legacyOoBSupportTimeout(remoteAddress: String) {
        Log.d(TAG, "legacyOoBSupportTimeout: ")
        val accessory = getAccessoryFromBluetoothLeAddress(remoteAddress)
        if (accessory != null) {
            transmitLegacyStartUwbRangingConfiguration(accessory)
            cancelTimerAccessoriesLegacyOoBSupport(accessory)
        }
    }

    private fun getAccessoryFromBluetoothLeAddress(address: String): Accessory? {
        for (accessory in mAccessoriesList) {
            if (accessory.mac == address) {
                return accessory
            }
        }
        return null
    }


    private fun transmitLegacyStartUwbRangingConfiguration(accessory: Accessory): Boolean {
        Log.d(TAG, "transmitLegacyStartUwbRangingConfiguration: ")
        val legacyStartUwbRangingConfigurationTlv = OoBTlvHelper.buildTlv(
            OoBTlvHelper.MessageId.initialize.value,
            Utils.byteToByteArray(OoBTlvHelper.DevTypeLegacy.android.value)
        )
        return mBluetoothLEManagerHelper.transmit(
            accessory.mac,
            legacyStartUwbRangingConfigurationTlv
        )
    }

    private fun startRanging(accessory: Accessory, deviceConfigData: ByteArray): Boolean {
        Log.d(TAG, "Start ranging with accessory: " + accessory.mac)
        val uwbDeviceConfigData = UwbDeviceConfigData.fromByteArray(deviceConfigData)
        return mUwbManagerHelper.startRanging(accessory.mac, uwbDeviceConfigData)
    }

    private fun stopRanging(accessory: Accessory): Boolean {
        Log.d(TAG, "Stop ranging with accessory: " + accessory.mac)
        return mUwbManagerHelper.stopRanging(accessory.mac)
    }

    private fun uwbRangingSessionStarted(accessory: Accessory) {
        Log.d(TAG, "Ranging started with accessory: " + accessory.mac)
    }

    private fun uwbRangingSessionStopped(accessory: Accessory) {
        Log.d(TAG, "Ranging stopped with accessory: " + accessory.mac)
    }

    private fun transmitUwbPhoneConfigData(
        accessory: Accessory,
        uwbPhoneConfigData: UwbPhoneConfigData
    ): Boolean {
        Log.d(TAG, "transmitUwbPhoneConfigData: ")
        val transmitUwbPhoneConfigDataTlv = OoBTlvHelper.buildTlv(
            OoBTlvHelper.MessageId.uwbPhoneConfigurationData.value,
            uwbPhoneConfigData.toByteArray()
        )
        return mBluetoothLEManagerHelper.transmit(accessory.mac, transmitUwbPhoneConfigDataTlv)
    }

    private fun transmitUwbRangingStop(accessory: Accessory): Boolean {
        Log.d(TAG, "transmitUwbRangingStop: ")
        val transmitUwbRangingStopTlv = OoBTlvHelper.buildTlv(
            OoBTlvHelper.MessageId.stop.value
        )
        return mBluetoothLEManagerHelper.transmit(accessory.mac, transmitUwbRangingStopTlv)
    }


}
