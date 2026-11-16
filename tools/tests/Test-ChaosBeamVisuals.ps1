param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/chaos-beam-visual-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$sourcePath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod'
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath `
    "$sourcePath/abilities/YagiAwakening/ShockerBreakerVisualTiming.java" `
    "$sourcePath/abilities/YagiAwakening/ChaosSaberPattern.java" `
    "$sourcePath/abilities/YagiAwakening/ChaosBusterPattern.java" `
    "$sourcePath/abilities/YagiAwakening/YagiAwakeningMath.java" `
    "$sourcePath/renderers/entities/asriel/ChaosSaberMotion.java" `
    "$sourcePath/renderers/entities/asriel/ChaosSaberVisuals.java" `
    "$sourcePath/renderers/entities/asriel/AsrielFlowPattern.java" `
    "$sourcePath/renderers/entities/asriel/AsrielVisualGeometry.java" `
    "$sourcePath/renderers/entities/asriel/ShockerBreakerVisuals.java" `
    "$sourcePath/renderers/entities/asriel/ChaosBeamVisuals.java" `
    "$sourcePath/renderers/entities/asriel/ChaosCannonVisuals.java" `
    "$PSScriptRoot/ChaosBeamVisualsTest.java" `
    "$PSScriptRoot/ChaosCannonVisualsTest.java" `
    "$PSScriptRoot/ChaosBusterPatternTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Chaos beam visual checks failed to compile.' }
& "$JavaBin/java.exe" -cp $classesPath ChaosBeamVisualsTest
if ($LASTEXITCODE -ne 0) { throw 'Chaos beam visual regression failed.' }
& "$JavaBin/java.exe" -cp $classesPath ChaosCannonVisualsTest
if ($LASTEXITCODE -ne 0) { throw 'Chaos cannon visual regression failed.' }
& "$JavaBin/java.exe" -cp $classesPath ChaosBusterPatternTest
if ($LASTEXITCODE -ne 0) { throw 'Chaos Buster timing regression failed.' }
