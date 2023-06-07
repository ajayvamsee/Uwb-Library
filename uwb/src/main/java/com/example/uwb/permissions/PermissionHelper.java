package com.example.uwb.permissions;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.uwb.tools.BaseObservable;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 06:10
 */
public class PermissionHelper extends BaseObservable<PermissionHelper.Listener> {

    public interface Listener {
        void onPermissionGranted(String permission, int requestCode);

        void onPermissionDeclined(String permission, int requestCode);

        void onPermissionDeclinedDontAskAgain(String permission, int requestCode);
    }

    private final Activity mActivity;

    public PermissionHelper(Activity activity) {
        mActivity = activity;
    }

    public boolean hasPermission(String permission) {
        return ActivityCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission(String permission, int requestCode) {
        ActivityCompat.requestPermissions(mActivity, new String[]{permission}, requestCode);
    }

    public void requestPermissions(String[] permissionList, int requestCode) {
        ActivityCompat.requestPermissions(mActivity, permissionList, requestCode);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissionsList, @NonNull int[] grantResults) {
        if (permissionsList.length < 1) {
            return;
        }

        for (int i = 0; i < permissionsList.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                notifyPermissionGranted(permissionsList[i], requestCode);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permissionsList[i])) {
                    notifyPermissionDeclined(permissionsList[i], requestCode);
                } else {
                    notifyPermissionDeclinedDontAskAgain(permissionsList[i], requestCode);
                }
            }
        }
    }

    private void notifyPermissionDeclinedDontAskAgain(String permission, int requestCode) {
        for (Listener listener : getListeners()) {
            listener.onPermissionDeclinedDontAskAgain(permission, requestCode);
        }
    }

    private void notifyPermissionDeclined(String permission, int requestCode) {
        for (Listener listener : getListeners()) {
            listener.onPermissionDeclined(permission, requestCode);
        }
    }

    private void notifyPermissionGranted(String permission, int requestCode) {
        for (Listener listener : getListeners()) {
            listener.onPermissionGranted(permission, requestCode);
        }
    }
}
