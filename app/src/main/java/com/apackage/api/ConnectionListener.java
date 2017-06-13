package com.apackage.api;

import org.json.JSONArray;

/**
 * Created by thidu on 18/11/2016.
 */
public interface ConnectionListener {
    void onConnectionError();
    void onConnectionSuccess(JSONArray jsonArray);
}
