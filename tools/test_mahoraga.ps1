$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$out = Join-Path $root 'build/mahoraga-logic-tests'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$sources = @(
    (Join-Path $root 'src/main/java/net/kazi/kazimod/mahoraga/AdaptationMemory.java'),
    (Join-Path $root 'src/main/java/net/kazi/kazimod/mahoraga/CombatMemory.java'),
    (Join-Path $root 'src/main/java/net/kazi/kazimod/mahoraga/MahoragaAction.java'),
    (Join-Path $PSScriptRoot 'tests/MahoragaLogicTest.java')
)
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { 'C:/Users/User/.jdks/corretto-1.8.0_482/bin' }
& (Join-Path $java 'javac.exe') -d $out @sources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& (Join-Path $java 'java.exe') -cp $out net.kazi.kazimod.mahoraga.MahoragaLogicTest
if ($LASTEXITCODE -ne 0) { throw 'Mahoraga regression test failed' }
