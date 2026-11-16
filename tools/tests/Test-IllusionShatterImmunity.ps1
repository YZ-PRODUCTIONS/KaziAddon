param([string]$Javap = 'javap')

# Incoming fruit-damage immunity belongs to the renamed Imagine Breaker passive.
# The former active ImagineBreakerAbility remains retired.
# These compiled-code checks do not replace an in-game combat test.
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'build/classes/java/main'
$checks = 0
function Assert-Immunity([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}
function Read-Bytecode([string]$ClassName, [string]$Mode = '-c') {
    $result = (& $Javap -classpath $classesPath -p $Mode $ClassName) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw "Compile $ClassName before running this check." }
    return $result
}
function Get-MethodCode([string]$Bytecode, [string]$Name) {
    $pattern = '(?ms)^  (?:public|private|protected|static)[^\r\n]*\b' + [regex]::Escape($Name) + '\([^\r\n]*\).*?(?=^  (?:public|private|protected|static)|\z)'
    $method = [regex]::Match($Bytecode, $pattern)
    if (-not $method.Success) { throw "Missing method $Name" }
    return $method.Value
}

Assert-Immunity (-not (Test-Path -LiteralPath (Join-Path $classesPath 'net/kazi/kazimod/abilities/NagiAwakening/ImagineBreakerAbility.class'))) 'The removed Imagine Breaker move must not leave active compiled callbacks.'
$shatterClass = 'net.kazi.kazimod.abilities.NagiAwakening.IllusionShatterAbility'
$verbose = Read-Bytecode $shatterClass '-v'
$shatter = Read-Bytecode $shatterClass
$support = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.NagiAwakeningSupport'
foreach ($hook in @('onFruitAttack', 'onForgeFruitAttack', 'onFruitHurt')) {
    $code = Get-MethodCode $shatter $hook
    Assert-Immunity ($code.Contains('blocksFruitAttack:')) "$hook must apply the same immunity rules."
    Assert-Immunity ($code.Contains('.setCanceled:')) "$hook must cancel protected incoming hits."
}
Assert-Immunity ($verbose.Contains('net.minecraftforge.fml.common.Mod$EventBusSubscriber(') -and $verbose.Contains('modid="kazimod"')) 'Illusion Shatter must register incoming immunity on the Forge event bus.'
Assert-Immunity (-not $verbose.Contains('Dist;.CLIENT') -and -not $verbose.Contains('EventBusSubscriber$Bus;.MOD')) 'Passive immunity must work on dedicated servers through the Forge event bus.'
foreach ($entry in @(@{ Name = 'onFruitAttack'; Priority = 'HIGHEST' },
        @{ Name = 'onForgeFruitAttack'; Priority = 'HIGHEST' }, @{ Name = 'onFruitHurt'; Priority = 'LOWEST' })) {
    $pattern = '(?s)public static void ' + $entry.Name + '\([^\r\n]*\).*?net\.minecraftforge\.eventbus\.api\.SubscribeEvent\(\s+priority=Lnet/minecraftforge/eventbus/api/EventPriority;\.' + $entry.Priority
    Assert-Immunity ([regex]::IsMatch($verbose, $pattern)) "$($entry.Name) must retain its existing event priority."
}
$damageGate = Get-MethodCode $shatter 'blocksFruitAttack'
Assert-Immunity ($damageGate.Contains('getActiveNegation:') -and $damageGate.Contains('isDevilFruitAttack:')) 'Damage negation must share the passive state gate and require actual Devil Fruit attribution.'
$gate = Get-MethodCode $shatter 'getActiveNegation'
foreach ($required in @('isClientSide', 'isAlive:', 'isSpectator:', 'getHealth:', 'getMaxHealth:',
        'IllusionShatterHealth.immune:', 'canUnlock:', 'AbilityDataCapability.get:', 'IAbilityData.getPassiveAbility:', 'isDisabled:',
        '.IMAGINE_BROKEN:', '.LOST_WORKS_FRUIT_LOCK:', 'allowsAbilityAt:')) {
    Assert-Immunity ($gate.Contains($required)) "Missing immunity gate: $required"
}
Assert-Immunity ($gate.Contains('Field INSTANCE:')) 'Fruit immunity must look up the Illusion Shatter passive itself.'
Assert-Immunity ($gate.Contains('ifnull')) 'Missing user or ability data must fail safely.'
Assert-Immunity ($gate.Contains('disableComponent:') -and $gate.Contains('isDisabled:') -and $gate.Contains('isPaused:')) 'Disabled or paused Illusion Shatter instances must not retain incoming immunity.'
foreach ($exception in @('isUnavoidable:', 'isBypassInvul:', 'getHakiNature:', '.NO_HANDS:', '.GUARDING:',
        'isOnCooldown:', 'cooldownComponent:', 'canUse:', 'getEquippedAbility:', 'ImagineBreakerAbility',
        'isGuarding:', 'ContinuousComponent', 'getMainHandItem:', 'ItemStack.isEmpty:')) {
    Assert-Immunity (-not $gate.Contains($exception)) "Fruit immunity must not have the $exception exception."
}
$area = Get-MethodCode $shatter 'allowsAbilityAt'
Assert-Immunity ($area.Contains('ProtectedAreasData.get:') -and $area.Contains('ProtectedArea.canUseAbility:') -and $area.Contains('Field INSTANCE:')) 'Protected areas must authorize Illusion Shatter itself.'
Assert-Immunity (-not $shatter.Contains('ImagineBreakerAbility')) 'Passive fruit immunity must not depend on the removed move.'
Assert-Immunity (-not $shatter.Contains('net/minecraft/client/')) 'Server-side immunity must not load client rendering classes.'
$health = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.IllusionShatterHealth'
$threshold = Get-MethodCode $health 'immune'
Assert-Immunity ($threshold.Contains('Method active:') -and -not $threshold.Contains('0.75')) 'Incoming immunity must share the inclusive 30%-and-above passive health rule with no former 75% ceiling.'
$meleeGate = Get-MethodCode $shatter 'canTriggerSuppression'
Assert-Immunity ($meleeGate.Contains('IllusionShatterHealth.active:') -and $meleeGate.Contains('getHealth:') -and $meleeGate.Contains('getMaxHealth:')) 'Melee suppression must stop below 30% of current maximum health.'
$aura = Read-Bytecode 'net.kazi.kazimod.client.nagi.ImagineBreakerAuraRenderer'
Assert-Immunity ($aura.Contains('IllusionShatterHealth.active:') -and $aura.Contains('getHealth:') -and $aura.Contains('getMaxHealth:')) 'The passive arm aura must use the same health cutoff as melee suppression.'
Assert-Immunity ($aura.Contains('IllusionShatterAbility.INSTANCE:') -and $aura.Contains('getPassiveAbility:') -and -not $aura.Contains('ImagineBreakerAbility')) 'Moving immunity must not move the arm aura away from Illusion Shatter.'
$source = Get-MethodCode $support 'isDevilFruitAttack'
Assert-Immunity ($source.Contains('AbilityDamageSource.getAbilitySource:')) 'Use the actual MMNM damage-source ability API.'
Assert-Immunity ($source.Contains('getDirectEntity:') -and $source.Contains('getEntity:')) 'Check both direct and indirect projectile attribution.'
Assert-Immunity (-not $source.Contains('DevilFruitCapability')) 'Eating a fruit must not make ordinary M1 or environmental damage count as a fruit attack.'
$projectile = Get-MethodCode $support 'isFruitProjectile'
Assert-Immunity ($projectile.Contains('AbilityProjectileEntity.getParent:')) 'Keep legacy/addon projectile parent fallback.'
Assert-Immunity ($projectile.Contains('AbilityProjectileEntity.getDamageSource:')) 'Keep projectile damage-source fallback.'
Assert-Immunity (-not $projectile.Contains('IllusionShatterHealth')) 'Shared projectile classification must not inherit Illusion Shatter health restrictions.'
$category = Get-MethodCode $support 'isFruitAbility'
Assert-Immunity ($category.Contains('AbilityCategory.DEVIL_FRUITS:')) 'Filter by Devil Fruit category across all addon namespaces.'
Assert-Immunity ($category.Contains('ifnull')) 'Missing ability attribution must not crash.'
Write-Output "Illusion Shatter passive immunity regression: $checks compiled-code checks passed."
