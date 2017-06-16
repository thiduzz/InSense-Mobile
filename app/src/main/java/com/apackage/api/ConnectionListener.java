package com.apackage.api;

import java.util.Map;

/**
 * Created by thidu on 18/11/2016.
 */
public interface ConnectionListener {
    void onConnectionError();
    void onConnectionSuccess();
    void onConnectionError(Map<String, String> result);
    void onConnectionSuccess(Map<String, Object> result);
}
