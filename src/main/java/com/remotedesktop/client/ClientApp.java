package com.remotedesktop.client;

import com.remotedesktop.common.ProtocolConstants;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Giao diện JavaFX cho Client (máy điều khiển).
 * Hiển thị màn hình Server và gửi sự kiện chuột/bàn phím.
 */
public class ClientApp extends Application {

    private RemoteDesktopClient client;
    private Canvas remoteCanvas;
    private GraphicsContext gc;
    private TextArea logArea;
    private Label statusLabel;
    private Circle statusDot;
    private TextField hostField;
    private Spinner<Integer> portSpinner;
    private Button connectButton;
    private Button disconnectButton;
    private ToggleButton controlToggle;

    private Dimension serverScreenSize;
    private boolean controlEnabled = true;
    private double scaleX = 1.0;
    private double scaleY = 1.0;

    // Kích thước canvas hiển thị
    private static final double CANVAS_WIDTH = 960;
    private static final double CANVAS_HEIGHT = 540;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VKU Remote Desktop - Client");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // --- Top: Connection bar ---
        VBox topBox = createTopBar();
        root.setTop(topBox);

        // --- Center: Remote screen + Log ---
        SplitPane center = createCenter();
        root.setCenter(center);

        // --- Bottom: Status bar ---
        HBox bottomBar = createBottomBar();
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 1050, 720);
        scene.getStylesheets().add(getClass().getResource("/com/remotedesktop/css/client-style.css").toExternalForm());

        // Bắt sự kiện bàn phím trên scene
        scene.setOnKeyPressed(this::handleKeyPressed);
        scene.setOnKeyReleased(this::handleKeyReleased);

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (client != null) client.disconnect();
            Platform.exit();
            System.exit(0);
        });

        // Animation mở
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        primaryStage.show();
        initClient();
    }

    private VBox createTopBar() {
        VBox topBox = new VBox(0);

        // Header
        HBox header = new HBox(10);
        header.getStyleClass().add("header-box");
        header.setPadding(new Insets(18, 25, 15, 25));
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("VKU Remote Desktop");
        title.getStyleClass().add("title-label");

        Label subtitle = new Label("Client - Điều khiển từ xa");
        subtitle.getStyleClass().add("subtitle-label");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        statusDot = new Circle(6);
        statusDot.getStyleClass().add("status-dot-offline");

        statusLabel = new Label("Chưa kết nối");
        statusLabel.getStyleClass().add("status-label");

        header.getChildren().addAll(title, subtitle, spacer1, statusDot, statusLabel);

        // Connection bar
        HBox connBar = new HBox(12);
        connBar.getStyleClass().add("connection-bar");
        connBar.setPadding(new Insets(12, 25, 12, 25));
        connBar.setAlignment(Pos.CENTER_LEFT);

        Label hostLbl = new Label("Địa chỉ IP:");
        hostLbl.getStyleClass().add("control-label");

        hostField = new TextField("127.0.0.1");
        hostField.setPrefWidth(180);
        hostField.getStyleClass().add("control-field");
        hostField.setPromptText("Nhập địa chỉ IP server");

        Label portLbl = new Label("Cổng:");
        portLbl.getStyleClass().add("control-label");

        portSpinner = new Spinner<>(1024, 65535, ProtocolConstants.DEFAULT_PORT);
        portSpinner.setEditable(true);
        portSpinner.setPrefWidth(100);
        portSpinner.getStyleClass().add("control-spinner");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        controlToggle = new ToggleButton("Điều khiển: BẬT");
        controlToggle.setSelected(true);
        controlToggle.getStyleClass().add("btn-toggle");
        controlToggle.setOnAction(e -> {
            controlEnabled = controlToggle.isSelected();
            controlToggle.setText(controlEnabled ? "Điều khiển: BẬT" : "Điều khiển: TẮT");
        });

        connectButton = new Button("Kết nối");
        connectButton.getStyleClass().add("btn-connect");
        connectButton.setOnAction(e -> connectToServer());

        disconnectButton = new Button("Ngắt");
        disconnectButton.getStyleClass().add("btn-disconnect");
        disconnectButton.setDisable(true);
        disconnectButton.setOnAction(e -> disconnectFromServer());

        connBar.getChildren().addAll(hostLbl, hostField, portLbl, portSpinner,
                spacer2, controlToggle, connectButton, disconnectButton);

        topBox.getChildren().addAll(header, connBar);
        return topBox;
    }

    private SplitPane createCenter() {
        // Remote screen panel
        StackPane canvasContainer = new StackPane();
        canvasContainer.getStyleClass().add("canvas-container");
        canvasContainer.setPadding(new Insets(5));

        remoteCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = remoteCanvas.getGraphicsContext2D();
        gc.setFill(Color.rgb(30, 30, 40));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Vẽ text chào mừng trên canvas
        gc.setFill(Color.rgb(120, 120, 140));
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", 24));
        gc.fillText("Chưa kết nối tới Server", CANVAS_WIDTH / 2 - 140, CANVAS_HEIGHT / 2);
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", 14));
        gc.fillText("Nhập địa chỉ IP và nhấn Kết nối để bắt đầu", CANVAS_WIDTH / 2 - 160, CANVAS_HEIGHT / 2 + 35);

        // Mouse events trên canvas
        remoteCanvas.setOnMouseMoved(this::handleMouseMove);
        remoteCanvas.setOnMouseDragged(this::handleMouseMove);
        remoteCanvas.setOnMousePressed(this::handleMousePressed);
        remoteCanvas.setOnMouseReleased(this::handleMouseReleased);
        remoteCanvas.setOnMouseClicked(this::handleMouseClicked);
        remoteCanvas.setOnScroll(this::handleScroll);

        canvasContainer.getChildren().add(remoteCanvas);

        // Log panel
        VBox logPanel = new VBox(8);
        logPanel.setPadding(new Insets(10));
        logPanel.getStyleClass().add("log-panel");

        Label logTitle = new Label("Nhật ký");
        logTitle.getStyleClass().add("section-label");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("log-area");
        logArea.setPrefHeight(150);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        logPanel.getChildren().addAll(logTitle, logArea);

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.getItems().addAll(canvasContainer, logPanel);
        splitPane.setDividerPositions(0.75);

        return splitPane;
    }

    private HBox createBottomBar() {
        HBox bar = new HBox(15);
        bar.getStyleClass().add("bottom-bar");
        bar.setPadding(new Insets(8, 25, 8, 25));
        bar.setAlignment(Pos.CENTER_LEFT);

        Label info = new Label("VKU Remote Desktop v1.0 - Đề 12 Lập trình mạng");
        info.getStyleClass().add("footer-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label fpsLabel = new Label("JavaFX 17 + TCP/IP");
        fpsLabel.getStyleClass().add("footer-label");

        bar.getChildren().addAll(info, spacer, fpsLabel);
        return bar;
    }

    private void initClient() {
        client = new RemoteDesktopClient();
        client.setLogCallback(msg -> Platform.runLater(() -> appendLog(msg)));
        client.setConnectionCallback(connected -> Platform.runLater(() -> {
            if (connected) {
                setConnectedState(true);
            } else {
                setConnectedState(false);
            }
        }));
        client.setScreenInfoCallback(dim -> Platform.runLater(() -> {
            serverScreenSize = dim;
            scaleX = (double) dim.width / CANVAS_WIDTH;
            scaleY = (double) dim.height / CANVAS_HEIGHT;
            appendLog("Tỷ lệ hiển thị: " + String.format("%.2f x %.2f", scaleX, scaleY));
        }));
        client.setScreenCallback(data -> Platform.runLater(() -> renderScreen(data)));
    }

    private void connectToServer() {
        String host = hostField.getText().trim();
        int port = portSpinner.getValue();

        if (host.isEmpty()) {
            showAlert("Vui lòng nhập địa chỉ IP!");
            return;
        }

        connectButton.setDisable(true);
        appendLog("Đang kết nối tới " + host + ":" + port + "...");

        // Animation kết nối
        RotateTransition rotate = new RotateTransition(Duration.millis(1000), statusDot);
        rotate.setByAngle(360);
        rotate.setCycleCount(3);
        rotate.play();

        client.connect(host, port);
    }

    private void disconnectFromServer() {
        if (client != null) {
            client.disconnect();
        }
    }

    private void setConnectedState(boolean connected) {
        connectButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        hostField.setDisable(connected);
        portSpinner.setDisable(connected);

        statusDot.getStyleClass().clear();
        if (connected) {
            statusDot.getStyleClass().add("status-dot-online");
            statusLabel.setText("Đã kết nối");
            remoteCanvas.setCursor(Cursor.CROSSHAIR);

            // Pulse animation
            ScaleTransition pulse = new ScaleTransition(Duration.millis(400), statusDot);
            pulse.setFromX(1);
            pulse.setFromY(1);
            pulse.setToX(1.8);
            pulse.setToY(1.8);
            pulse.setCycleCount(4);
            pulse.setAutoReverse(true);
            pulse.play();
        } else {
            statusDot.getStyleClass().add("status-dot-offline");
            statusLabel.setText("Chưa kết nối");
            remoteCanvas.setCursor(Cursor.DEFAULT);

            // Vẽ lại canvas rỗng
            gc.setFill(Color.rgb(30, 30, 40));
            gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
            gc.setFill(Color.rgb(120, 120, 140));
            gc.setFont(javafx.scene.text.Font.font("Segoe UI", 24));
            gc.fillText("Đã ngắt kết nối", CANVAS_WIDTH / 2 - 100, CANVAS_HEIGHT / 2);
        }
    }

    /**
     * Vẽ ảnh màn hình Server lên Canvas.
     */
    private void renderScreen(byte[] imageData) {
        try {
            BufferedImage bImg = ImageIO.read(new ByteArrayInputStream(imageData));
            if (bImg != null) {
                WritableImage fxImage = SwingFXUtils.toFXImage(bImg, null);
                gc.drawImage(fxImage, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
            }
        } catch (Exception e) {
            appendLog("Lỗi render: " + e.getMessage());
        }
    }

    // --- Xử lý sự kiện chuột ---

    private void handleMouseMove(MouseEvent event) {
        if (!controlEnabled || client == null || !client.isConnected()) return;
        int x = (int) (event.getX() * scaleX);
        int y = (int) (event.getY() * scaleY);
        client.sendMouseMove(x, y);
    }

    private void handleMousePressed(MouseEvent event) {
        if (!controlEnabled || client == null || !client.isConnected()) return;
        remoteCanvas.requestFocus();
        int button = mapMouseButton(event.getButton());
        client.sendMousePress(button);
    }

    private void handleMouseReleased(MouseEvent event) {
        if (!controlEnabled || client == null || !client.isConnected()) return;
        int button = mapMouseButton(event.getButton());
        client.sendMouseRelease(button);
    }

    private void handleMouseClicked(MouseEvent event) {
        if (!controlEnabled || client == null || !client.isConnected()) return;
        if (event.getClickCount() == 2) {
            int button = mapMouseButton(event.getButton());
            client.sendMouseClick(button, 2);
        }
    }

    private void handleScroll(ScrollEvent event) {
        if (!controlEnabled || client == null || !client.isConnected()) return;
        int amount = (int) (-event.getDeltaY() / 40);
        if (amount != 0) {
            client.sendMouseScroll(amount);
        }
    }

    private int mapMouseButton(MouseButton button) {
        return switch (button) {
            case PRIMARY -> 1;
            case MIDDLE -> 2;
            case SECONDARY -> 3;
            default -> 1;
        };
    }

    // --- Xử lý sự kiện bàn phím ---

    private void handleKeyPressed(KeyEvent event) {
        if (!controlEnabled || client == null || !client.isConnected()) return;
        int keyCode = mapKeyCode(event);
        if (keyCode != -1) {
            client.sendKeyPress(keyCode);
            event.consume();
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        if (!controlEnabled || client == null || !client.isConnected()) return;
        int keyCode = mapKeyCode(event);
        if (keyCode != -1) {
            client.sendKeyRelease(keyCode);
            event.consume();
        }
    }

    /**
     * Map JavaFX KeyCode sang java.awt.event.KeyEvent code.
     */
    private int mapKeyCode(KeyEvent event) {
        try {
            String name = event.getCode().name();
            // Ánh xạ trực tiếp qua tên
            java.lang.reflect.Field field = java.awt.event.KeyEvent.class.getField("VK_" + name);
            return field.getInt(null);
        } catch (Exception e) {
            // Fallback: dùng code thông thường
            return switch (event.getCode()) {
                case ENTER -> java.awt.event.KeyEvent.VK_ENTER;
                case BACK_SPACE -> java.awt.event.KeyEvent.VK_BACK_SPACE;
                case TAB -> java.awt.event.KeyEvent.VK_TAB;
                case SHIFT -> java.awt.event.KeyEvent.VK_SHIFT;
                case CONTROL -> java.awt.event.KeyEvent.VK_CONTROL;
                case ALT -> java.awt.event.KeyEvent.VK_ALT;
                case ESCAPE -> java.awt.event.KeyEvent.VK_ESCAPE;
                case SPACE -> java.awt.event.KeyEvent.VK_SPACE;
                case DELETE -> java.awt.event.KeyEvent.VK_DELETE;
                case UP -> java.awt.event.KeyEvent.VK_UP;
                case DOWN -> java.awt.event.KeyEvent.VK_DOWN;
                case LEFT -> java.awt.event.KeyEvent.VK_LEFT;
                case RIGHT -> java.awt.event.KeyEvent.VK_RIGHT;
                default -> -1;
            };
        }
    }

    private void appendLog(String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText("[" + time + "] " + message + "\n");
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Cảnh báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
