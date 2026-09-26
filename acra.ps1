$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
mvn --batch-mode --no-transfer-progress -pl app/standalone -am package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java -jar "app/standalone/target/acra-standalone-0.3.0.jar" @args
exit $LASTEXITCODE
