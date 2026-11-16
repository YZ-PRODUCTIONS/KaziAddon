param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$classesPath = Join-Path $repoPath 'output/validation/angel-visual-tests'
if (!(Test-Path -LiteralPath "$classesPath/AngelOfDeathVisualsTest.class")) {
    & "$PSScriptRoot/tests/Test-AngelOfDeathVisuals.ps1" -JavaBin $JavaBin
}
& "$JavaBin/javac.exe" -encoding UTF-8 -cp $classesPath -d $classesPath `
    "$repoPath/src/main/java/net/kazi/kazimod/abilities/YagiAwakening/AngelOfDeathPattern.java" `
    "$repoPath/src/main/java/net/kazi/kazimod/renderers/entities/asriel/AngelOfDeathVisuals.java" `
    "$PSScriptRoot/AngelOfDeathPreview.java"
if ($LASTEXITCODE -ne 0) { throw 'Angel of death preview failed to compile.' }
& "$JavaBin/java.exe" '-Djava.awt.headless=true' -cp $classesPath AngelOfDeathPreview $repoPath
if ($LASTEXITCODE -ne 0) { throw 'Angel of death preview failed.' }
