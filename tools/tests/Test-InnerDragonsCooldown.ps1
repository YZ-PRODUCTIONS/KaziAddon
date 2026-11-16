param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')

$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/inner-dragons-cooldown-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$sourcePath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod/abilities/NagiAwakening'

& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath `
    "$sourcePath/InnerDragonsSequence.java" "$sourcePath/InnerDragonsCutsceneCooldown.java" `
    "$PSScriptRoot/InnerDragonsCooldownTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Inner Dragons cooldown test compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath InnerDragonsCooldownTest
if ($LASTEXITCODE -ne 0) { throw 'Inner Dragons cooldown checks failed.' }
