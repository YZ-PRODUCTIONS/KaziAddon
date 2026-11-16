param([string]$Javap = 'javap')

# Run after compileJava. Inspect the compiled passive without booting Minecraft.
# Cart Addon casts AbilityUseEvent.getAbility() to Ability, so a PassiveAbility2
# must never send active-use events from inside the ordinary attack callback.
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'build/classes/java/main'
$className = 'net.kazi.kazimod.abilities.NagiAwakening.IllusionShatterAbility'
$bytecode = (& $Javap -classpath $classesPath -p -c $className) -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Compile IllusionShatterAbility before running this check.' }

$checks = 0
function Assert-Bytecode([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}

Assert-Bytecode ($bytecode.Contains('extends xyz.pixelatedw.mineminenomi.api.abilities.PassiveAbility2')) 'Must remain a real passive.'
Assert-Bytecode ($bytecode.Contains('onMeleeContact(net.minecraftforge.event.entity.player.AttackEntityEvent)')) 'Keep the ordinary melee-contact hook.'
Assert-Bytecode ($bytecode.Contains('onNativeMeleeHit(xyz.pixelatedw.mineminenomi.api.events.WyLivingAttackEvent)')) 'Armed native melee needs a confirmed-hit fallback.'
Assert-Bytecode ($bytecode.Contains('onMeleeHit(net.minecraftforge.event.entity.living.LivingAttackEvent)')) 'Armed Forge melee needs the same fallback.'
foreach ($gate in @('DamageSource.isProjectile:', 'DamageSource.isExplosion:', 'DamageSource.isMagic:',
        'DamageSource.getEntity:', 'DamageSource.getDirectEntity:', 'DamageSource.getMsgId:',
        'ISweep.isSweeping:', 'api/damagesource/AbilityDamageSource')) {
    Assert-Bytecode ($bytecode.Contains($gate)) "Keep confirmed ordinary-melee filtering: $gate"
}
Assert-Bytecode (-not $bytecode.Contains('getMainHandItem:') -and -not $bytecode.Contains('ItemStack.isEmpty:')) 'Held items must not disable the passive contact effect.'
Assert-Bytecode ($bytecode.Contains('Set.contains:') -and $bytecode.Contains('Set.add:') -and $bytecode.Contains('Set.clear:') -and $bytecode.Contains('getGameTime:')) 'Deduplicate each target within the current tick while allowing different targets and later ticks.'
Assert-Bytecode ($bytecode.Contains('contactPoint:') -and $bytecode.Contains('getBoundingBox:') -and $bytecode.Contains('distanceToSqr:') -and $bytecode.Contains('World.clip:')) 'Validate range and solid blocks against the actual struck target.'
Assert-Bytecode (-not $bytecode.Contains('WyHelper.rayTraceBlocksAndEntities:')) 'Unrelated VFX entities must not swallow contact or its sound.'
Assert-Bytecode (-not $bytecode.Contains('events/ability/AbilityUseEvent')) 'Passive must not post active-use events (Cart ClassCastException regression).'
Assert-Bytecode (-not $bytecode.Contains('IEventBus.post')) 'Passive contact must not dispatch the incompatible active-use event bus path.'
Assert-Bytecode (-not $bytecode.Contains('AttackEntityEvent.setCanceled')) 'Do not cancel normal melee damage.'
Assert-Bytecode (-not [regex]::IsMatch($bytecode, '.(hurt|setAmount|setHealth):')) 'Do not replace, zero, or add to ordinary melee damage.'
Assert-Bytecode ($bytecode.Contains('ImagineBrokenEffect.apply:')) 'Keep suppression on the struck target.'
Assert-Bytecode (-not $bytecode.Contains('CooldownComponent.startCooldown:')) 'Illusion Shatter must not start a proc cooldown.'
Assert-Bytecode (-not $bytecode.Contains('CooldownComponent.getTooltip:')) 'Do not advertise a removed proc cooldown.'
Assert-Bytecode ($bytecode.Contains('CooldownComponent.stopCooldown:')) 'Existing saved proc cooldowns must be cleared on restore.'
$migration = (& $Javap -classpath $classesPath -p -c 'net.kazi.kazimod.abilities.NagiAwakening.NagiAwakeningProgression') -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Compile NagiAwakeningProgression before running this check.' }
Assert-Bytecode ($migration.Contains('IllusionShatterAbility.clearLegacyCooldown:')) 'Restoring the passive must clear its old cooldown.'
Assert-Bytecode (-not $migration.Contains('inheritCooldownFrom:')) 'Do not inherit the retired active-slot cooldown.'
foreach ($gate in @('IMAGINE_BROKEN', 'LOST_WORKS_FRUIT_LOCK', 'NO_HANDS', 'GUARDING')) {
    Assert-Bytecode ($bytecode.Contains(".$($gate):")) "Keep the $gate restriction without active-use events."
}
foreach ($gate in @('canUse:', 'allowsAbilityAt:', 'canHarmPlayer:', 'isPvpAllowed:', 'isDodging:')) {
    Assert-Bytecode ($bytecode.Contains($gate)) "Keep the $gate safety check."
}
Write-Output "Illusion Shatter melee regression: $checks compiled-code checks passed."
