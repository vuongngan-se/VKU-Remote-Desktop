@echo off
echo ========================================
echo   VKU Remote Desktop - Build & Run
echo   De so 12 - Lap trinh mang
echo ========================================
echo.

echo [1] Build project...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo [ERROR] Build that bai!
    pause
    exit /b 1
)
echo [OK] Build thanh cong!
echo.

echo Chon che do chay:
echo   1. Server (may bi dieu khien)
echo   2. Client (may dieu khien)
echo.
set /p choice="Nhap lua chon (1 hoac 2): "

if "%choice%"=="1" (
    echo.
    echo Dang khoi dong Server...
    call mvn javafx:run -Djavafx.mainClass=com.remotedesktop.server.ServerApp -q
) else if "%choice%"=="2" (
    echo.
    echo Dang khoi dong Client...
    call mvn javafx:run -q
) else (
    echo Lua chon khong hop le!
)

pause
