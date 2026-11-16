param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')

$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/illusion-shatter-health-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$sourcePath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod/abilities/NagiAwakening'

& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath `
    "$sourcePath/InnerDragonsSequence.java" "$sourcePath/IllusionShatterHealth.java" `
    "$PSScriptRoot/IllusionShatterHealthTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Illusion Shatter health test compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath IllusionShatterHealthTest
if ($LASTEXITCODE -ne 0) { throw 'Illusion Shatter health checks failed.' }
