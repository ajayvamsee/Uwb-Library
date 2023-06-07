package com.example.uwb;

import android.util.Log;
import android.view.View;

import com.example.uwb.model.Accessory;
import com.example.uwb.model.Position;
import com.example.uwb.tools.BaseObservable;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 05:38
 */
public class UwbRangingViewImpl extends BaseObservable<UwbRangingView.Listener> implements UwbRangingView {


    private void notifySelectAccessoryClicked() {
        for (Listener listener : getListeners()) {
            listener.onSelectAccessoryButtonClicked();
        }
    }

    private void notifySelectedAccessoryRemoveClicked() {
        for (Listener listener : getListeners()) {
            listener.onSelectedAccessoryRemoveClicked();
        }
    }

    private void notifySelectedAccessorySelectAccessoryClicked() {
        for (Listener listener : getListeners()) {
            listener.onSelectedAccessorySelectAccessoryClicked();
        }
    }

    @Override
    public void showSelectAccessoryText() {
        /*findViewById(R.id.uwbranging_selectaccessory_text).setVisibility(View.VISIBLE);
        findViewById(R.id.uwbranging_selectaccessory_button).setVisibility(View.VISIBLE);
        findViewById(R.id.uwbranging_selectedaccessory_frame).setVisibility(View.GONE);*/
    }

    @Override
    public void showSelectAccessory(Accessory accessory) {

    }


    @Override
    public void updateSelectedAccessoryPosition(Accessory accessory, Position position) {
        //findViewById(R.id.uwbranging_selectedaccessoryposition_frame).setVisibility(View.VISIBLE);

        double scaleFactor;
        if (position.getDistance() < 8.0) {
            scaleFactor = 1 - (position.getDistance() * 0.1);
        } else {
            scaleFactor = 0.2;
        }


        if (position.getAzimuth() >= -10 && position.getAzimuth() <= 10) {
            if (accessory.getAlias() != null && !accessory.getAlias().isEmpty()) {
                Log.d("TAG", "updateSelectedAccessoryPosition:" + accessory.getAlias() + "," + (int) (position.getDistance() * 100) + "," + (int) position.getAzimuth());
            } else {
                Log.d("TAG", "updateSelectedAccessoryPosition: selectedaccessory_distanceahead" + accessory.getName() + "," + (int) (position.getDistance() * 100) + "," + (int) position.getAzimuth());
            }
        } else {
            if (position.getAzimuth() >= 0) {
                if (accessory.getAlias() != null && !accessory.getAlias().isEmpty()) {
                    Log.d("TAG", "updateSelectedAccessoryPosition: selectedaccessory_distancedirection" + accessory.getAlias() + ", " + (int) (position.getDistance() * 100) + "," + "distancedirection_right," + (int) position.getAzimuth());
                } else {
                    Log.d("TAG", "updateSelectedAccessoryPosition: selectedaccessory_distancedirection," + accessory.getName() + ", " + (int) (position.getDistance() * 100) + "," + "istancedirection_right)," + (int) position.getAzimuth());
                }
            } else {
                if (accessory.getAlias() != null && !accessory.getAlias().isEmpty()) {
                    Log.d("TAG", "updateSelectedAccessoryPosition: selectedaccessory_distancedirection, " + accessory.getAlias() + ", " + (int) (position.getDistance() * 100) + "," + "distancedirection_left)," + (int) position.getAzimuth());
                } else {
                    Log.d("TAG", "updateSelectedAccessoryPosition: selectedaccessory_distancedirection," + accessory.getName() + ", " + (int) (position.getDistance() * 100) + ", " + "distancedirection_left)," + (int) position.getAzimuth());
                }
            }
        }
        Log.d("TAG", "updateSelectedAccessoryPosition: " + position.getAzimuth() + "azimuth" + position.getAzimuth());
    }

    @Override
    public View getRootView() {
        return null;
    }
}
