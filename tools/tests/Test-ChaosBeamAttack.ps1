param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/chaos-beam-attack-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$sourcePath = Join-Path $repoPath 'src/main/java'
& "$JavaBin/javac.exe" -encoding UTF-8 -sourcepath $sourcePath -d $classesPath `
    "$sourcePath/net/kazi/kazimod/abilities/YagiAwakening/ChaosBeamPattern.java" `
    "$sourcePath/net/kazi/kazimod/renderers/entities/asriel/ChaosBeamAttackVisuals.java" `
    "$PSScriptRoot/ChaosBeamAttackTest.java" `
    "$repoPath/tools/AsrielVisualPreview.java" `
    "$repoPath/tools/ChaosBeamAttackPreview.java"
if ($LASTEXITCODE -ne 0) { throw 'Chaos Beam checks failed to compile.' }
& "$JavaBin/java.exe" -cp $classesPath ChaosBeamAttackTest
if ($LASTEXITCODE -ne 0) { throw 'Chaos Beam checks failed.' }
& "$JavaBin/java.exe" '-Djava.awt.headless=true' -cp $classesPath ChaosBeamAttackPreview $repoPath
if ($LASTEXITCODE -ne 0) { throw 'Chaos Beam preview failed.' }
