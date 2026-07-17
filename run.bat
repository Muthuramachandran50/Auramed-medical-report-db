@echo off
title AuraMed Diagnostics Server
echo ===================================================
echo   AuraMed Diagnostics DBMS Web Server Launcher
echo ===================================================
echo.

:: Check for bin folder
if not exist bin (
    echo Creating bin directory...
    mkdir bin
)

:: Compile classes
echo Compiling Java source files...
javac -encoding UTF-8 -cp ".;D:\mysql-connector-j-9.3.0\mysql-connector-j-9.3.0\mysql-connector-j-9.3.0.jar" -d bin src/model/*.java src/dao/*.java src/WebServer.java

if %errorlevel% neq 0 (
    echo.
    echo ❌ Compilation FAILED!
    echo Please check Java Compiler output above.
    pause
    exit /b %errorlevel%
)

echo.
echo ✅ Compilation successful!
echo.
echo Starting Web Server...
echo Access the web UI at: http://localhost:8081/
echo.
echo Press Ctrl+C in this terminal window to stop the server.
echo ===================================================
echo.

:: Run server
java -cp "bin;D:\mysql-connector-j-9.3.0\mysql-connector-j-9.3.0\mysql-connector-j-9.3.0.jar" WebServer
