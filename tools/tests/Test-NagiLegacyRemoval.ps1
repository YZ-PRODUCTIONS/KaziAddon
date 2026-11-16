param(
    [string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin',
    [string]$Javap = 'C:/Users/th3wyz/.jdks/corretto-17.0.12/bin/javap.exe',
    [switch]$FixturesOnly
)

# Execute the actual retirement handler and boss-only access guard with small API
# fixtures, then inspect compiled wiring and the shipped dependency's save loader.
# These checks do not claim to replace a live Forge world-upgrade test.
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/nagi-retirement-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$fixturesPath = Join-Path $PSScriptRoot 'nagi_retirement_fixtures'
$fixtureSources = @(Get-ChildItem -LiteralPath $fixturesPath -Filter '*.java' -Recurse -File | ForEach-Object { $_.FullName })
$retirement = Join-Path $repoPath 'src/main/java/net/kazi/kazimod/events/RetiredNagiAbilities.java'
$guard = Join-Path $repoPath 'src/main/java/net/kazi/kazimod/entities/boss/sunjinwoo/abilities/SunJinWooOnlyAbility.java'
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath $fixtureSources $retirement $guard "$PSScriptRoot/NagiLegacyMappingsTest.java" "$PSScriptRoot/SunJinWooAbilityAccessTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Nagi retirement fixture compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath NagiLegacyMappingsTest
if ($LASTEXITCODE -ne 0) { throw 'Nagi retirement mapping behavior failed.' }
& "$JavaBin/java.exe" -cp $classesPath net.kazi.kazimod.entities.boss.sunjinwoo.abilities.SunJinWooAbilityAccessTest
if ($LASTEXITCODE -ne 0) { throw 'Sun Jin Woo access guard failed.' }
if ($FixturesOnly) { return }

$productionClasses = Join-Path $repoPath 'build/classes/java/main'
$dependency = Join-Path $repoPath 'libs/mine-mine-no-mi-1.16.5-0.10.11.jar'
$checks = 0
function Read-Bytecode([string]$ClassName, [string]$Classpath = $productionClasses, [switch]$VerboseCode) {
    $options = @('-p', '-c')
    if ($VerboseCode) { $options += '-v' }
    $result = (& $Javap -classpath $Classpath @options $ClassName) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw "Compile $ClassName before running this check." }
    return $result
}
function Read-Method([string]$Code, [string]$Name, [string]$ParameterMatch = '[^\r\n]*') {
    $pattern = '(?ms)^  [^\r\n]*\b' + [regex]::Escape($Name) + '\(' + $ParameterMatch + '\);\r?\n    Code:\r?\n(?<Body>.*?)(?=^  (?:public|private|protected|static)|^})'
    $body = [regex]::Match($Code, $pattern).Groups['Body'].Value
    if ([string]::IsNullOrEmpty($body)) { throw "Compiled method $Name was not found." }
    return $body
}
function Assert-Wiring([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}

$registry = Read-Bytecode 'net.kazi.kazimod.init.KaziAbilities'
$initializer = $registry.Substring($registry.IndexOf('static {};'))
$playerArray = [regex]::Match($initializer, '(?s)^.*?putstatic[^\r\n]*Field KAZIABILITY:').Value
$bossArray = [regex]::Match($initializer, '(?s)Field KAZIABILITY:.*?putstatic[^\r\n]*Field BOSS_ABILITIES:').Value
Assert-Wiring (-not [string]::IsNullOrEmpty($playerArray) -and -not [string]::IsNullOrEmpty($bossArray)) 'Player and boss registries must remain separate.'
Assert-Wiring (-not $playerArray.Contains('sunjinwoo/abilities/')) 'The KAZI player group must not grant boss copies of the retired moves.'
Assert-Wiring (-not $registry.Contains('abilities/NagiRework/')) 'The legacy Nagi implementation must no longer be registered.'
$commandGroup = Read-Method $registry 'lambda$register$0'
Assert-Wiring ($commandGroup.Contains('Field KAZIABILITY:') -and -not $commandGroup.Contains('BOSS_ABILITIES:')) 'The command group must expose only the player registry.'
$registration = Read-Method $registry 'register'
Assert-Wiring ($registration.Contains('Field BOSS_ABILITIES:') -and $registration.Contains('Arrays.stream:')) 'Hidden boss attacks must still be registered for boss networking.'

$legacyNames = @('Stride', 'Slice', 'Step', 'Box', 'Death')
foreach ($name in $legacyNames) {
    $className = "Silent${name}Ability"
    $oldClass = Join-Path $productionClasses "net/kazi/kazimod/abilities/NagiRework/$className.class"
    Assert-Wiring (-not (Test-Path -LiteralPath $oldClass)) "Stale legacy bytecode must be absent: $className"
    $move = Read-Bytecode "net.kazi.kazimod.entities.boss.sunjinwoo.abilities.$className"
    $metadata = Read-Bytecode "net.kazi.kazimod.entities.boss.sunjinwoo.abilities.$className" -VerboseCode
    Assert-Wiring ($bossArray.Contains("sunjinwoo/abilities/$className.INSTANCE:")) "$className must be in the isolated boss registry."
    Assert-Wiring ($move.Contains('extends net.kazi.kazimod.entities.boss.sunjinwoo.abilities.SunJinWooOnlyAbility')) "$className must inherit the tested use-time guard."
    Assert-Wiring ($move.Contains("String sun_jin_woo_silent_$($name.ToLowerInvariant())")) "$className must have a new boss-only registry ID."
    Assert-Wiring ($move.Contains('AbilityCore$Builder.setHidden:')) "$className must stay hidden from player ability selection."
    Assert-Wiring ($move.Contains('AbilityCore$Builder.setUnlockCheck:') -and $metadata.Contains('SunJinWooOnlyAbility.canUnlock:')) "$className must bind the tested boss-only unlock guard."
}

$setup = Read-Bytecode 'net.kazi.kazimod.setup.KaziDickSetup'
$nagiInjection = [regex]::Match($setup, '(?s)Field xyz/pixelatedw/mineminenomi/init/ModAbilities\.NAGI_NAGI_NO_MI.*?Method net/kazi/kazimod/util/FruitAbilityInjector\.addAbilities:').Value
Assert-Wiring (-not [string]::IsNullOrEmpty($nagiInjection)) 'Nagi must retain its current awakening move pool.'
Assert-Wiring (-not [regex]::IsMatch($nagiInjection, 'NagiRework|sunjinwoo/abilities|Silent(?:Stride|Slice|Step|Box|Death)Ability')) 'Nagi must never inject retired or boss-only attacks.'
foreach ($move in @('IllusionShatterAbility', 'DragonStrikeAbility')) {
    Assert-Wiring ($nagiInjection.Contains("NagiAwakening/$move.INSTANCE:") -and $playerArray.Contains("NagiAwakening/$move.INSTANCE:")) "The current kit must retain $move."
}
$progression = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.NagiAwakeningProgression'
Assert-Wiring (-not $registry.Contains('ImagineBreakerAbility') -and -not $nagiInjection.Contains('ImagineBreakerAbility') -and -not $progression.Contains('ImagineBreakerAbility')) 'Imagine Breaker must be removed from registration, grants and login repair.'
foreach ($removed in @('abilities/NagiAwakening/ImagineBreakerAbility', 'abilities/NagiAwakening/ImagineBreakerGeometry', 'abilities/NagiAwakening/ImagineBreakerBeamGeometry', 'mixin/ImagineBreakerBeamMixin')) {
    Assert-Wiring (-not (Test-Path -LiteralPath (Join-Path $productionClasses "net/kazi/kazimod/$removed.class"))) "Removed move bytecode must not remain: $removed"
}
$mixinConfig = Get-Content -LiteralPath (Join-Path $repoPath 'src/main/resources/kazimod.mixins.json') -Raw | ConvertFrom-Json
Assert-Wiring ($mixinConfig.mixins -notcontains 'ImagineBreakerBeamMixin') 'Deleted beam interception must not remain in the required mixin list.'
foreach ($beam in @('net.kazi.kazimod.entities.EnhancementLightEntity', 'net.kazi.kazimod.entities.EaVfxEntity', 'net.kazi.kazimod.entities.asriel.AsrielChaosFieldEntity', 'net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity')) {
    Assert-Wiring (-not (Read-Bytecode $beam).Contains('ImagineBreakerAbility')) "$beam must not call the deleted move."
}
Assert-Wiring ($playerArray.Contains('CombatPremonitionAbility.INSTANCE:') -and -not $nagiInjection.Contains('CombatPremonitionAbility')) 'The existing parked Combat Premonition registration must not reenter the Nagi grant pool.'
Assert-Wiring (-not [regex]::IsMatch($progression, 'NagiRework|sunjinwoo/abilities|REPLACEMENTS')) 'Current awakening progression must not recreate retired or boss moves.'
Assert-Wiring ($progression.Contains('DragonStrikeAbility.clearLegacyCooldown:') -and $progression.Contains('IllusionShatterAbility.clearLegacyCooldown:') -and $progression.Contains('IAbilityData.addPassiveAbility:')) 'Current passive repair must remain intact.'
$handler = Read-Bytecode 'net.kazi.kazimod.events.RetiredNagiAbilities' -VerboseCode
Assert-Wiring ($handler.Contains('net.minecraftforge.fml.common.Mod$EventBusSubscriber') -and $handler.Contains('FORGE')) 'Retirement must be registered on the Forge event bus used for saved mappings.'
Assert-Wiring ($handler.Contains('MissingMappings$Mapping.ignore:') -and -not $handler.Contains('MissingMappings$Mapping.remap:')) 'Old player IDs must be ignored, never redirected to a boss ID.'

# Verify the exact native loader this build ships. Missing registrations are how
# old unlocked, passive, and equipped entries disappear, even without Nagi/config eligibility.
$storage = Read-Bytecode 'xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability$1' $dependency
$read = Read-Method $storage 'readNBT' '[^\r\n]*IAbilityData[^\r\n]*'
$skipUnknown = 'IForgeRegistry\.getValue:[^\r\n]*\s+\d+: checkcast[^\r\n]*AbilityCore\s+\d+: astore\s+(\d+)\s+\d+: aload\s+\1\s+\d+: ifnonnull\s+\d+\s+\d+: goto\s+\d+'
Assert-Wiring ([regex]::Matches($read, $skipUnknown).Count -eq 2) 'Native passive and equipped loaders must skip unknown registry IDs before creating abilities.'
Assert-Wiring ($read.Contains('AbilityCoreUnlockWrapper.of:') -and $read.Contains('IAbilityData.addUnlockedAbility:')) 'Native saved unlocks must pass through the wrapper admission guard.'
$base = Read-Bytecode 'xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataBase' $dependency
$addWrapper = Read-Method $base 'addUnlockedAbility' 'xyz\.pixelatedw\.mineminenomi\.api\.abilities\.AbilityCoreUnlockWrapper'
Assert-Wiring ([regex]::IsMatch($addWrapper, '(?s)AbilityCoreUnlockWrapper.getAbilityCore:.*?ifnull\s+(\d+).*?\1: iconst_0\s+\d+: ireturn')) 'The native unlock admission path must reject null cores from removed IDs.'
foreach ($collection in @('clearUnlockedAbilities:', 'clearPassiveAbilities:', 'clearEquippedAbilities:')) {
    Assert-Wiring ($read.Contains($collection)) "Native load must clear stale existing entries with $collection"
}
Write-Output "Nagi removal compiled wiring and native save compatibility: $checks checks passed."
