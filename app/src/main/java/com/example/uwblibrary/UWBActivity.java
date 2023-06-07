package com.example.uwblibrary;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.example.uwb.UwbRangingController;
import com.example.uwb.bluetooth.BluetoothLEManagerHelper;
import com.example.uwb.location.LocationManagerHelper;
import com.example.uwb.manager.UwbManagerHelper;
import com.example.uwb.permissions.PermissionHelper;
import com.example.uwb.sharedpreference.PreferenceStorageHelper;
import com.example.uwblibrary.databinding.ActivityUwbactivityBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class UWBActivity extends AppCompatActivity implements UwbRangingController.UWBCallBack {

    private static final String TAG = "UWBActivityLogs";
    private static final int PERMISSION_REQUEST_CODE = 1234;

    private Context context;

    private UwbRangingController uwbRangingController;

    private ActivityUwbactivityBinding binding;

    private static final String[] permissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private static final String[] permissions12 = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private static final String[] permissions13 = new String[]{
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.UWB_RANGING
    };

    private UwbService uwbService ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_uwbactivity);

        //viewModel = new ViewModelProvider(this).get(DeviceStatusViewModel.class);


        BluetoothLEManagerHelper bluetoothLEManagerHelper = new BluetoothLEManagerHelper(this);
        PreferenceStorageHelper preferenceStorageHelper = new PreferenceStorageHelper(this);
        LocationManagerHelper locationManagerHelper = new LocationManagerHelper(this);
        PermissionHelper permissionHelper = new PermissionHelper(this);
        UwbManagerHelper uwbManagerHelper = new UwbManagerHelper(this);

        checkUwbSupported();

        checkPermission();




        uwbRangingController = new UwbRangingController(bluetoothLEManagerHelper,
                preferenceStorageHelper,
                uwbManagerHelper,
                locationManagerHelper,
                permissionHelper);


        uwbRangingController.registerUwbCallBack(this);

        // For testing call the all default
        uwbRangingController.initListener();
        uwbRangingController.uwbBleStartScan();

        binding.clUwbDetails.setVisibility(View.GONE);
        binding.uwbLoading.setVisibility(View.VISIBLE);
        binding.uwbLoading.playAnimation();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }


    @Override
    protected void onResume() {
        super.onResume();

        binding.switchUwb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    uwbRangingController.initListener();
                    uwbRangingController.uwbBleStartScan();

                    binding.clUwbDetails.setVisibility(View.GONE);
                    binding.uwbLoading.setVisibility(View.VISIBLE);
                    binding.uwbLoading.playAnimation();
                } else {
                    binding.clUwbDetails.setVisibility(View.VISIBLE);

                    binding.uwbLoading.cancelAnimation();
                    binding.uwbLoading.setVisibility(View.GONE);
                    uwbRangingController.deinit();

                    binding.tvDeviceName.setText("");
                    binding.tvAddress.setText("");
                    binding.tvDistance.setText("" + 0);
                    binding.tvAngle.setText("" + 0);
                    binding.tvElevation.setText("" + 0);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        //uwbRangingController.deinit();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        uwbRangingController.deinit();
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    private void checkUwbSupported() {
        /**
         * Check if device supports Ultra-wideband
         */
        PackageManager packageManager = getApplicationContext().getPackageManager();
        boolean deviceSupportsUwb = packageManager.hasSystemFeature("android.hardware.uwb");

        if (!deviceSupportsUwb) {
            Log.e(TAG, "Device does not support Ultra-wideband");
            Toast.makeText(getApplicationContext(), "Device does not support UWB", Toast.LENGTH_SHORT).show();
            binding.switchUwb.setEnabled(false);
        } else {
            binding.switchUwb.setEnabled(true);
        }

    }


    // UWB callbacks
    @Override
    public void uwbRangingStarted() {
        Log.d(TAG, "UWB ranging started");
        binding.uwbLoading.setVisibility(View.GONE);
        binding.clUwbDetails.setVisibility(View.VISIBLE);
    }

    @Override
    public void uwbRangingResult(int distance, int angle, int elevation) {
        Log.d(TAG, "UWB ranging result: distance=" + distance + ", angle=" + angle + ", elevation=" + elevation);
        //binding.tvDistance.setText(""+distance);
        binding.tvAngle.setText("" + angle);
        binding.tvElevation.setText("" + elevation);

        if (distance >= 100) {
            int meters = distance / 100;
            int centimeters = distance % 100;
            binding.tvDistance.setText("" + meters + "." + centimeters);
            binding.tvCMorM.setText("M");
        } else {
            binding.tvDistance.setText("" + distance);
            binding.tvCMorM.setText("cms");


            /*boolean doorState = true;

            if(doorState && distance ){
                UIManager.getInstance().open((byte) 1);
                doorState = false;
            } else if(distance > 100){
                doorState = true;
            }
*/
            /*Log.d(TAG, "uwbRangingResult: getAdoOperationResponse "+Repository.getInstance().getAdoOperationResponse().getValue());
            if(Repository.getInstance().getAdoOperationResponse().getValue() != OperationResponseEnum.OPENED.getValue()){
                UIManager.getInstance().open((byte) 1);
                Log.d(TAG, "uwbRangingResult: dooropen cmd send is coz is door is closed");
            }else {
                Log.d(TAG, "uwbRangingResult: door is already opened");
            }*/
        }

        boolean doorState = true;

        if (doorState && distance < 300) {
            //UIManager.getInstance().open((byte) 1);
            doorState = false;
        } else if (distance > 100) {
            doorState = true;
        }


        double scaleFactor;
        if (distance < 8.0) {
            scaleFactor = 1 - (distance * 0.1);
        } else {
            scaleFactor = 0.2;
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

    @Override
    public void uwbRangingCapabilities(boolean distanceSupported, boolean angleSupported, boolean elevationSupported) {
        Log.d(TAG, "UWB ranging capabilities: distanceSupported=" + distanceSupported +
                ", angleSupported=" + angleSupported +
                ", elevationSupported=" + elevationSupported);
    }

    @Override
    public void uwbRangingComplete() {
        Log.d(TAG, "UWB ranging complete");
    }

    @Override
    public void uwbRangingError() {
        Log.e(TAG, "UWB ranging error occurred");
    }

    @Override
    public void uwbConnectDeviceDetails(String name, String mac, String alias) {
        Log.d(TAG, "UWB device connected: name=" + name + ", mac=" + mac + ", alias=" + alias);
        binding.tvDeviceName.setText(name);
        binding.tvAddress.setText(mac);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = Arrays.stream(grantResults).allMatch(result -> result == PackageManager.PERMISSION_GRANTED);

            if (allPermissionsGranted) {
                //dooropenSplash();
                Log.d(TAG, "onRequestPermissionsResult: PERMISSION_GRANTED");
            } else {
                handlePermissionDenied();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void handlePermissionDenied() {
        String[] permissionsToCheck;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToCheck = permissions12;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToCheck = permissions13;
        } else {
            permissionsToCheck = permissions;
        }

        if (checkPermissions(permissionsToCheck)) {
            // dooropenSplash();
            Log.d(TAG, "checkPermission: calling InitADOModule");
        } else if (shouldShowRationale(permissionsToCheck)) {
            showPermissionRationaleDialog(permissionsToCheck);
        } else {
            showPermissionSettingsDialog();
        }
    }

    private boolean checkPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldShowRationale(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    private void showPermissionRationaleDialog(String[] permissions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("Please grant the necessary permissions to use this feature");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(UWBActivity.this, permissions, PERMISSION_REQUEST_CODE);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        String permissionNames = Arrays.toString(permissions);
        Toast.makeText(this, "Please grant the following permissions: " + permissionNames, Toast.LENGTH_LONG).show();
    }

    private void showPermissionSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("Please grant the necessary permissions to use this feature");

        builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                finish();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            Log.d(TAG, "12 >");
            List<String> missingPermissions = new ArrayList<>();

            for (String permission : permissions12) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "checkPermission: " + permission);
                    missingPermissions.add(permission);
                }
            }

            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
                Log.d(TAG, "13 >");

                for (String permission : permissions13) {
                    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                        missingPermissions.add(permission);
                    }
                }
            }

            if (!missingPermissions.isEmpty()) {
                ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            } else {
                //intiADOModule();
                //dooropenSplash();
                Log.d(TAG, "checkPermission: calling InitADOModule");
            }
        } else {
            Log.d(TAG, "< 12 ");

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            } else {
                //intiADOModule();
                //dooropenSplash();
                Log.d(TAG, "checkPermission: calling InitADOModule");
            }
        }
    }
}