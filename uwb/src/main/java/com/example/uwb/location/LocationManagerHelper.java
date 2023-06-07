package com.example.uwb.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 04:45
 */
public class LocationManagerHelper {

    private static final String TAG = LocationManagerHelper.class.getSimpleName();

    private Context mContext;
    private LocationManager locationManager;

    private Listener mListener = null;

    public interface Listener {
        void onLocationStateChanged(boolean enabled);
    }

    public LocationManagerHelper(final Context context) {
        this.mContext = context;

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean isSupported() {
        return locationManager != null;
    }

    public boolean isEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void registerListener(Listener listener) {
        this.mListener = listener;

        IntentFilter filter = new IntentFilter(LocationManager.MODE_CHANGED_ACTION);
        mContext.registerReceiver(locationStateChangeReceiver, filter);
    }

    public void unregisterListener() {
        this.mListener = null;
        mContext.unregisterReceiver(locationStateChangeReceiver);
    }

    private void onStateChanged (final boolean enabled) {
        // Send callback to app on the UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mListener != null) {
                mListener.onLocationStateChanged(enabled);
            }
        });
    }

    private final BroadcastReceiver locationStateChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(LocationManager.MODE_CHANGED_ACTION)) {
                final boolean enabled = intent.getBooleanExtra(LocationManager.EXTRA_LOCATION_ENABLED, false);
                onStateChanged(enabled);
            }
        }
    };
}
