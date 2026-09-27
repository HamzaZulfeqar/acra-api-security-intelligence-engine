@echo off
setlocal
where java >nul 2>nul
if errorlevel 1 (
  echo ACRA requires Java 21 or newer.
  exit /b 1
)
java -jar "%~dp0acra-standalone.jar" %*
