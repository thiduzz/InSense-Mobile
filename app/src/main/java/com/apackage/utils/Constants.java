package com.apackage.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by thidu on 16/06/2017.
 */

public class Constants {
    //Auth
    public static final String DEFAULT_PASSWORD_INSENSE_GLASS = "vizadvizad";
    //fragment names
    public static final String FRAGMENT_HOME = "HOME_FRAGMENT";
    public static final String FRAGMENT_APPS = "HOME_APPS";
    public static final String FRAGMENT_MAP = "HOME_MAP";
    public static final String FRAGMENT_DEVICES = "HOME_DEVICES";

    //Chamadas
    public static final String REQUEST_LOGIN = "login_request";
    public static final String REQUEST_SETTINGS = "settings_request";
    public static final String REQUEST_DEVICES = "devices_request";
    public static final String REQUEST_REFRESH_TOKEN = "token_request";
    public static final String REQUEST_VALIDATE_TOKEN = "validate_token";

    //Request codes
    public static final int PERMISSION_RC_MAPS = 405;

    public static final String CONNECTION_IP = "192.168.0.101";
    public static final int CONNECTION_PORT = 3333;
    //Wifi codes
    public static final int CONNECTION_GENERAL_ERROR = 1000;
    public static final int CONNECTION_ERROR = 1010;
    public static final int CONNECTION_CLOSED = 1020;
    //Hotspot codes
    public static final int HOTSPOT_GENERAL_ERROR = 2000;
    public static final int HOTSPOT_DEVICE_FOUND = 2200;
    public static final int HOTSPOT_DEVICE_NOTFOUND = 2404;
    public static final int HOTSPOT_DISABLED = 2400;

    //Device codes
    public static final int GLASS_STARTED = 3200;
    public static final int GLASS_AUDIO_RECORDING = 3201;
    public static final int GLASS_AUDIO_SAVED = 3202;
    public static final int GLASS_AUDIO_RECOGNIZED = 3203;
    public static final int GLASS_NOT_CONNECTED = 3400;
    public static final int GLASS_ERROR_CODE = 3500;

    public static final List<String> REGISTERED_IMACS =
            Collections.unmodifiableList(Arrays.asList("60:01:94:41:2f:84"));

    //File paths
    public static final String RECORDED_AUDIO_FILE_PATH = "/recorded.wav";
}
