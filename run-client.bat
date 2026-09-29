@echo off
echo ========================================
echo   VKU Remote Desktop - Start CLIENT
echo   (May dieu khien)
echo ========================================
echo.
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo [ERROR] Build that bai!
    pause
    exit /b 1
)
echo Dang khoi dong Client...
call mvn javafx:run
pause
