package com.example.uwblibrary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.*
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.uwb.uwbcontrol.UwbRangingController
import com.example.uwb.bluetooth.BluetoothLEManagerHelper
import com.example.uwb.location.LocationManagerHelper
import com.example.uwb.manager.UwbManagerHelper
import com.example.uwb.permissions.PermissionHelper
import com.example.uwb.sharedpreference.PreferenceStorageHelper
import com.example.uwblibrary.databinding.ActivityUwbactivityBinding
import java.util.Arrays

class MainActivity : AppCompatActivity(), UwbRangingController.UWBCallBack {

    private lateinit var uwbRangingController: UwbRangingController

    private lateinit var binding: ActivityUwbactivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_uwbactivity);

        val bluetoothLEManagerHelper = BluetoothLEManagerHelper(this)
        val preferenceStorageHelper = PreferenceStorageHelper(this)
        val locationLEManagerHelper = LocationManagerHelper(this)
        val permissionHelper = PermissionHelper(this)
        val uwbLEManagerHelper = UwbManagerHelper(this)

        checkUwbSupported()

        checkPermission()




        uwbRangingController = UwbRangingController(
            bluetoothLEManagerHelper,
            preferenceStorageHelper,
            uwbLEManagerHelper,
            locationLEManagerHelper,
            permissionHelper
        )




    }

    private fun startProcess(){
        uwbRangingController.apply {
            registerUwbCallBack(this@MainActivity)
            initListener()
            uwbBleStartScan()
        }

        binding.clUwbDetails.visibility = View.GONE
        binding.uwbLoading.visibility = View.VISIBLE
        binding.uwbLoading.playAnimation()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }


    override fun onResume() {
        super.onResume()
        binding.switchUwb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                uwbRangingController.initListener()
                uwbRangingController.uwbBleStartScan()
                binding.clUwbDetails.visibility = View.GONE
                binding.uwbLoading.visibility = View.VISIBLE
                binding.uwbLoading.playAnimation()
            } else {
                binding.clUwbDetails.visibility = View.VISIBLE
                binding.uwbLoading.cancelAnimation()
                binding.uwbLoading.visibility = View.GONE
                uwbRangingController.deinit()
                binding.tvDeviceName.text = ""
                binding.tvAddress.text = ""
                binding.tvDistance.text = "" + 0
                binding.tvAngle.text = "" + 0
                binding.tvElevation.text = "" + 0
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }


    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        //uwbRangingController.deinit();
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        uwbRangingController.deinit()
    }


    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart")
    }

    private fun checkUwbSupported() {
        /**
         * Check if device supports Ultra-wideband
         */
        val packageManager = applicationContext.packageManager
        val deviceSupportsUwb = packageManager.hasSystemFeature("android.hardware.uwb")
        if (!deviceSupportsUwb) {
            Log.e(TAG, "Device does not support Ultra-wideband")
            Toast.makeText(applicationContext, "Device does not support UWB", Toast.LENGTH_SHORT)
                .show()
            binding.switchUwb.isEnabled = false
        } else {
            binding.switchUwb.isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allPermissionsGranted = Arrays.stream(grantResults)
                .allMatch { result: Int -> result == PackageManager.PERMISSION_GRANTED }
            if (allPermissionsGranted) {
                //dooropenSplash()
                startProcess()
                Log.d(TAG, "onRequestPermissionsResult: PERMISSION_GRANTED")
            } else {
                handlePermissionDenied()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun handlePermissionDenied() {
        val permissionsToCheck: Array<String> = if (VERSION.SDK_INT >= VERSION_CODES.S) {
            permissions12
        } else if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            permissions13
        } else {
            permissions
        }
        if (checkPermissions(permissionsToCheck)) {
            //dooropenSplash()
            startProcess()
            Log.d(TAG, "checkPermission: calling InitADOModule")
        } else if (shouldShowRationale(permissionsToCheck)) {
            showPermissionRationaleDialog(permissionsToCheck)
        } else {
            showPermissionSettingsDialog()
        }
    }

    private fun checkPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun shouldShowRationale(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true
            }
        }
        return false
    }

    private fun showPermissionRationaleDialog(permissions: Array<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permission Required")
        builder.setMessage("Please grant the necessary permissions to use this feature")
        builder.setPositiveButton(
            "OK"
        ) { _, _ ->
            ActivityCompat.requestPermissions(
                this@MainActivity, permissions, PERMISSION_REQUEST_CODE
            )
        }
        builder.setNegativeButton(
            "Cancel"
        ) { _, _ -> finish() }
        val dialog = builder.create()
        dialog.show()
        val permissionNames = permissions.contentToString()
        Toast.makeText(
            this, "Please grant the following permissions: $permissionNames", Toast.LENGTH_LONG
        ).show()
    }

    private fun showPermissionSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permission Required")
        builder.setMessage("Please grant the necessary permissions to use this feature")
        builder.setPositiveButton(
            "Open Settings"
        ) { _, _ ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { _, _ -> finish() }
        val dialog = builder.create()
        dialog.show()
    }


    // UWB callbacks
    override fun uwbRangingStarted() {
        Log.d(TAG, "UWB ranging started")
        binding.uwbLoading.visibility = View.GONE
        binding.clUwbDetails.visibility = View.VISIBLE
    }

    override fun uwbRangingResult(distance: Int, angle: Int, elevation: Int) {
        Log.d(
            TAG, "UWB ranging result: distance=$distance, angle=$angle, elevation=$elevation"
        )
        //binding.tvDistance.setText(""+distance);
        binding.tvAngle.text = "" + angle
        binding.tvElevation.text = "" + elevation
        if (distance >= 100) {
            val meters = distance / 100
            val centimeters = distance % 100
            binding.tvDistance.text = "$meters.$centimeters"
            binding.tvCMorM.text = "M"
        } else {
            binding.tvDistance.text = "" + distance
            binding.tvCMorM.text = "cms"


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
        binding.tvDeviceName.text = name
        binding.tvAddress.text = mac
    }

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName

        const val PERMISSION_REQUEST_CODE = 1234

        // Define the Bluetooth permission as a string
        private val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        private val permissions12 = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.UWB_RANGING
        )

        @RequiresApi(api = VERSION_CODES.TIRAMISU)
        private val permissions13 = arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.UWB_RANGING
        )
    }

    private fun checkPermission() {
        if (VERSION.SDK_INT > VERSION_CODES.S) {
            Log.d(TAG, "12 >")
            val missingPermissions: MutableList<String> = ArrayList()
            for (permission in permissions12) {
                if (ContextCompat.checkSelfPermission(
                        this, permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(
                        TAG, "checkPermission: $permission"
                    )
                    missingPermissions.add(permission)
                }
            }
            if (VERSION.SDK_INT == VERSION_CODES.TIRAMISU) {
                Log.d(TAG, "13 >")
                for (permission in permissions13) {
                    if (ContextCompat.checkSelfPermission(
                            this, permission
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        missingPermissions.add(permission)
                    }
                }
            }
            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this, missingPermissions.toTypedArray<String>(), PERMISSION_REQUEST_CODE
                )
            } else {
                //intiADOModule();
                //dooropenSplash()
                startProcess()
                Log.d(
                    TAG, "checkPermission: calling InitADOModule"
                )
            }
        } else {
            Log.d(TAG, "< 12 ")
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, permissions, PERMISSION_REQUEST_CODE
                )
            } else {
                //intiADOModule();
                // dooropenSplash()
                startProcess()
                Log.d(
                    TAG, "checkPermission: calling InitADOModule"
                )
            }
        }
    }

}