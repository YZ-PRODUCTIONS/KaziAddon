param([string]$Javap = 'javap')

# Resource and compiled-wiring regression checks, not an in-game audio test.
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$assetPath = Join-Path $repoPath 'src/main/resources/assets/kazimod'
$classesPath = Join-Path $repoPath 'build/classes/java/main'
$checks = 0
function Assert-Sound([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}
function Read-Bytecode([string]$Class) {
    $code = (& $Javap -classpath $classesPath -p -c $Class) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw "Compile $Class before running this check." }
    return $code
}

$sounds = Get-Content (Join-Path $assetPath 'sounds.json') -Raw | ConvertFrom-Json
$entry = $sounds.fruit_nullification
Assert-Sound ($null -ne $entry) 'Missing fruit_nullification sound event.'
Assert-Sound ($entry.sounds.Count -eq 3) 'There must be exactly three random variants.'
Assert-Sound (($entry.sounds | Select-Object -Unique).Count -eq 3) 'Sound variants must be distinct.'
foreach ($number in 1..3) {
    $name = "imagine_breaker_$number"
    Assert-Sound ($entry.sounds -contains "kazimod:$name") "Missing equally weighted variant $name."
    $bytes = [IO.File]::ReadAllBytes((Join-Path $assetPath "sounds/$name.ogg"))
    Assert-Sound ($bytes.Length -gt 1000 -and [Text.Encoding]::ASCII.GetString($bytes, 0, 4) -eq 'OggS') "Invalid OGG asset $name."
}
$lang = Get-Content (Join-Path $assetPath 'lang/en_us.json') -Raw | ConvertFrom-Json
Assert-Sound (-not [string]::IsNullOrWhiteSpace($lang.($entry.subtitle))) 'Missing nullification subtitle.'
$registry = Read-Bytecode 'net.kazi.kazimod.init.KaziSounds'
Assert-Sound ($registry.Contains('FRUIT_NULLIFICATION_SFX') -and $registry.Contains('String Fruit Nullification')) 'Sound event must be registered on both client and server.'
$support = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.NagiAwakeningSupport'
foreach ($required in @('ServerWorld', 'LAST_NULLIFICATION_SOUND', 'java/util/WeakHashMap', 'getGameTime:', 'playSound:', 'FRUIT_NULLIFICATION_SFX')) {
    Assert-Sound ($support.Contains($required)) "Missing shared playback protection: $required"
}
$shatter = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.IllusionShatterAbility'
Assert-Sound ([regex]::Matches($shatter, 'NagiAwakeningSupport.playNullificationSound:').Count -eq 4) 'Illusion Shatter must retain melee feedback and all three passive fruit-immunity callbacks.'
Assert-Sound (-not $shatter.Contains('NagiAwakeningSupport.hasFruitPowers:')) 'Valid Illusion Shatter melee procs must play their custom sound against fruitless targets too.'
Assert-Sound ($shatter.Contains('NagiAwakeningSupport.shatterParticles:') -and -not $shatter.Contains('NagiAwakeningSupport.shatter:')) 'Melee feedback must preserve particles without layering the glass-break sound over the custom recordings.'
$negation = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.ImagineBreakerNegation'
Assert-Sound ($negation.Contains('NagiAwakeningSupport.playNullificationSound:') -and $negation.Contains('NagiAwakeningSupport.shatterParticles:')) 'The renamed passive must use shared nullification feedback when erasing fruit projectiles or beams.'
$dragons = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.DragonStrikeAbility'
Assert-Sound ($dragons.Contains('NagiAwakeningSupport.isFruitProjectile:') -and $dragons.Contains('NagiAwakeningSupport.playNullificationSound:')) 'Inner Dragons must retain fruit-projectile nullification and its shared sound.'
$suppression = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.ImagineBrokenEffect'
Assert-Sound ($suppression.Contains('NagiAwakeningSupport.playNullificationSound:')) 'Suppressed fruit-cast attempts need nullification feedback too.'
Write-Output "Nagi nullification sound: $checks resource and compiled-code checks passed."
