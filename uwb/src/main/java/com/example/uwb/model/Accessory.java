package com.example.uwb.model;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 03:31
 */
public class Accessory {

    private String name;
    private String mac;
    private String alias;

    public Accessory() {
    }

    public Accessory(String name, String mac, String alias) {
        this.name = name;
        this.mac = mac;
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
