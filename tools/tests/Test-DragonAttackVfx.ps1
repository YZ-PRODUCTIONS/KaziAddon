param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')

$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/dragon-attack-vfx-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$abilityPath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod/abilities/NagiAwakening'
$meshPath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod/models/abilities/DragonAttackMesh.java'
$previewPath = Join-Path $repoPath 'deliverables/nagi_dragons/attack_vfx_preview.png'

# Compile and render the actual geometry without launching Forge or changing gameplay.
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath `
    "$abilityPath/DragonKind.java" "$abilityPath/DragonSpecial.java" `
    "$abilityPath/DragonVfxPhase.java" $meshPath "$PSScriptRoot/DragonAttackVfxTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Dragon attack VFX test compilation failed.' }
& "$JavaBin/java.exe" '-Djava.awt.headless=true' -cp $classesPath DragonAttackVfxTest $previewPath
if ($LASTEXITCODE -ne 0) { throw 'Dragon attack VFX checks failed.' }
