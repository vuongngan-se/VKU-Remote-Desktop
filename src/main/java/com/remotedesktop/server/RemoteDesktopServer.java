package com.remotedesktop.server;

import com.remotedesktop.common.NetworkUtils;
import com.remotedesktop.common.ProtocolConstants;
import com.remotedesktop.common.ScreenCaptureUtils;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * TCP Server: chấp nhận kết nối từ Client,
 * liên tục gửi ảnh chụp màn hình và nhận lệnh điều khiển.
 */
public class RemoteDesktopServer {

    private int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Robot robot;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private float imageQuality = ProtocolConstants.IMAGE_QUALITY_MEDIUM;
    private int captureInterval = 50; // ms giữa các lần chụp

    private Consumer<String> logCallback;
    private Consumer<Boolean> connectionCallback;

    public RemoteDesktopServer(int port) {
        this.port = port;
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void setLogCallback(Consumer<String> callback) {
        this.logCallback = callback;
    }

    public void setConnectionCallback(Consumer<Boolean> callback) {
        this.connectionCallback = callback;
    }

    public void setImageQuality(float quality) {
        this.imageQuality = quality;
    }

    public void setCaptureInterval(int interval) {
        this.captureInterval = interval;
    }

    private void log(String message) {
        System.out.println("[Server] " + message);
        if (logCallback != null) {
            logCallback.accept(message);
        }
    }

    /**
     * Khởi động server, lắng nghe kết nối trên port.
     */
    public void start() {
        running.set(true);
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Server đang lắng nghe trên cổng " + port);

                while (running.get()) {
                    log("Chờ kết nối từ Client...");
                    clientSocket = serverSocket.accept();
                    connected.set(true);

                    String clientAddr = clientSocket.getInetAddress().getHostAddress();
                    log("Client đã kết nối: " + clientAddr);

                    if (connectionCallback != null) {
                        connectionCallback.accept(true);
                    }

                    handleClient(clientSocket);

                    connected.set(false);
                    if (connectionCallback != null) {
                        connectionCallback.accept(false);
                    }
                    log("Client " + clientAddr + " đã ngắt kết nối");
                }
            } catch (IOException e) {
                if (running.get()) {
                    log("Lỗi server: " + e.getMessage());
                }
            }
        }, "ServerListenThread").start();
    }

    /**
     * Xử lý một phiên kết nối Client.
     */
    private void handleClient(Socket socket) {
        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            // Gửi thông tin màn hình
            Dimension screenSize = ScreenCaptureUtils.getScreenSize();
            NetworkUtils.sendCommand(dos, ProtocolConstants.CMD_SCREEN_INFO);
            NetworkUtils.sendCommand(dos, screenSize.width + "x" + screenSize.height);

            // Thread gửi ảnh liên tục
            Thread screenThread = new Thread(() -> {
                try {
                    while (connected.get() && !socket.isClosed()) {
                        byte[] screenData = ScreenCaptureUtils.captureScreenAsBytes(imageQuality);
                        synchronized (dos) {
                            NetworkUtils.sendCommand(dos, ProtocolConstants.CMD_SCREEN_CAPTURE);
                            NetworkUtils.sendBytes(dos, screenData);
                        }
                        Thread.sleep(captureInterval);
                    }
                } catch (Exception e) {
                    if (connected.get()) {
                        log("Lỗi gửi ảnh: " + e.getMessage());
                    }
                }
            }, "ScreenCaptureThread");
            screenThread.setDaemon(true);
            screenThread.start();

            // Nhận lệnh điều khiển từ Client
            while (connected.get() && !socket.isClosed()) {
                String command = NetworkUtils.receiveCommand(dis);
                processCommand(command);
            }
        } catch (IOException e) {
            if (connected.get()) {
                log("Mất kết nối: " + e.getMessage());
            }
        } finally {
            connected.set(false);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Xử lý lệnh điều khiển từ Client.
     */
    private void processCommand(String command) {
        try {
            String[] parts = command.split("\\|");
            String cmd = parts[0];

            switch (cmd) {
                case ProtocolConstants.CMD_MOUSE_MOVE -> {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    robot.mouseMove(x, y);
                }
                case ProtocolConstants.CMD_MOUSE_CLICK -> {
                    int button = Integer.parseInt(parts[1]);
                    int clickCount = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                    int mask = getMouseButtonMask(button);
                    for (int i = 0; i < clickCount; i++) {
                        robot.mousePress(mask);
                        robot.mouseRelease(mask);
                        robot.delay(50);
                    }
                }
                case ProtocolConstants.CMD_MOUSE_PRESS -> {
                    int button = Integer.parseInt(parts[1]);
                    robot.mousePress(getMouseButtonMask(button));
                }
                case ProtocolConstants.CMD_MOUSE_RELEASE -> {
                    int button = Integer.parseInt(parts[1]);
                    robot.mouseRelease(getMouseButtonMask(button));
                }
                case ProtocolConstants.CMD_MOUSE_SCROLL -> {
                    int amount = Integer.parseInt(parts[1]);
                    robot.mouseWheel(amount);
                }
                case ProtocolConstants.CMD_KEY_PRESS -> {
                    int keyCode = Integer.parseInt(parts[1]);
                    robot.keyPress(keyCode);
                }
                case ProtocolConstants.CMD_KEY_RELEASE -> {
                    int keyCode = Integer.parseInt(parts[1]);
                    robot.keyRelease(keyCode);
                }
                case ProtocolConstants.CMD_CLIPBOARD_SEND -> {
                    String text = parts[1];
                    StringSelection selection = new StringSelection(text);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                    log("Clipboard đã nhận: " + text.substring(0, Math.min(50, text.length())));
                }
                case ProtocolConstants.CMD_DISCONNECT -> {
                    log("Client yêu cầu ngắt kết nối");
                    connected.set(false);
                }
                default -> log("Lệnh không xác định: " + cmd);
            }
        } catch (Exception e) {
            log("Lỗi xử lý lệnh: " + e.getMessage());
        }
    }

    private int getMouseButtonMask(int button) {
        return switch (button) {
            case 1 -> InputEvent.BUTTON1_DOWN_MASK;
            case 2 -> InputEvent.BUTTON2_DOWN_MASK;
            case 3 -> InputEvent.BUTTON3_DOWN_MASK;
            default -> InputEvent.BUTTON1_DOWN_MASK;
        };
    }

    /**
     * Dừng server.
     */
    public void stop() {
        running.set(false);
        connected.set(false);
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("Lỗi khi dừng server: " + e.getMessage());
        }
        log("Server đã dừng");
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isConnected() {
        return connected.get();
    }
}
