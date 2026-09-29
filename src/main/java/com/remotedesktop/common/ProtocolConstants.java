package com.remotedesktop.common;

/**
 * Các hằng số dùng chung giữa Server và Client.
 * Giao thức truyền thông qua TCP/IP.
 */
public class ProtocolConstants {

    // --- Cổng mặc định ---
    public static final int DEFAULT_PORT = 5900;

    // --- Lệnh giao thức ---
    public static final String CMD_SCREEN_CAPTURE = "SCREEN_CAPTURE";
    public static final String CMD_MOUSE_MOVE     = "MOUSE_MOVE";
    public static final String CMD_MOUSE_CLICK    = "MOUSE_CLICK";
    public static final String CMD_MOUSE_PRESS    = "MOUSE_PRESS";
    public static final String CMD_MOUSE_RELEASE  = "MOUSE_RELEASE";
    public static final String CMD_MOUSE_SCROLL   = "MOUSE_SCROLL";
    public static final String CMD_KEY_PRESS      = "KEY_PRESS";
    public static final String CMD_KEY_RELEASE    = "KEY_RELEASE";
    public static final String CMD_KEY_TYPE       = "KEY_TYPE";
    public static final String CMD_DISCONNECT     = "DISCONNECT";
    public static final String CMD_SCREEN_INFO    = "SCREEN_INFO";
    public static final String CMD_CLIPBOARD_SEND = "CLIPBOARD_SEND";
    public static final String CMD_CLIPBOARD_GET  = "CLIPBOARD_GET";
    public static final String CMD_FILE_TRANSFER  = "FILE_TRANSFER";

    // --- Phản hồi ---
    public static final String RESP_OK    = "OK";
    public static final String RESP_ERROR = "ERROR";

    // --- Chất lượng ảnh ---
    public static final float IMAGE_QUALITY_HIGH   = 0.8f;
    public static final float IMAGE_QUALITY_MEDIUM = 0.5f;
    public static final float IMAGE_QUALITY_LOW    = 0.3f;

    // --- Kích thước buffer ---
    public static final int BUFFER_SIZE = 65536;

    private ProtocolConstants() {
    }
}
