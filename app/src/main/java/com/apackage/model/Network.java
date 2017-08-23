package com.apackage.model;

/**
 * Created by thidu on 8/22/2017.
 */

public class Network {

    public static final int CONNECTED = 1;
    public String ssid;
    public String mac;
    public String password;
    public boolean connected = true;

    public Network(String ssid, String mac) {
        this.ssid = ssid;
        this.mac = mac;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isConnected() {
        return connected;
    }
}
