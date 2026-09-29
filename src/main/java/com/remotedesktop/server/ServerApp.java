package com.remotedesktop.server;

import com.remotedesktop.common.ProtocolConstants;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Giao diện JavaFX cho Server (máy bị điều khiển).
 */
public class ServerApp extends Application {

    private RemoteDesktopServer server;
    private TextArea logArea;
    private Label statusLabel;
    private Circle statusDot;
    private Label ipLabel;
    private Label portLabel;
    private Button startButton;
    private Button stopButton;
    private Spinner<Integer> portSpinner;
    private ComboBox<String> qualityCombo;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VKU Remote Desktop - Server");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // --- Header ---
        VBox header = createHeader();
        root.setTop(header);

        // --- Center: Log ---
        VBox center = createCenter();
        root.setCenter(center);

        // --- Bottom: Controls ---
        HBox bottom = createBottom();
        root.setBottom(bottom);

        Scene scene = new Scene(root, 650, 580);
        scene.getStylesheets().add(getClass().getResource("/com/remotedesktop/css/server-style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e -> {
            if (server != null) server.stop();
            Platform.exit();
            System.exit(0);
        });

        // Animation xuất hiện
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        primaryStage.show();
        loadNetworkInfo();
    }

    private VBox createHeader() {
        VBox header = new VBox(8);
        header.getStyleClass().add("header-box");
        header.setPadding(new Insets(25, 30, 20, 30));
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("VKU Remote Desktop");
        title.getStyleClass().add("title-label");

        Label subtitle = new Label("Server - Máy bị điều khiển");
        subtitle.getStyleClass().add("subtitle-label");

        // Status bar
        HBox statusBar = new HBox(12);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(10, 0, 0, 0));

        statusDot = new Circle(6);
        statusDot.getStyleClass().add("status-dot-offline");

        statusLabel = new Label("Chưa khởi động");
        statusLabel.getStyleClass().add("status-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ipLabel = new Label("IP: ---");
        ipLabel.getStyleClass().add("info-label");

        portLabel = new Label("Port: ---");
        portLabel.getStyleClass().add("info-label");

        statusBar.getChildren().addAll(statusDot, statusLabel, spacer, ipLabel, portLabel);

        header.getChildren().addAll(title, subtitle, statusBar);
        return header;
    }

    private VBox createCenter() {
        VBox center = new VBox(10);
        center.setPadding(new Insets(15, 30, 15, 30));

        Label logTitle = new Label("Nhật ký hoạt động");
        logTitle.getStyleClass().add("section-label");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("log-area");
        logArea.setPrefHeight(280);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        center.getChildren().addAll(logTitle, logArea);
        return center;
    }

    private HBox createBottom() {
        HBox bottom = new HBox(15);
        bottom.getStyleClass().add("bottom-box");
        bottom.setPadding(new Insets(18, 30, 22, 30));
        bottom.setAlignment(Pos.CENTER);

        // Port
        Label portLbl = new Label("Cổng:");
        portLbl.getStyleClass().add("control-label");
        portSpinner = new Spinner<>(1024, 65535, ProtocolConstants.DEFAULT_PORT);
        portSpinner.setEditable(true);
        portSpinner.setPrefWidth(100);
        portSpinner.getStyleClass().add("control-spinner");

        // Quality
        Label qualityLbl = new Label("Chất lượng:");
        qualityLbl.getStyleClass().add("control-label");
        qualityCombo = new ComboBox<>();
        qualityCombo.getItems().addAll("Cao", "Trung bình", "Thấp");
        qualityCombo.setValue("Trung bình");
        qualityCombo.getStyleClass().add("control-combo");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Buttons
        startButton = new Button("Khởi động");
        startButton.getStyleClass().add("btn-start");
        startButton.setOnAction(e -> startServer());

        stopButton = new Button("Dừng");
        stopButton.getStyleClass().add("btn-stop");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopServer());

        bottom.getChildren().addAll(portLbl, portSpinner, qualityLbl, qualityCombo, spacer, startButton, stopButton);
        return bottom;
    }

    private void loadNetworkInfo() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            ipLabel.setText("IP: " + ip);
        } catch (Exception e) {
            ipLabel.setText("IP: Không xác định");
        }
    }

    private void startServer() {
        int port = portSpinner.getValue();
        float quality = switch (qualityCombo.getValue()) {
            case "Cao" -> ProtocolConstants.IMAGE_QUALITY_HIGH;
            case "Thấp" -> ProtocolConstants.IMAGE_QUALITY_LOW;
            default -> ProtocolConstants.IMAGE_QUALITY_MEDIUM;
        };

        server = new RemoteDesktopServer(port);
        server.setImageQuality(quality);
        server.setLogCallback(msg -> Platform.runLater(() -> appendLog(msg)));
        server.setConnectionCallback(connected -> Platform.runLater(() -> {
            if (connected) {
                setStatus("Client đã kết nối", true);
                pulseAnimation(statusDot);
            } else {
                setStatus("Đang lắng nghe...", false);
            }
        }));

        server.start();

        portLabel.setText("Port: " + port);
        setStatus("Đang lắng nghe...", false);
        statusDot.getStyleClass().clear();
        statusDot.getStyleClass().add("status-dot-waiting");

        startButton.setDisable(true);
        stopButton.setDisable(false);
        portSpinner.setDisable(true);
        qualityCombo.setDisable(true);

        appendLog("Server đã khởi động trên cổng " + port);

        // Animation cho nút
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), stopButton);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1);
        scale.setToY(1);
        scale.play();
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
        }
        setStatus("Đã dừng", false);
        statusDot.getStyleClass().clear();
        statusDot.getStyleClass().add("status-dot-offline");

        startButton.setDisable(false);
        stopButton.setDisable(true);
        portSpinner.setDisable(false);
        qualityCombo.setDisable(false);

        appendLog("Server đã dừng");
    }

    private void setStatus(String text, boolean isConnected) {
        statusLabel.setText(text);
        statusDot.getStyleClass().clear();
        if (isConnected) {
            statusDot.getStyleClass().add("status-dot-online");
        } else {
            statusDot.getStyleClass().add("status-dot-waiting");
        }
    }

    private void appendLog(String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText("[" + time + "] " + message + "\n");
    }

    private void pulseAnimation(Circle dot) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(600), dot);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.5);
        pulse.setToY(1.5);
        pulse.setCycleCount(6);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
