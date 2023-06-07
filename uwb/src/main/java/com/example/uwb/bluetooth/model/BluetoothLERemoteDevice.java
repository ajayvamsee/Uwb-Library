package com.example.uwb.bluetooth.model;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 04:58
 */
public class BluetoothLERemoteDevice {

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic txCharacteristic;
    private BluetoothGattCharacteristic rxCharacteristic;

    public BluetoothLERemoteDevice() {

    }

    public BluetoothLERemoteDevice(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic txCharacteristic, BluetoothGattCharacteristic rxCharacteristic) {
        this.bluetoothGatt = bluetoothGatt;
        this.txCharacteristic = txCharacteristic;
        this.rxCharacteristic = rxCharacteristic;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }

    public BluetoothGattCharacteristic getTxCharacteristic() {
        return txCharacteristic;
    }

    public void setTxCharacteristic(BluetoothGattCharacteristic txCharacteristic) {
        this.txCharacteristic = txCharacteristic;
    }

    public BluetoothGattCharacteristic getRxCharacteristic() {
        return rxCharacteristic;
    }

    public void setRxCharacteristic(BluetoothGattCharacteristic rxCharacteristic) {
        this.rxCharacteristic = rxCharacteristic;
    }
}
