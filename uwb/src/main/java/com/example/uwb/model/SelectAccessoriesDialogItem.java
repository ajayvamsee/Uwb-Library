package com.example.uwb.model;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 03:42
 */
public class SelectAccessoriesDialogItem extends Accessory {

    private long timeStamp;
    private boolean isBonded;
    private boolean isSelected;

    public SelectAccessoriesDialogItem(String tagName, String macAddress, String alias, long timeStamp, boolean isBonded, boolean isSelected) {
        super(tagName, macAddress, alias);
        this.timeStamp = timeStamp;
        this.isBonded = isBonded;
        this.isSelected = isSelected;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isBonded() {
        return isBonded;
    }

    public void setBonded(boolean bonded) {
        isBonded = bonded;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}

