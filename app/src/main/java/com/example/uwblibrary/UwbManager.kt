package com.example.uwblibrary

import android.util.Log
import com.example.uwb.uwbcontrol.UwbRangingController
import com.example.uwb.bluetooth.BluetoothLEManagerHelper
import com.example.uwb.location.LocationManagerHelper
import com.example.uwb.manager.UwbManagerHelper
import com.example.uwb.permissions.PermissionHelper
import com.example.uwb.sharedpreference.PreferenceStorageHelper

/**
 * Created by Ajay Vamsee on 6/7/2023.
 * Time : 18:43
 */
class UwbManager() : UwbRangingController.UWBCallBack {

    private lateinit var bluetoothLEManagerHelper: BluetoothLEManagerHelper
    private lateinit var preferenceStorageHelper: PreferenceStorageHelper
    private lateinit var locationManagerHelper: LocationManagerHelper
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var uwbLEManagerHelper: UwbManagerHelper

    private lateinit var uwbActivity: UWBActivity


    private var uwbRangingController: UwbRangingController? = null


    constructor(uwbActivity: UWBActivity) : this() {
        this.uwbActivity = uwbActivity
        this.bluetoothLEManagerHelper = BluetoothLEManagerHelper(uwbActivity)
        this.preferenceStorageHelper = PreferenceStorageHelper(uwbActivity)
        this.locationManagerHelper = LocationManagerHelper(uwbActivity)
        this.permissionHelper = PermissionHelper(uwbActivity)
        this.uwbLEManagerHelper = UwbManagerHelper(uwbActivity)

        registerUwbCallBack()

    }


    private fun registerUwbCallBack() {
        uwbRangingController = UwbRangingController(
            bluetoothLEManagerHelper,
            preferenceStorageHelper,
            uwbLEManagerHelper,
            locationManagerHelper,
            permissionHelper
        )
        uwbRangingController?.registerUwbCallBack(uwbActivity)
    }

    fun setUwbActivity(activity: UWBActivity) {
        uwbActivity = activity
    }

    // Implement the required UWB callbacks
    // ...

    fun startUwbRanging() {
        uwbRangingController?.initListener()
        uwbRangingController?.uwbBleStartScan()
    }

    fun stopUwbRanging() {
        uwbRangingController?.deinit()
    }

    companion object {
        private val TAG = UwbManager::class.java.simpleName
    }

    // UWB callbacks
    override fun uwbRangingStarted() {
        Log.d(TAG, "UWB ranging started")

    }

    override fun uwbRangingResult(distance: Int, angle: Int, elevation: Int) {
        Log.d(
            TAG, "UWB ranging result: distance=$distance, angle=$angle, elevation=$elevation"
        )
        //binding.tvDistance.setText(""+distance);
        if (distance >= 100) {
            val meters = distance / 100
            val centimeters = distance % 100
            Log.d(TAG, "$meters.$centimeters" + "cms")

        } else {
            Log.d(TAG, "" + distance + "cms")


            /*boolean doorState = true;

            if(doorState && distance ){
                UIManager.getInstance().open((byte) 1);
                doorState = false;
            } else if(distance > 100){
                doorState = true;
            }
*//*Log.d(TAG, "uwbRangingResult: getAdoOperationResponse "+Repository.getInstance().getAdoOperationResponse().getValue());
            if(Repository.getInstance().getAdoOperationResponse().getValue() != OperationResponseEnum.OPENED.getValue()){
                UIManager.getInstance().open((byte) 1);
                Log.d(TAG, "uwbRangingResult: dooropen cmd send is coz is door is closed");
            }else {
                Log.d(TAG, "uwbRangingResult: door is already opened");
            }*/
        }
        var doorState = true
        if (doorState && distance < 300) {
            //UIManager.getInstance().open((byte) 1);
            doorState = false
        } else if (distance > 100) {
            doorState = true
        }
        val scaleFactor: Double
        scaleFactor = if (distance < 8.0) {
            1 - distance * 0.1
        } else {
            0.2
        }
        if (angle >= -10 && angle <= 10) {
            ///Log.d(TAG, "uwbRangingResult: AHEAD");
        } else {
            if (angle >= 0) {
                //Log.d(TAG, "uwbRangingResult: RIGHT");
            } else {
                //Log.d(TAG, "uwbRangingResult: LEFT");
            }
        }


        // Rotate arrow and icon to show the direction
        // selectedAccessoryArrow.setRotation(position.getAzimuth());
        //selectedAccessoryCircle.setRotation(position.getAzimuth());
    }

    override fun uwbRangingCapabilities(
        distanceSupported: Boolean, angleSupported: Boolean, elevationSupported: Boolean
    ) {
        Log.d(
            TAG,
            "UWB ranging capabilities: distanceSupported=" + distanceSupported + ", angleSupported=" + angleSupported + ", elevationSupported=" + elevationSupported
        )
    }

    override fun uwbRangingComplete() {
        Log.d(TAG, "UWB ranging complete")
    }

    override fun uwbRangingError() {
        Log.e(TAG, "UWB ranging error occurred")
    }

    override fun uwbConnectDeviceDetails(name: String, mac: String, alias: String) {
        Log.d(TAG, "UWB device connected: name=$name, mac=$mac, alias=$alias")

    }

}
