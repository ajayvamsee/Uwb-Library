package com.example.uwb.uwbcontrol;

import com.example.uwb.model.Accessory;
import com.example.uwb.model.Position;
import com.example.uwb.tools.IBaseObservableView;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 05:35
 */
public interface UwbRangingView extends IBaseObservableView<UwbRangingView.Listener> {
    interface Listener {
        void onBackPressed();

        void onSelectAccessoryButtonClicked();

        void onSelectedAccessoryRemoveClicked();

        void onSelectedAccessorySelectAccessoryClicked();
    }

    void showSelectAccessoryText();

    void showSelectAccessory(Accessory accessory);

    void updateSelectedAccessoryPosition(Accessory accessory, Position position);
}
