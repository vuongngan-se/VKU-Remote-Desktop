package com.remotedesktop.common;

import java.io.*;

/**
 * Tiện ích truyền/nhận dữ liệu qua TCP Stream.
 * Sử dụng giao thức: [4 bytes chiều dài] + [payload]
 */
public class NetworkUtils {

    /**
     * Gửi một chuỗi lệnh qua OutputStream.
     */
    public static void sendCommand(DataOutputStream dos, String command) throws IOException {
        byte[] data = command.getBytes("UTF-8");
        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();
    }

    /**
     * Nhận một chuỗi lệnh từ InputStream.
     */
    public static String receiveCommand(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (length <= 0 || length > 10_000_000) {
            throw new IOException("Invalid command length: " + length);
        }
        byte[] data = new byte[length];
        dis.readFully(data);
        return new String(data, "UTF-8");
    }

    /**
     * Gửi mảng byte (ảnh chụp màn hình) qua OutputStream.
     */
    public static void sendBytes(DataOutputStream dos, byte[] data) throws IOException {
        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();
    }

    /**
     * Nhận mảng byte từ InputStream.
     */
    public static byte[] receiveBytes(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (length <= 0 || length > 50_000_000) {
            throw new IOException("Invalid data length: " + length);
        }
        byte[] data = new byte[length];
        dis.readFully(data);
        return data;
    }
}
