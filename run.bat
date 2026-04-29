@echo off
REM Run script for EditorNiko on Windows

echo Building EditorNiko...
call mvn clean package

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful! Running application...
    echo.
    java --enable-native-access=ALL-UNNAMED -jar target\EditorNiko-executable.jar
) else (
    echo Build failed!
    exit /b 1
)
