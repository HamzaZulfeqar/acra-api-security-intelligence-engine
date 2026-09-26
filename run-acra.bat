@echo off
setlocal
cd /d "%~dp0"
call mvn --batch-mode --no-transfer-progress -pl standalone -am package
if errorlevel 1 exit /b %errorlevel%
java -jar standalone\target\acra-standalone.jar %*
