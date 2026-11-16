param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/chaos-beam-title-motion-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath `
    "$repoPath/src/main/java/net/kazi/kazimod/client/asriel/ChaosBeamTitleMotion.java" `
    "$repoPath/src/main/java/net/kazi/kazimod/client/asriel/ChaosBeamTitleFont.java" `
    "$repoPath/src/main/java/net/kazi/kazimod/client/asriel/ChaosBeamTitleVoiceCursor.java" `
    "$PSScriptRoot/ChaosBeamTitleMotionTest.java" `
    "$PSScriptRoot/ChaosBeamTitleVoiceTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Chaos Beam title motion compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath net.kazi.kazimod.client.asriel.ChaosBeamTitleMotionTest
if ($LASTEXITCODE -ne 0) { throw 'Chaos Beam title motion checks failed.' }
& "$JavaBin/java.exe" -cp $classesPath net.kazi.kazimod.client.asriel.ChaosBeamTitleVoiceTest
if ($LASTEXITCODE -ne 0) { throw 'Chaos Beam title voice checks failed.' }
