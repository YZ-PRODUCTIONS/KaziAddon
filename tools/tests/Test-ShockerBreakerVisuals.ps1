param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin', [switch]$Preview)
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/shocker-visual-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$sourcePath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod'
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath `
    "$sourcePath/abilities/YagiAwakening/ShockerBreakerVisualTiming.java" `
    "$sourcePath/abilities/YagiAwakening/ShockerBreakerDimensions.java" `
    "$sourcePath/abilities/YagiAwakening/ChaosSaberPattern.java" `
    "$sourcePath/renderers/entities/asriel/ChaosSaberMotion.java" `
    "$sourcePath/renderers/entities/asriel/ChaosSaberVisuals.java" `
    "$sourcePath/renderers/entities/asriel/AsrielFlowPattern.java" `
    "$sourcePath/renderers/entities/asriel/AsrielVisualGeometry.java" `
    "$sourcePath/renderers/entities/asriel/ChaosBeamVisuals.java" `
    "$sourcePath/renderers/entities/asriel/ChaosCannonVisuals.java" `
    "$sourcePath/renderers/entities/asriel/ShockerBreakerVisuals.java" `
    "$sourcePath/renderers/entities/asriel/ShockerBreakerWarningVisuals.java" `
    "$PSScriptRoot/ShockerBreakerWarningTest.java" `
    "$PSScriptRoot/ShockerBreakerVisualsTest.java" `
    "$repoPath/tools/ShockerBreakerPreview.java"
if ($LASTEXITCODE -ne 0) { throw 'Shocker Breaker visual checks failed to compile.' }
& "$JavaBin/java.exe" -cp $classesPath ShockerBreakerVisualsTest
if ($LASTEXITCODE -ne 0) { throw 'Shocker Breaker visual regression failed.' }
& "$JavaBin/java.exe" -cp $classesPath ShockerBreakerWarningTest
if ($LASTEXITCODE -ne 0) { throw 'Shocker Breaker warning regression failed.' }
if ($Preview) {
    & "$JavaBin/java.exe" '-Djava.awt.headless=true' -cp $classesPath ShockerBreakerPreview $repoPath
    if ($LASTEXITCODE -ne 0) { throw 'Shocker Breaker visual preview failed.' }
}
