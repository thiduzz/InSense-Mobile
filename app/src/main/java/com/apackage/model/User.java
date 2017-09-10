package com.apackage.model;

/**
 * Created by thidu on 13/06/2017.
 */

import java.io.Serializable;

public class User implements Serializable  {


    public int id;
    public String login;
    public String token;
    public String name;
    public String refresh_token;
    public boolean is_connected;


    public User()
    {

    }

    public User(int id, String login, String token, String name, String refresh_token) {
        this.id = id;
        this.login = login;
        this.token = token;
        this.name = name;
        this.refresh_token = refresh_token;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public void setRefreshToken(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public boolean isConnected() {
        return is_connected;
    }

    public void setIsConnected(boolean is_connected) {
        this.is_connected = is_connected;
    }
}
