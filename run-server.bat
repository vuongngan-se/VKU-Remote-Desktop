@echo off
echo ========================================
echo   VKU Remote Desktop - Start SERVER
echo   (May bi dieu khien)
echo ========================================
echo.
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo [ERROR] Build that bai!
    pause
    exit /b 1
)
echo Dang khoi dong Server...
call mvn javafx:run -Pserver
pause
