package com.apackage.model;

/**
 * Created by thidu on 13/06/2017.
 */

import java.io.Serializable;

public class User implements Serializable  {

    public String login;
    public String token;
    public String refresh_token;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
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

}
