param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')

$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/dragon-special-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$sourcePath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod/abilities/NagiAwakening'

# Compile the actual profiles and cooldown state. No Gradle/Forge instance is launched.
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath `
    "$sourcePath/DragonKind.java" "$sourcePath/DragonSpecial.java" `
    "$sourcePath/DragonSpecialCycle.java" "$PSScriptRoot/DragonSpecialTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Dragon special test compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath DragonSpecialTest
if ($LASTEXITCODE -ne 0) { throw 'Dragon special checks failed.' }
