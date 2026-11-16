param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/shocker-ex-entity-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$fixturesPath = Join-Path $PSScriptRoot 'shocker_entity_fixtures'
$fixtureSources = @(Get-ChildItem -LiteralPath $fixturesPath -Filter '*.java' -File -Recurse | ForEach-Object { $_.FullName })
$abilityPath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod/abilities/YagiAwakening'
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath $fixtureSources `
    "$abilityPath/ShockerBreakerExSequence.java" `
    "$abilityPath/ShockerBreakerDimensions.java" `
    "$abilityPath/ShockerBreakerVisualTiming.java" `
    "$abilityPath/YagiAwakeningMath.java" `
    "$repoPath/src/main/java/net/kazi/kazimod/entities/asriel/AsrielLightningEntity.java" `
    "$PSScriptRoot/ShockerBreakerExEntityTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Shocker Breaker production entity harness failed to compile.' }
& "$JavaBin/java.exe" -cp $classesPath ShockerBreakerExEntityTest
if ($LASTEXITCODE -ne 0) { throw 'Shocker Breaker production entity regression failed.' }
