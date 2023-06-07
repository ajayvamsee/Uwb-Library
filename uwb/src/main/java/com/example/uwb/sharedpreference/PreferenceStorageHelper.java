package com.example.uwb.sharedpreference;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 04:28
 */
public class PreferenceStorageHelper {

    private Context mContext;

    private static final String PREFS_NAME_APP = "MK_UWB_CONNECT_APP_SHARED_PREFERENCES";
    private static final String PREFS_NAME_UWB = "MK_UWB_CONNECT_UWB_SHARED_PREFERENCES";
    private static final String PREFS_NAME_DISTANCEALERT = "MK_UWB_CONNECT_DISTANCEALERT_SHARED_PREFERENCES";

    private static final String PREF_SHOW_PAIRING_INFO_KEY = "SHOW_PAIRING_INFO_KEY";

    private static final boolean DEFAULT_SHOW_PAIRING_INFO = true;

    private static final String PREF_SHOWPAIRINGINFO_KEY = "SHOWPAIRINGINFO_KEY";
    private static final String PREF_SHOWELEVATIONNOTSUPPORTED_KEY = "SHOWELEVATIONNOTSUPPORTED_KEY";
    private static final String PREF_SHOWMULTIPLESESSIONSNOTSUPPORTED_KEY = "SHOWMULTIPLESESSIONSNOTSUPPORTED_KEY";
    private static final String PREF_LOGSENABLED_KEY = "LOGSENABLED_KEY";
    private static final String PREF_UWBCHANNEL_KEY = "UWBCHANNEL_KEY";
    private static final String PREF_UWBPREAMBLEINDEX_KEY = "UWBPREAMBLEINDEX_KEY";
    private static final String PREF_UWBROLE_KEY = "UWBROLE_KEY";
    private static final String PREF_UWBCONFIGTYPE_KEY = "UWBCONFIGTYPE_KEY";
    private static final String PREF_DISTANCEALERTCLOSERANGETHRESHOLD_KEY = "DISTANCEALERTCLOSERANGETHRESHOLD_KEY";
    private static final String PREF_DISTANCEALERTFARRANGETHRESHOLD_KEY = "DISTANCEALERTFARRANGETHRESHOLD_KEY";

    private static final boolean DEFAULT_SHOWPAIRINGINFO = true;
    private static final boolean DEFAULT_SHOWELEVATIONNOTSUPPORTED = true;
    private static final boolean DEFAULT_SHOWMULTIPLESESSIONSNOTSUPPORTED = true;
    private static final boolean DEFAULT_LOGSENABLED = false;
    private static final int DEFAULT_UWBCHANNEL = 9;
    private static final int DEFAULT_UWBPREAMBLEINDEX = 10;
    private static final String DEFAULT_UWBROLE = "Controlee";
    private static final int DEFAULT_UWBCONFIGTYPE = 1;
    private static final int DEFAULT_DISTANCEALERTCLOSERANGETHRESHOLD = 100;
    private static final int DEFAULT_DISTANCEALERTFARRANGETHRESHOLD = 200;

    public PreferenceStorageHelper(Context context) {
        this.mContext = context;
    }

    public boolean getShowPairingInfo() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME_APP, MODE_PRIVATE);
        return prefs.getBoolean(PREF_SHOW_PAIRING_INFO_KEY, DEFAULT_SHOW_PAIRING_INFO);
    }

    public void setShowElevationNotSupported(boolean showElevationNotSupported) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME_APP, MODE_PRIVATE).edit();
        editor.putBoolean(PREF_SHOWELEVATIONNOTSUPPORTED_KEY, showElevationNotSupported);
        editor.apply();
    }

    public boolean getShowElevationNotSupported() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME_APP, MODE_PRIVATE);
        return prefs.getBoolean(PREF_SHOWELEVATIONNOTSUPPORTED_KEY, DEFAULT_SHOWELEVATIONNOTSUPPORTED);
    }

    public void setShowMultipleSessionsNotSupported(boolean showMultipleSessionsNotSupported) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME_APP, MODE_PRIVATE).edit();
        editor.putBoolean(PREF_SHOWMULTIPLESESSIONSNOTSUPPORTED_KEY, showMultipleSessionsNotSupported);
        editor.apply();
    }

    public boolean getShowMultipleSessionsNotSupported() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME_APP, MODE_PRIVATE);
        return prefs.getBoolean(PREF_SHOWMULTIPLESESSIONSNOTSUPPORTED_KEY, DEFAULT_SHOWMULTIPLESESSIONSNOTSUPPORTED);
    }

    public void setLogsEnabled(boolean logsEnabled) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE).edit();
        editor.putBoolean(PREF_LOGSENABLED_KEY, logsEnabled);
        editor.apply();
    }

    public boolean getLogsEnabled() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE);
        return prefs.getBoolean(PREF_LOGSENABLED_KEY, DEFAULT_LOGSENABLED);
    }

    public void setUwbChannel(int channel) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE).edit();
        editor.putInt(PREF_UWBCHANNEL_KEY, channel);
        editor.apply();
    }

    public int getUwbChannel() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE);
        return prefs.getInt(PREF_UWBCHANNEL_KEY, DEFAULT_UWBCHANNEL);
    }

    public void setUwbPreambleIndex(int preambleIndex) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE).edit();
        editor.putInt(PREF_UWBPREAMBLEINDEX_KEY, preambleIndex);
        editor.apply();
    }

    public int getUwbPreambleIndex() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE);
        return prefs.getInt(PREF_UWBPREAMBLEINDEX_KEY, DEFAULT_UWBPREAMBLEINDEX);
    }

    public void setUwbRole(String role) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE).edit();
        editor.putString(PREF_UWBROLE_KEY, role);
        editor.apply();
    }

    public String getUwbRole() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE);
        return prefs.getString(PREF_UWBROLE_KEY, DEFAULT_UWBROLE);
    }

    public void setUwbConfigType(int configType) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE).edit();
        editor.putInt(PREF_UWBCONFIGTYPE_KEY, configType);
        editor.apply();
    }

    public int getUwbConfigType() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE);
        return prefs.getInt(PREF_UWBCONFIGTYPE_KEY, DEFAULT_UWBCONFIGTYPE);
    }

    public void clearUwbSettings() {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME_UWB, MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    public int getDistanceAlertCloseRangeThreshold() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME_DISTANCEALERT, MODE_PRIVATE);
        return prefs.getInt(PREF_DISTANCEALERTCLOSERANGETHRESHOLD_KEY, DEFAULT_DISTANCEALERTCLOSERANGETHRESHOLD);
    }

    public void setDistanceAlertCloseRangeThreshold(int threshold) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME_DISTANCEALERT, MODE_PRIVATE).edit();
        editor.putInt(PREF_DISTANCEALERTCLOSERANGETHRESHOLD_KEY, threshold);
        editor.apply();
    }

    public int getDistanceAlertFarRangeThreshold() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME_DISTANCEALERT, MODE_PRIVATE);
        return prefs.getInt(PREF_DISTANCEALERTFARRANGETHRESHOLD_KEY, DEFAULT_DISTANCEALERTFARRANGETHRESHOLD);
    }

    public void setDistanceAlertFarRangeThreshold(int threshold) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME_DISTANCEALERT, MODE_PRIVATE).edit();
        editor.putInt(PREF_DISTANCEALERTFARRANGETHRESHOLD_KEY, threshold);
        editor.apply();
    }
}
