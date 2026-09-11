param(
    [Parameter(Mandatory=$true)][string]$BaselineArchive,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [switch]$CurrentTreeOnly
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$repoPath = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$baselinePath = [System.IO.Path]::GetFullPath($BaselineArchive)
$expectedBaselineHash = '34b846321b1e2b4a6791bf8948c127cfba26697ec0425ec8dda8ea782e38df01'
if (-not $CurrentTreeOnly -and (Get-FileHash -LiteralPath $baselinePath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedBaselineHash) {
    throw 'Canonical baseline SHA-256 mismatch'
}
$outputPath = [System.IO.Path]::GetFullPath($OutputDirectory)
if ($outputPath.StartsWith($repoPath + [System.IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Output directory must be outside the repository to avoid recursive packaging'
}
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
$archivePath = Join-Path $outputPath 'acra-sprint-05-closure-continuation-checkpoint.zip'
if (Test-Path -LiteralPath $archivePath) { throw 'Archive already exists; use a new output directory to preserve it' }

function Is-Disposable([string]$RelativePath) {
    return $RelativePath -match '(^|/)(build|target|__pycache__|\.cache|\.git|\.gradle|node_modules)(/|$)|\.(class|pyc|pyo)$'
}
function Assert-SafeEntry([string]$Name) {
    if ($Name -match '(^/|\\|:|(^|/)\.\.(/|$)|(^|/)\.(/|$))' -or -not $Name.StartsWith('acra/')) {
        throw "Unsafe or unexpected archive path: $Name"
    }
}
function Stream-Hash([System.IO.Stream]$Stream) {
    $digest = [System.Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($digest.ComputeHash($Stream)).Replace('-','').ToLowerInvariant() }
    finally { $digest.Dispose() }
}
function Repository-Files {
    return @(Get-ChildItem -LiteralPath $repoPath -Recurse -Force -File | Where-Object {
        -not (Is-Disposable $_.FullName.Substring($repoPath.Length+1).Replace('\','/'))
    } | Sort-Object FullName)
}

$baselineFiles = @{}
if (-not $CurrentTreeOnly) {
$original = [System.IO.Compression.ZipFile]::OpenRead($baselinePath)
try {
    foreach ($entry in $original.Entries) {
        Assert-SafeEntry $entry.FullName
        if ($entry.FullName.EndsWith('/')) { continue }
        $relative = $entry.FullName.Substring(5)
        if (Is-Disposable $relative) { continue }
        if ($baselineFiles.ContainsKey($relative)) { throw "Duplicate baseline file: $relative" }
        $stream = $entry.Open()
        try { $baselineFiles[$relative] = Stream-Hash $stream } finally { $stream.Dispose() }
    }
} finally { $original.Dispose() }
}

# Generated inventories are part of the checkpoint, and are not evidence of implementation.
$changesPath = Join-Path $repoPath 'docs/sprints/sprint-05-file-changes.json'
if (-not (Test-Path -LiteralPath $changesPath)) { [System.IO.File]::WriteAllText($changesPath, '{}') }
$files = Repository-Files
$manifestNames = @($files | ForEach-Object { $_.FullName.Substring($repoPath.Length+1).Replace('\','/') } | Sort-Object)
[System.IO.File]::WriteAllLines((Join-Path $repoPath 'REPOSITORY_MANIFEST.txt'), [string[]]$manifestNames)
$currentFiles = @{}
foreach ($file in (Repository-Files)) {
    $relative = $file.FullName.Substring($repoPath.Length+1).Replace('\','/')
    $currentFiles[$relative] = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
}
$added = @($currentFiles.Keys | Where-Object { -not $baselineFiles.ContainsKey($_) } | Sort-Object)
$modified = @($currentFiles.Keys | Where-Object { $baselineFiles.ContainsKey($_) -and $baselineFiles[$_] -ne $currentFiles[$_] } | Sort-Object)
$deleted = @($baselineFiles.Keys | Where-Object { -not $currentFiles.ContainsKey($_) } | Sort-Object)
if ($deleted.Count -ne 0) { throw ('Unexpected non-disposable deletions: ' + ($deleted -join ', ')) }
$changeReport = [ordered]@{ baselineArchive=$baselinePath; baselineSha256=$expectedBaselineHash;
    baselineTrackedFileCount=$baselineFiles.Count; gitCommit=$null; gitBranch=$null;
    comparison='Actual file bytes against canonical ZIP; disposable files excluded';
    added=$added; modified=$modified; deleted=$deleted; renamed=@();
    renameDetection='No renames inferred; deleted files must be zero' }
if ($CurrentTreeOnly) {
    $journal = Get-Content -LiteralPath (Join-Path $repoPath 'docs/sprints/sprint-05-change-journal.json') -Raw | ConvertFrom-Json
    $added = @($journal.files | Where-Object status -eq 'ADDED' | ForEach-Object path)
    $modified = @($journal.files | Where-Object status -eq 'MODIFIED' | ForEach-Object path)
    $deleted = @()
    $changeReport = [ordered]@{ baselineArchive=$baselinePath; baselineSha256=$expectedBaselineHash;
        baselineComparison='BLOCKED - canonical ZIP unavailable at packaging time; hash previously verified';
        comparison='Recorded edit operations only; not a byte diff against the canonical ZIP';
        gitCommit=$null; gitBranch=$null; added=$added; modified=$modified; deleted=$deleted;
        renamed=@(); deletionEvidence='No deletion operation performed; baseline deletion comparison unavailable' }
}
[System.IO.File]::WriteAllText($changesPath, ($changeReport | ConvertTo-Json -Depth 8) + [Environment]::NewLine)

$files = Repository-Files
$sourceHashes = @{}
$zip = [System.IO.Compression.ZipFile]::Open($archivePath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in $files) {
        if (($file.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) { throw "Reparse point not supported: $($file.FullName)" }
        $relative = $file.FullName.Substring($repoPath.Length+1).Replace('\','/')
        $name = 'acra/' + $relative
        Assert-SafeEntry $name
        $sourceHashes[$relative] = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $name, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $zip.Dispose() }

$unpackPath = Join-Path $outputPath ('clean-unpack-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $unpackPath | Out-Null
$entries = [System.Collections.Generic.List[string]]::new()
$seen = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
$checkedZip = [System.IO.Compression.ZipFile]::OpenRead($archivePath)
try {
    foreach ($entry in $checkedZip.Entries) {
        Assert-SafeEntry $entry.FullName
        if (-not $seen.Add($entry.FullName)) { throw 'Duplicate archive entry' }
        if (Is-Disposable $entry.FullName) { throw 'Disposable artifact in ZIP' }
        $entries.Add($entry.FullName)
        $relative = $entry.FullName.Substring(5)
        $stream = $entry.Open()
        try { $entryHash = Stream-Hash $stream } finally { $stream.Dispose() }
        if (-not $sourceHashes.ContainsKey($relative) -or $sourceHashes[$relative] -ne $entryHash) { throw 'ZIP content mismatch' }
    }
} finally { $checkedZip.Dispose() }
[System.IO.Compression.ZipFile]::ExtractToDirectory($archivePath, $unpackPath)
$unpackedRepo = Join-Path $unpackPath 'acra'
$unpackedFiles = @{}
foreach ($file in (Get-ChildItem -LiteralPath $unpackedRepo -Recurse -Force -File)) {
    $relative = $file.FullName.Substring($unpackedRepo.Length+1).Replace('\','/')
    $unpackedFiles[$relative] = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
}
$missing = @($sourceHashes.Keys | Where-Object { -not $unpackedFiles.ContainsKey($_) })
$extra = @($unpackedFiles.Keys | Where-Object { -not $sourceHashes.ContainsKey($_) })
$changed = @($sourceHashes.Keys | Where-Object { $unpackedFiles.ContainsKey($_) -and $sourceHashes[$_] -ne $unpackedFiles[$_] })
if ($missing.Count -or $extra.Count -or $changed.Count) { throw 'Clean unpack comparison failed' }
foreach ($relative in $sourceHashes.Keys) {
    if ((Get-FileHash -LiteralPath (Join-Path $repoPath $relative) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $sourceHashes[$relative]) {
        throw 'Repository changed during packaging'
    }
}
$archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
[System.IO.File]::WriteAllText(($archivePath+'.sha256'), $archiveHash+'  '+[System.IO.Path]::GetFileName($archivePath)+[Environment]::NewLine)
[System.IO.File]::WriteAllLines((Join-Path $outputPath 'checkpoint-entries.txt'), $entries)
$verification = [ordered]@{ status='PASS'; evidence='NEWLY EXECUTED'; timestamp=(Get-Date).ToUniversalTime().ToString('o');
    archive=$archivePath; archiveBytes=(Get-Item -LiteralPath $archivePath).Length; sha256=$archiveHash;
    canonicalBaselineSha256=$expectedBaselineHash; repository=$repoPath; cleanUnpack=$unpackedRepo;
    baselineComparedNow=(-not [bool]$CurrentTreeOnly); inventoryBasis=$changeReport.comparison;
    zipIntegrity='PASS - every entry fully decompressed and SHA-256 compared'; unsafePaths=0;
    entryCount=$entries.Count; missing=$missing.Count; extra=$extra.Count; changed=$changed.Count;
    disposableArtifacts=0; added=$added.Count; modified=$modified.Count; deleted=$deleted.Count;
    decision='S5 SOFTWARE PARTIAL'; s6='BLOCKED - S5 completion prerequisite not met' }
[System.IO.File]::WriteAllText((Join-Path $outputPath 'checkpoint-verification.json'), ($verification | ConvertTo-Json -Depth 8)+[Environment]::NewLine)
$verification | ConvertTo-Json -Depth 8
