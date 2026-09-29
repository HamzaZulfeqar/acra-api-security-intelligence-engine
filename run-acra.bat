@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
  echo ACRA requires Java 21 or newer.
  exit /b 1
)

set "NO_BUILD=0"
if /I "%~1"=="--no-build" (
  set "NO_BUILD=1"
  shift
)

if "%NO_BUILD%"=="0" (
  where mvn >nul 2>nul
  if errorlevel 1 (
    echo Maven is required for source-clone startup. Use --no-build only after packaging.
    exit /b 1
  )
  call mvn --batch-mode --no-transfer-progress -pl standalone -am package
  if errorlevel 1 exit /b %errorlevel%
) else (
  if not exist "standalone\target\acra-standalone.jar" (
    echo Prebuilt standalone\target\acra-standalone.jar was not found.
    exit /b 1
  )
)

java -jar standalone\target\acra-standalone.jar %*
