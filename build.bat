@echo off
setlocal
cd /d "%~dp0"
echo.
echo === Building BBS Afterimage. The first run takes 10-20 minutes. Do not close this window. ===
echo.
call gradlew.bat build
if errorlevel 1 goto failed
if not exist READY_MOD mkdir READY_MOD
copy /y "build\libs\bbs-am-1.0.0.jar" "READY_MOD\" >nul
if errorlevel 1 goto failed
echo.
echo === DONE. The ready mod is in the READY_MOD folder. Put that file into your mods folder. ===
explorer "%~dp0READY_MOD"
pause
exit /b 0
:failed
echo.
echo === FAILED. Take a photo of the red text above and send it to Claude. ===
pause
exit /b 1
