param(
    [string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-17.0.12/bin',
    [string]$OutputDirectory,
    [switch]$StillsOnly
)
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$classesPath = Join-Path $repoPath 'output/validation/hyper-goner-preview'
if ([string]::IsNullOrEmpty($OutputDirectory)) { $OutputDirectory = Join-Path $repoPath 'deliverables/asriel_vfx/hyper_goner' }
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
& "$JavaBin/javac.exe" -encoding UTF-8 -sourcepath "$repoPath/src/main/java" -d $classesPath "$PSScriptRoot/HyperGonerPreview.java"
if ($LASTEXITCODE -ne 0) { throw 'Hyper Goner preview compilation failed.' }
$previewArgs = @('-Djava.awt.headless=true', '-cp', "$classesPath;$repoPath/src/main/resources", 'HyperGonerPreview', $OutputDirectory)
if ($StillsOnly) { $previewArgs += '--stills-only' }
& "$JavaBin/java.exe" @previewArgs
if ($LASTEXITCODE -ne 0) { throw 'Hyper Goner preview rendering failed.' }
