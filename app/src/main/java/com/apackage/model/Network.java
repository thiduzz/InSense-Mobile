package com.apackage.model;

import android.os.Handler;

import java.io.Serializable;

/**
 * Created by thidu on 8/22/2017.
 */

public class Network implements Serializable{

    public String ssid;
    public String mac;
    public String password;
    public String address;
    public int port;
    public boolean connected;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    public Network() {
    }

    public Network(String ssid, String mac) {
        this.ssid = ssid;
        this.mac = mac;
    }

    public Network(String address, int port)
    {
        this.address = address;
        this.port = port;
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

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }

}
