param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin', [switch]$ChaosOnly, [switch]$ShockerOnly)
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$classesPath = Join-Path $repoPath 'output/validation/explosive-beam-preview'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$sourcePath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod'
$sources = @(
    "$sourcePath/abilities/YagiAwakening/ShockerBreakerVisualTiming.java",
    "$sourcePath/abilities/YagiAwakening/ShockerBreakerDimensions.java",
    "$sourcePath/abilities/YagiAwakening/ShockerBreakerExSequence.java",
    "$sourcePath/abilities/YagiAwakening/ChaosBusterPattern.java",
    "$sourcePath/abilities/YagiAwakening/ChaosSaberPattern.java",
    "$sourcePath/renderers/entities/asriel/ChaosSaberMotion.java",
    "$sourcePath/renderers/entities/asriel/ChaosSaberVisuals.java",
    "$sourcePath/renderers/entities/asriel/AsrielFlowPattern.java",
    "$sourcePath/renderers/entities/asriel/AsrielVisualGeometry.java",
    "$sourcePath/renderers/entities/asriel/ChaosCannonVisuals.java",
    "$sourcePath/renderers/entities/asriel/ShockerBreakerVisuals.java",
    "$sourcePath/renderers/entities/asriel/ShockerBreakerWarningVisuals.java",
    "$PSScriptRoot/ExplosiveBeamsPreview.java"
)
$chaosBeamPath = "$sourcePath/renderers/entities/asriel/ChaosBeamVisuals.java"
if (Test-Path -LiteralPath $chaosBeamPath) { $sources += $chaosBeamPath }
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath $sources
if ($LASTEXITCODE -ne 0) { throw 'Explosive beam preview compilation failed.' }
$previewArgs = @($repoPath)
if ($ChaosOnly) { $previewArgs += '--chaos-only' }
elseif ($ShockerOnly) { $previewArgs += '--shocker-only' }
& "$JavaBin/java.exe" '-Djava.awt.headless=true' -cp $classesPath ExplosiveBeamsPreview @previewArgs
if ($LASTEXITCODE -ne 0) { throw 'Explosive beam preview rendering failed.' }
