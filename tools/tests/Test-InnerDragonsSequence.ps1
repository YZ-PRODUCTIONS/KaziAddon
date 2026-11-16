param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')

$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/inner-dragons-sequence-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$sourcePath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod/abilities/NagiAwakening'

# Compile the actual shared timeline. No client, server or replacement timing model is used.
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath `
    "$sourcePath/InnerDragonsSequence.java" "$sourcePath/DragonKind.java" "$sourcePath/DragonSpecial.java" `
    "$repoPath/src/main/java/net/kazi/kazimod/client/nagi/InnerDragonsEmergenceMesh.java" `
    "$PSScriptRoot/InnerDragonsSequenceTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Inner Dragons cinematic test compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath InnerDragonsSequenceTest
if ($LASTEXITCODE -ne 0) { throw 'Inner Dragons cinematic checks failed.' }
