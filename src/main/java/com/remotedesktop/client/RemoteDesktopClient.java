package com.remotedesktop.client;

import com.remotedesktop.common.NetworkUtils;
import com.remotedesktop.common.ProtocolConstants;

import java.awt.Dimension;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * TCP Client: kết nối tới Server, nhận ảnh chụp màn hình
 * và gửi lệnh điều khiển chuột/bàn phím.
 */
public class RemoteDesktopClient {

    private String host;
    private int port;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private Consumer<byte[]> screenCallback;
    private Consumer<Dimension> screenInfoCallback;
    private Consumer<String> logCallback;
    private Consumer<Boolean> connectionCallback;

    public RemoteDesktopClient() {
    }

    public void setScreenCallback(Consumer<byte[]> callback) {
        this.screenCallback = callback;
    }

    public void setScreenInfoCallback(Consumer<Dimension> callback) {
        this.screenInfoCallback = callback;
    }

    public void setLogCallback(Consumer<String> callback) {
        this.logCallback = callback;
    }

    public void setConnectionCallback(Consumer<Boolean> callback) {
        this.connectionCallback = callback;
    }

    private void log(String message) {
        System.out.println("[Client] " + message);
        if (logCallback != null) {
            logCallback.accept(message);
        }
    }

    /**
     * Kết nối tới Server.
     */
    public void connect(String host, int port) {
        this.host = host;
        this.port = port;

        new Thread(() -> {
            try {
                log("Đang kết nối tới " + host + ":" + port + "...");
                socket = new Socket(host, port);
                dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                connected.set(true);

                log("Đã kết nối thành công tới " + host + ":" + port);
                if (connectionCallback != null) {
                    connectionCallback.accept(true);
                }

                // Nhận dữ liệu từ Server
                receiveLoop();

            } catch (IOException e) {
                log("Không thể kết nối: " + e.getMessage());
                connected.set(false);
                if (connectionCallback != null) {
                    connectionCallback.accept(false);
                }
            }
        }, "ClientConnectThread").start();
    }

    /**
     * Vòng lặp nhận dữ liệu từ Server.
     */
    private void receiveLoop() {
        try {
            while (connected.get() && !socket.isClosed()) {
                String command = NetworkUtils.receiveCommand(dis);

                switch (command) {
                    case ProtocolConstants.CMD_SCREEN_INFO -> {
                        String info = NetworkUtils.receiveCommand(dis);
                        String[] parts = info.split("x");
                        int w = Integer.parseInt(parts[0]);
                        int h = Integer.parseInt(parts[1]);
                        log("Màn hình Server: " + w + "x" + h);
                        if (screenInfoCallback != null) {
                            screenInfoCallback.accept(new Dimension(w, h));
                        }
                    }
                    case ProtocolConstants.CMD_SCREEN_CAPTURE -> {
                        byte[] imageData = NetworkUtils.receiveBytes(dis);
                        if (screenCallback != null) {
                            screenCallback.accept(imageData);
                        }
                    }
                    default -> log("Lệnh không xác định từ server: " + command);
                }
            }
        } catch (IOException e) {
            if (connected.get()) {
                log("Mất kết nối: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    /**
     * Gửi lệnh di chuyển chuột.
     */
    public void sendMouseMove(int x, int y) {
        sendCommand(ProtocolConstants.CMD_MOUSE_MOVE + "|" + x + "|" + y);
    }

    /**
     * Gửi lệnh click chuột.
     */
    public void sendMouseClick(int button, int clickCount) {
        sendCommand(ProtocolConstants.CMD_MOUSE_CLICK + "|" + button + "|" + clickCount);
    }

    /**
     * Gửi lệnh nhấn chuột.
     */
    public void sendMousePress(int button) {
        sendCommand(ProtocolConstants.CMD_MOUSE_PRESS + "|" + button);
    }

    /**
     * Gửi lệnh thả chuột.
     */
    public void sendMouseRelease(int button) {
        sendCommand(ProtocolConstants.CMD_MOUSE_RELEASE + "|" + button);
    }

    /**
     * Gửi lệnh cuộn chuột.
     */
    public void sendMouseScroll(int amount) {
        sendCommand(ProtocolConstants.CMD_MOUSE_SCROLL + "|" + amount);
    }

    /**
     * Gửi lệnh nhấn phím.
     */
    public void sendKeyPress(int keyCode) {
        sendCommand(ProtocolConstants.CMD_KEY_PRESS + "|" + keyCode);
    }

    /**
     * Gửi lệnh thả phím.
     */
    public void sendKeyRelease(int keyCode) {
        sendCommand(ProtocolConstants.CMD_KEY_RELEASE + "|" + keyCode);
    }

    /**
     * Gửi clipboard text.
     */
    public void sendClipboard(String text) {
        sendCommand(ProtocolConstants.CMD_CLIPBOARD_SEND + "|" + text);
    }

    /**
     * Gửi lệnh qua TCP.
     */
    private void sendCommand(String command) {
        if (!connected.get() || dos == null) return;
        new Thread(() -> {
            try {
                synchronized (dos) {
                    NetworkUtils.sendCommand(dos, command);
                }
            } catch (IOException e) {
                log("Lỗi gửi lệnh: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Ngắt kết nối.
     */
    public void disconnect() {
        if (!connected.getAndSet(false)) return;
        try {
            if (dos != null) {
                NetworkUtils.sendCommand(dos, ProtocolConstants.CMD_DISCONNECT);
            }
        } catch (IOException ignored) {
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        log("Đã ngắt kết nối");
        if (connectionCallback != null) {
            connectionCallback.accept(false);
        }
    }

    public boolean isConnected() {
        return connected.get();
    }
}
