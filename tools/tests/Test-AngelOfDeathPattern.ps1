param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/angel-pattern-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath `
    "$repoPath/src/main/java/net/kazi/kazimod/abilities/YagiAwakening/AngelOfDeathPattern.java" `
    "$PSScriptRoot/AngelOfDeathPatternTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Angel of death pattern checks failed to compile.' }
& "$JavaBin/java.exe" -cp $classesPath AngelOfDeathPatternTest
if ($LASTEXITCODE -ne 0) { throw 'Angel of death pattern checks failed.' }
