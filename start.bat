@echo off
echo ================================================
echo    Fruit Detection System - Quick Start
echo ================================================
echo.

echo [1] Starting Python Detection Service...
start "Python Service" cmd /c "cd src\main\resources\python && py fruit_detection_service.py"
timeout /t 8 /nobreak >nul

echo [2] Starting Java Backend...
start "Java Backend" cmd /c "mvn spring-boot:run -q"
timeout /t 15 /nobreak >nul

echo [3] Starting Vue Frontend...
start "Vue Frontend" cmd /c "npm run serve"

echo.
echo ================================================
echo Services are starting...
echo.
echo   Python Service: http://127.0.0.1:5005
echo   Java Backend:   http://localhost:8080
echo   Vue Frontend:   http://localhost:8081
echo ================================================
echo.
echo Wait ~30 seconds for all services to fully start.
pause