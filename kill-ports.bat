@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   OpenIoT Backend Ports Cleanup Script
echo ========================================
echo.

REM Define backend service ports
set PORTS=8080 8081 8082 8083 8084 8087

set KILLED=0

for %%p in (%PORTS%) do (
    echo Checking port %%p...

    REM Find process using this port
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :%%p ^| findstr LISTENING') do (
        set PID=%%a
        if not "!PID!"=="" (
            echo   Found process !PID! on port %%p, killing...
            taskkill /F /PID !PID! >nul 2>&1
            if !errorlevel! equ 0 (
                echo   [OK] Process !PID! killed successfully
                set /a KILLED+=1
            ) else (
                echo   [FAIL] Failed to kill process !PID!
            )
        )
    )
)

echo.
echo ========================================
if !KILLED! equ 0 (
    echo   No processes were killed. All ports are free.
) else (
    echo   Total processes killed: !KILLED!
)
echo ========================================
echo.

pause
