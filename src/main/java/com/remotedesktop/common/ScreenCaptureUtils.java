package com.remotedesktop.common;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Tiện ích chụp và nén ảnh màn hình.
 */
public class ScreenCaptureUtils {

    private static Robot robot;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    /**
     * Chụp toàn bộ màn hình thành BufferedImage.
     */
    public static BufferedImage captureScreen() {
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        return robot.createScreenCapture(screenRect);
    }

    /**
     * Chụp màn hình và nén thành mảng byte JPEG.
     */
    public static byte[] captureScreenAsBytes(float quality) throws IOException {
        BufferedImage image = captureScreen();
        return compressImage(image, quality);
    }

    /**
     * Nén BufferedImage thành byte[] JPEG với chất lượng tùy chỉnh.
     */
    public static byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        }
        writer.dispose();
        return baos.toByteArray();
    }

    /**
     * Giải nén byte[] thành BufferedImage.
     */
    public static BufferedImage decompressImage(byte[] data) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(data));
    }

    /**
     * Lấy kích thước màn hình.
     */
    public static Dimension getScreenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }
}
