param(
    [string]$Compiler = 'javac',
    [string]$Java = 'java'
)

$ErrorActionPreference = 'Stop'
$repoPath = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$startedAt = (Get-Date).ToUniversalTime().ToString('o')
$runName = 's5-defensive-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff')
$buildPath = Join-Path $repoPath ('build/' + $runName)
$mainOutput = Join-Path $buildPath 'main'
$testOutput = Join-Path $buildPath 'test'
$artifactPath = Join-Path $repoPath 'docs/testing/artifacts'
New-Item -ItemType Directory -Path $mainOutput, $testOutput, $artifactPath -Force | Out-Null
$records = [System.Collections.Generic.List[object]]::new()

function Invoke-CheckedNative {
    param([string]$Label, [string]$Executable, [string[]]$NativeArguments)
    $at = (Get-Date).ToUniversalTime().ToString('o')
    $lines = @(& $Executable @NativeArguments 2>&1 | ForEach-Object { $_.ToString() })
    $code = $LASTEXITCODE
    $records.Add([ordered]@{ label=$Label; evidence='NEWLY EXECUTED'; startedAt=$at;
        command=$Executable; arguments=$NativeArguments; exitCode=$code; output=$lines })
    $lines | Write-Output
    if ($code -ne 0) { throw "$Label failed with exit code $code" }
}

$result = 'FAIL'
Push-Location $repoPath
try {
    Invoke-CheckedNative 'Runtime' $Java @('-version')
    Invoke-CheckedNative 'Compiler' $Compiler @('-version')
    $mainSources = @(Get-ChildItem -LiteralPath 'core/src/main/java' -Filter '*.java' -Recurse -File | Sort-Object FullName)
    $testSources = @(Get-ChildItem -LiteralPath 'core/src/test/java' -Filter '*.java' -Recurse -File | Sort-Object FullName)
    $mainArgs = Join-Path $buildPath 'main-sources.txt'
    $testArgs = Join-Path $buildPath 'test-sources.txt'
    [System.IO.File]::WriteAllLines($mainArgs, [string[]]@($mainSources | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }))
    [System.IO.File]::WriteAllLines($testArgs, [string[]]@($testSources | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }))
    Invoke-CheckedNative 'Core main compile' $Compiler @('--release','21','-Xlint:all','-Werror','-d',$mainOutput,('@'+$mainArgs))
    Invoke-CheckedNative 'Core test compile' $Compiler @('--release','21','-Xlint:all','-Werror','-cp',$mainOutput,'-d',$testOutput,('@'+$testArgs))
    $classPath = $mainOutput + [System.IO.Path]::PathSeparator + $testOutput
    $suites = @(
        'io.acra.core.tests.TestSuite',
        'io.acra.core.tests.sprint3.Sprint3CoreTestSuite',
        'io.acra.core.tests.sprint4.Sprint4CoreVerificationTestSuite',
        'io.acra.core.tests.sprint4.Sprint4EngineSecurityTestSuite',
        'io.acra.core.tests.sprint4.Sprint4GraphIntegrationTestSuite',
        'io.acra.core.tests.sprint4.Sprint4ProductCompletionTestSuite',
        'io.acra.core.tests.sprint5.Sprint5AssessmentGuardTestSuite',
        'io.acra.core.tests.sprint5.Sprint5CorrelationSafetyTestSuite',
        'io.acra.core.tests.sprint5.Sprint5SerializationSecurityTestSuite',
        'io.acra.core.tests.sprint5.Sprint5EvidenceIntegrityTestSuite',
        'io.acra.core.tests.sprint5.Sprint5PolicyValidationTestSuite',
        'io.acra.core.tests.sprint5.Sprint5ContextNormalizationTestSuite',
        'io.acra.core.tests.sprint5.Sprint5AuthorizationDimensionTestSuite',
        'io.acra.core.tests.sprint5.Sprint5OrchestrationTestSuite'
    )
    foreach ($suite in $suites) {
        Invoke-CheckedNative $suite $Java @('-ea','-cp',$classPath,$suite)
    }
    $reasoningSources = @(Get-ChildItem -LiteralPath 'core/src/main/java/io/acra/core/engine' -Filter '*Assessment*.java' -File)
    $networkMatches = @($reasoningSources | Select-String -Pattern 'java\.net|ProcessBuilder|Runtime\.getRuntime|java\.nio\.channels\.Socket')
    if ($networkMatches.Count -ne 0) { throw 'Network/process access found in assessment reasoning sources' }
    $records.Add([ordered]@{ label='Assessment network/process dependency scan'; evidence='NEWLY EXECUTED';
        command='Select-String'; scope=@($reasoningSources.Name); matches=$networkMatches.Count; result='PASS' })
    $result = 'PASS'
} finally {
    Pop-Location
    $sourceInventory = @()
    if ($mainSources) {
        $sourceInventory = @(@($mainSources) + @($testSources) | ForEach-Object {
            [ordered]@{ path=$_.FullName.Substring($repoPath.Length+1).Replace('\','/');
                sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() }
        })
    }
    $report = [ordered]@{ project='ACRA'; checkpoint='S5-DEFENSIVE-CONTINUATION'; evidence='NEWLY EXECUTED';
        result=$result; startedAt=$startedAt; finishedAt=(Get-Date).ToUniversalTime().ToString('o');
        repositoryPath=$repoPath; buildPath=$buildPath; targetJavaRelease=21;
        liveLabExecuted=$false; mavenAvailable=[bool](Get-Command mvn -ErrorAction SilentlyContinue);
        records=$records; sourceInventory=$sourceInventory }
    $json = $report | ConvertTo-Json -Depth 12
    [System.IO.File]::WriteAllText((Join-Path $artifactPath 'verification-s5-defensive.json'), $json + [Environment]::NewLine)
    $textLines = @("S5 DEFENSIVE VERIFICATION $result", "Started UTC: $startedAt", 'Evidence: NEWLY EXECUTED',
        'Scope: core compilation, offline regression and defensive S5 tests; no live lab run.')
    foreach ($record in $records) {
        $textLines += [string]$record.label
        $textLines += @($record.output)
        if ($record.Contains('exitCode')) { $textLines += ('Exit code: ' + $record.exitCode) }
    }
    [System.IO.File]::WriteAllLines((Join-Path $artifactPath 'verification-s5-defensive.txt'), [string[]]$textLines)
}
Write-Output "S5 DEFENSIVE VERIFICATION $result"
