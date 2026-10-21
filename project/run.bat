@echo off
cd /d "%~dp0"
echo ==================================================
echo   Campus Lost ^& Found System - Starting...
echo ==================================================
echo.
echo ^>^> Compiling Java files...
javac -encoding UTF-8 Item.java Main.java Server.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Please ensure JDK 11+ is installed.
    pause
    exit /b %ERRORLEVEL%
)

echo ^>^> Compilation successful!
echo.
echo ^>^> Starting HTTP Server on port 8080...
echo ^>^> Open your browser at: http://localhost:8080
echo ^>^> Press Ctrl + C to stop the server anytime.
echo ==================================================
echo.
java Server
pause
