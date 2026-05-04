@echo off
setlocal

echo [1/3] Building fat jar...
call mvn -q package -DskipTests
if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)

echo [2/3] Packaging server...
if exist dist\LTM2-Server rmdir /s /q dist\LTM2-Server
jpackage ^
  --type app-image ^
  --input dist ^
  --main-jar ltm2-chat-1.0.0-all.jar ^
  --main-class server.ServerLauncher ^
  --name LTM2-Server ^
  --app-version 1.0.0 ^
  --dest dist
if errorlevel 1 (
    echo SERVER PACKAGE FAILED
    exit /b 1
)

echo [3/3] Packaging client...
if exist dist\LTM2-Client rmdir /s /q dist\LTM2-Client
jpackage ^
  --type app-image ^
  --input dist ^
  --main-jar ltm2-chat-1.0.0-all.jar ^
  --main-class client.ClientLauncher ^
  --name LTM2-Client ^
  --app-version 1.0.0 ^
  --dest dist
if errorlevel 1 (
    echo CLIENT PACKAGE FAILED
    exit /b 1
)

echo.
echo Done! Output:
echo   dist\LTM2-Server\LTM2-Server.exe
echo   dist\LTM2-Client\LTM2-Client.exe
endlocal
