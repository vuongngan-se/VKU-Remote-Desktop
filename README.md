# VKU Remote Desktop 

## Chuong trinh dieu khien man hinh tu xa

Ung dung Remote Desktop xay dung bang Java Core + JavaFX 17 + TCP/IP, tuong tu TeamViewer/Remote Desktop.

---

## Kien truc he thong

```
+------------------+     TCP/IP      +------------------+
|   SERVER App     | <=============> |   CLIENT App     |
| (May bi dieu     |   Port 5900     | (May dieu khien) |
|  khien)          |                 |                  |
|                  |   Screen JPEG   |                  |
| - Chup man hinh -|---------------->|- Hien thi canvas |
| - Nhan lenh     <|-----------------|  - Gui su kien   |
|   dieu khien     |  Mouse/Keyboard |    chuot/phim    |
+------------------+                 +------------------+
```

## Cong nghe su dung

- Java 17 (Java Core)
- JavaFX 17 (Giao dien do hoa)
- Maven (Quan ly du an)
- TCP/IP Socket (Giao tiep mang)
- java.awt.Robot (Dieu khien chuot/phim)
- JPEG Compression (Nen anh man hinh)
- CSS3 (Giao dien hien dai dark theme)
- FXML (Thiet ke giao dien)

## Cau truc du an

```
src/main/java/com/remotedesktop/
  common/
    ProtocolConstants.java    - Hang so giao thuc TCP
    NetworkUtils.java         - Truyen/nhan du lieu qua socket
    ScreenCaptureUtils.java   - Chup va nen anh man hinh
  server/
    RemoteDesktopServer.java  - TCP Server xu ly ket noi
    ServerApp.java            - Giao dien JavaFX Server
    ServerLauncher.java       - Launcher Server
  client/
    RemoteDesktopClient.java  - TCP Client ket noi server
    ClientApp.java            - Giao dien JavaFX Client
    ClientLauncher.java       - Launcher Client

src/main/resources/com/remotedesktop/
  css/
    server-style.css          - CSS dark theme cho Server
    client-style.css          - CSS dark theme cho Client
  fxml/
    server-view.fxml          - FXML layout Server
    client-view.fxml          - FXML layout Client
```

## Tinh nang chinh

1. Chup va truyen man hinh theo thoi gian thuc qua TCP/IP
2. Dieu khien chuot tu xa (di chuyen, click, double-click, keo tha, cuon)
3. Dieu khien ban phim tu xa (nhan/tha phim)
4. Cau hinh chat luong anh (Cao/Trung binh/Thap)
5. Bat/tat che do dieu khien
6. Truyen clipboard text
7. Giao dien dark theme hien dai voi animation
8. Hien thi nhat ky hoat dong

## Huong dan chay

### Yeu cau

- JDK 17 tro len
- Maven 3.8+

### Build

```bash
mvn clean compile
```

### Chay Server (tren may bi dieu khien)

```bash
mvn javafx:run -Djavafx.mainClass=com.remotedesktop.server.ServerApp
```

Hoac chay file `run-server.bat`

### Chay Client (tren may dieu khien)

```bash
mvn javafx:run
```

Hoac chay file `run-client.bat`

### Su dung

1. Khoi dong Server tren may can bi dieu khien, nhan "Khoi dong"
2. Ghi nhan dia chi IP va cong hien thi tren Server
3. Khoi dong Client tren may dieu khien
4. Nhap dia chi IP va cong cua Server, nhan "Ket noi"
5. Man hinh Server se hien thi tren Client
6. Di chuot va go phim tren vung hien thi de dieu khien may Server

## Giao thuc truyen thong

Giao thuc tu dinh nghia tren TCP:

- Moi message: `[4 bytes do dai] + [payload bytes]`
- Lenh dieu khien dang text: `COMMAND|param1|param2|...`
- Anh man hinh: JPEG nen gui duoi dang byte array
- Server gui anh lien tuc, Client gui lenh khi co su kien

## Tac gia

Sinh vien Truong Dai hoc Viet Han (VKU)
Mon: Lap trinh mang - De so 12
