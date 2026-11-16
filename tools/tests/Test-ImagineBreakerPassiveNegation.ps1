param(
    [string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-17.0.12/bin',
    [switch]$GeometryOnly
)

# Pure contact geometry plus compiled routing checks. These do not substitute
# for a live multiplayer collision test.
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'build/classes/java/main'
$geometryPath = Join-Path $repoPath 'output/validation/imagine-breaker-passive-geometry'
[IO.Directory]::CreateDirectory($geometryPath) | Out-Null
& "$JavaBin/javac.exe" -encoding UTF-8 -sourcepath "$repoPath/src/main/java" -d $geometryPath "$PSScriptRoot/ImagineBreakerPassiveGeometryTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Passive beam geometry compilation failed.' }
& "$JavaBin/java.exe" -cp $geometryPath ImagineBreakerPassiveGeometryTest
if ($LASTEXITCODE -ne 0) { throw 'Passive beam geometry checks failed.' }
if ($GeometryOnly) { return }

$checks = 0
function Assert-Negation([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}
function Read-Bytecode([string]$ClassName) {
    $code = (& "$JavaBin/javap.exe" -classpath $classesPath -p -c $ClassName) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw "Compile $ClassName before running this check." }
    return $code
}
function Read-Method([string]$Code, [string]$Name, [string]$Signature = '') {
    $pattern = '(?ms)^  [^\r\n]*\b' + [regex]::Escape($Name) + '\([^\r\n]*\);\r?\n    Code:\r?\n(?<Body>.*?)(?=^  (?:public|private|protected|static)|^})'
    foreach ($match in [regex]::Matches($Code, $pattern)) {
        if ([string]::IsNullOrEmpty($Signature) -or $match.Value.Split("`n")[0].Contains($Signature)) {
            return $match.Groups['Body'].Value
        }
    }
    throw "Missing compiled method $Name $Signature"
}

$passive = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.IllusionShatterAbility'
$gate = Read-Method $passive 'getActiveNegation'
foreach ($required in @('IllusionShatterHealth.immune:', 'getHealth:', 'getMaxHealth:',
        'IAbilityData.getPassiveAbility:', 'disableComponent:', 'isDisabled:', 'isPaused:',
        'IMAGINE_BROKEN:', 'LOST_WORKS_FRUIT_LOCK:', 'allowsAbilityAt:')) {
    Assert-Negation ($gate.Contains($required)) "The common passive gate must enforce $required"
}
foreach ($forbidden in @('getMainHandItem:', 'getEquippedAbility:', 'ContinuousComponent',
        'isOnCooldown:', 'canUse:', 'AbilityUseEvent')) {
    Assert-Negation (-not $gate.Contains($forbidden)) "Passive negation must not depend on $forbidden"
}
Assert-Negation ($passive.Contains('String Imagine Breaker') -and $passive.Contains('String illusion_shatter')) 'The renamed passive must retain its saved ability ID.'
$passiveTick = Read-Method $passive 'tick'
Assert-Negation ($passiveTick.Contains('PassiveAbility2.tick:') -and $passiveTick.Contains('ImagineBreakerNegation.tick:')) 'Passive ticking must retain base suppression updates and scan for nearby fruit projectiles without manual activation.'

$helper = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.ImagineBreakerNegation'
$beam = Read-Method $helper 'negateBeam' 'java.util.function.Function'
Assert-Negation ($beam.Contains('AbilityCategory.DEVIL_FRUITS:') -and $beam.Contains('NagiAwakeningSupport.isFruitProjectile:')) 'Beam eligibility requires its own fruit ability attribution.'
Assert-Negation ($beam.Contains('IllusionShatterAbility.getActiveNegation:')) 'Every beam contact must recheck the current health/state gate.'
Assert-Negation ($beam.Contains('Method canReach:')) 'Beam contacts must use the shared range and wall check.'
Assert-Negation ($beam.Contains('NagiAwakeningSupport.isEnemy:') -and $beam.Contains('isAlliedTo:')) 'Own and allied beams must survive.'
Assert-Negation (-not $helper.Contains('getMainHandItem:') -and -not $helper.Contains('ContinuousComponent')) 'Projectile and beam protection must remain passive with occupied hands.'
$reach = Read-Method $helper 'canReach'
Assert-Negation ($reach.Contains('ImagineBreakerNegationGeometry.withinRadius:') -and $reach.Contains('double 3.0d') -and $reach.Contains('DragonStrikeAbility.clearPath:')) 'Contact range must be an unobstructed three-block sphere.'
$scan = Read-Method $helper 'tick'
$impact = Read-Method $helper 'negateAtImpact'
foreach ($entry in @(@{ Name = 'passive scan'; Code = $scan }, @{ Name = 'impact interception'; Code = $impact })) {
    Assert-Negation ($entry.Code.Contains('IllusionShatterAbility.getActiveNegation:') -and $entry.Code.Contains('Method canReach:') -and $entry.Code.Contains('Method erase:')) "$($entry.Name) must recheck current HP, range and walls before removal."
}
Assert-Negation ($impact.Contains('NagiAwakeningSupport.isFruitProjectile:') -and $impact.Contains('NagiAwakeningSupport.canNegate:')) 'Impact interception must require fruit attribution and hostile ownership.'
foreach ($hook in @('onProjectileImpact', 'onAbilityProjectileImpact')) {
    $code = Read-Method $helper $hook
    Assert-Negation ($code.Contains('Method negateAtImpact:') -and $code.Contains('.setCanceled:')) "$hook must cancel its own pre-impact event only after successful negation."
}
$erase = Read-Method $helper 'erase'
Assert-Negation ($erase.Contains('Entity.remove:') -and $erase.Contains('NagiAwakeningSupport.playNullificationSound:')) 'Successful negation must remove the attack and play the shared sound.'
Assert-Negation ($erase.Contains('LAST_PARTICLES:') -and $erase.Contains('NagiAwakeningSupport.shatterParticles:')) 'Barrage negation must retain bounded per-tick feedback.'
Assert-Negation (-not $helper.Contains('net/minecraft/client/')) 'Passive negation must remain safe on a dedicated server.'

$support = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.NagiAwakeningSupport'
$projectile = Read-Method $support 'canNegate'
Assert-Negation ($projectile.Contains('isFruitProjectile:') -and $projectile.Contains('getThrower:') -and $projectile.Contains('getOwner:')) 'Fruit-only projectile filtering must retain native and addon owner attribution.'
Assert-Negation ($projectile.Contains('isEnemy:') -and $projectile.Contains('isAlliedTo:')) 'Own and allied projectiles must survive.'
Assert-Negation (-not $projectile.Contains('DevilFruitCapability')) 'A shooter owning a fruit does not make ordinary projectiles eligible.'
$fruit = Read-Method $support 'isFruitProjectile'
Assert-Negation ($fruit.Contains('AbilityProjectileEntity.getParent:') -and $fruit.Contains('AbilityProjectileEntity.getDamageSource:')) 'Both parent ability and damage-source classification must remain supported.'

$mixins = Get-Content -LiteralPath (Join-Path $repoPath 'src/main/resources/kazimod.mixins.json') -Raw | ConvertFrom-Json
Assert-Negation ($mixins.mixins -contains 'ImagineBreakerPassiveBeamMixin' -and $mixins.mixins -notcontains 'ImagineBreakerBeamMixin') 'Only the new passive beam hook must be registered.'
$native = Read-Bytecode 'net.kazi.kazimod.mixin.ImagineBreakerPassiveBeamMixin'
Assert-Negation ($native.Contains('ImagineBreakerNegation.negateBeam:') -and $native.Contains('CallbackInfo.cancel:')) 'Successful native beam removal must stop the same tick before damage.'
Assert-Negation ($native.Contains('Field travelSpeed:') -and $native.Contains('Field maxTravelDistance:') -and $native.Contains('LightningEntity.getLength:')) 'Native geometry must account for this tick''s beam growth.'
$routes = @(
    @{ Class = 'net.kazi.kazimod.entities.EnhancementLightEntity'; Method = 'updateHitboxTargets'; Effect = 'EnhancementAbility.hitThirdBlast:' },
    @{ Class = 'net.kazi.kazimod.entities.EaVfxEntity'; Method = 'tick'; Effect = 'Method eraseTerrain:' },
    @{ Class = 'net.kazi.kazimod.entities.asriel.AsrielChaosFieldEntity'; Method = 'tick'; Effect = 'Method damageLine:' },
    @{ Class = 'net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity'; Method = 'tick'; Effect = 'Method damage:' }
)
foreach ($route in $routes) {
    $method = Read-Method (Read-Bytecode $route.Class) $route.Method
    $call = $method.IndexOf('ImagineBreakerNegation.negateBeam:')
    $effect = $method.IndexOf($route.Effect)
    Assert-Negation ($call -ge 0 -and $effect -gt $call) "$($route.Class) must check the passive before damage or terrain effects."
    Assert-Negation ([regex]::IsMatch($method.Substring($call, $effect-$call), '(?s)ifeq.*?\breturn\b')) "$($route.Class) must stop immediately after successful negation."
}
Assert-Negation (-not (Test-Path -LiteralPath (Join-Path $classesPath 'net/kazi/kazimod/abilities/NagiAwakening/ImagineBreakerAbility.class'))) 'The deleted active move must not be restored.'
Write-Output "Imagine Breaker passive negation: $checks compiled routing checks passed."
