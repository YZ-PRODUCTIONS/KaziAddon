param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/angel-visual-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$sourcePath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod'
$sources = @(
    "$sourcePath/abilities/YagiAwakening/ShockerBreakerVisualTiming.java",
    "$sourcePath/abilities/YagiAwakening/ChaosSaberPattern.java",
    "$sourcePath/renderers/entities/asriel/ChaosSaberMotion.java",
    "$sourcePath/renderers/entities/asriel/ChaosSaberVisuals.java",
    "$sourcePath/renderers/entities/asriel/AsrielFlowPattern.java",
    "$sourcePath/renderers/entities/asriel/AsrielVisualGeometry.java",
    "$sourcePath/renderers/entities/asriel/ShockerBreakerVisuals.java",
    "$sourcePath/renderers/entities/asriel/ChaosBeamVisuals.java",
    "$sourcePath/renderers/entities/asriel/ChaosCannonVisuals.java",
    "$sourcePath/renderers/entities/asriel/AngelOfDeathVisuals.java",
    "$PSScriptRoot/AngelOfDeathVisualsTest.java"
)
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath $sources
if ($LASTEXITCODE -ne 0) { throw 'Angel of death visual tests failed to compile.' }
& "$JavaBin/java.exe" -cp $classesPath AngelOfDeathVisualsTest
if ($LASTEXITCODE -ne 0) { throw 'Angel of death visual tests failed.' }
