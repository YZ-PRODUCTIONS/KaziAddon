param([string]$Javap = 'javap')

# Static checks against compiled classes. These guard the legacy/component routing
# regression; they do not substitute for a live multiplayer ability test.
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'build/classes/java/main'
$checks = 0

function Read-Bytecode([string]$ClassName) {
    $result = (& $Javap -classpath $classesPath -p -c $ClassName) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw "Compile $ClassName before running this check." }
    return $result
}
function Read-MethodBody([string]$Bytecode, [string]$Name, [string]$ParameterSuffix = '') {
    $escaped = [regex]::Escape($Name)
    $parameters = '\([^\r\n]*' + [regex]::Escape($ParameterSuffix) + '\)'
    $pattern = '(?ms)^  [^\r\n]*\b' + $escaped + $parameters + ';\r?\n    Code:\r?\n(?<Body>.*?)(?=^  (?:public|private|protected|static)|^})'
    $body = [regex]::Match($Bytecode, $pattern).Groups['Body'].Value
    if ([string]::IsNullOrEmpty($body)) { throw "Compiled method $Name was not found." }
    return $body
}
function Assert-Wiring([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}

foreach ($move in @('CombatPremonitionAbility')) {
    $code = Read-Bytecode "net.kazi.kazimod.abilities.NagiAwakening.$move"
    Assert-Wiring ([regex]::IsMatch($code, 'iconst_1\s+\d+: putfield[^\n]*isNew:Z')) "$move must opt into component-based player use/tick dispatch."
    Assert-Wiring ($code.Contains('Method addComponents:')) "$move must attach its components."
    Assert-Wiring ($code.Contains('ContinuousComponent.addTickEvent:')) "$move must register its continuous update."
    Assert-Wiring ($code.Contains('ContinuousComponent.addEndEvent:')) "$move must register its cleanup/cooldown."
    Assert-Wiring ($code.Contains('ContinuousComponent.triggerContinuity:')) "$move must start continuity from its use event."
    Assert-Wiring ($code.Contains('CooldownComponent.startCooldown:')) "$move must retain its cooldown."
    Assert-Wiring ($code.Contains('AbilityCore$Builder.setUnlockCheck:')) "$move must retain the awakening unlock check."
}

$registry = Read-Bytecode 'net.kazi.kazimod.init.KaziAbilities'
$setup = Read-Bytecode 'net.kazi.kazimod.setup.KaziDickSetup'
$nagiInjection = [regex]::Match($setup,
        '(?s)Field xyz/pixelatedw/mineminenomi/init/ModAbilities\.NAGI_NAGI_NO_MI.*?Method net/kazi/kazimod/util/FruitAbilityInjector\.addAbilities:').Value
Assert-Wiring (-not [string]::IsNullOrEmpty($nagiInjection)) 'Nagi must use the shared fruit injector, like the existing awakenings.'
foreach ($move in @('IllusionShatterAbility', 'DragonStrikeAbility')) {
    Assert-Wiring ($registry.Contains("NagiAwakening/$move.INSTANCE:")) "$move must be registered."
    Assert-Wiring ($nagiInjection.Contains("NagiAwakening/$move.INSTANCE:")) "$move must belong to the Nagi fruit list."
}
Assert-Wiring (-not $registry.Contains('ImagineBreakerAbility') -and -not $nagiInjection.Contains('ImagineBreakerAbility')) 'Imagine Breaker must not remain registered or granted to Nagi players.'
Assert-Wiring ($registry.Contains('CombatPremonitionAbility.INSTANCE:') -and -not $nagiInjection.Contains('CombatPremonitionAbility')) 'Combat Premonition remains registered for future use, outside the current Nagi kit.'
$lifecycle = Read-Bytecode 'net.kazi.kazimod.events.AwakeningAbilityLoginFix'
$progression = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.NagiAwakeningProgression'
Assert-Wiring ($progression.Contains('DragonStrikeAbility.INSTANCE:') -and $progression.Contains('DragonStrikeAbility.clearLegacyCooldown:') -and $progression.Contains('IAbilityData.addPassiveAbility:')) 'Existing awakened Nagi players must receive Inner Dragons as a passive without its former active cooldown.'
$dragons = Read-Bytecode 'net.kazi.kazimod.entities.DragonStrikeEntity'
Assert-Wiring (-not $dragons.Contains('net/minecraft/client/')) 'The server entity must not depend on client rendering classes.'
Assert-Wiring ($dragons.Contains('NetworkHooks.getEntitySpawningPacket:') -and $dragons.Contains('ownsDragons:')) 'Dragons need a spawn packet and passive-owner cleanup.'
$dragonMove = Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.DragonStrikeAbility'
foreach ($required in @('DragonKind.values:', 'List.add:', 'canHarmPlayer:', 'isPvpAllowed:', 'canUseAbility:', 'clearPath:', 'ImagineBrokenEffect.apply:', 'addRemoveEvent:', 'addDuringPassiveEvent:', 'IAbilityData.getPassiveAbility:')) {
    Assert-Wiring ($dragonMove.Contains($required)) "Inner Dragons requires $required for its automatic summons and safe combat."
}
Assert-Wiring ($dragonMove.Contains('extends xyz.pixelatedw.mineminenomi.api.abilities.PassiveAbility2') -and $dragonMove.Contains('AbilityType.PASSIVE:')) 'Inner Dragons must be registered and ticked as a passive.'
Assert-Wiring ($dragonMove.Contains('String dragon_strike') -and $dragonMove.Contains('String Inner Dragons')) 'The passive must retain the saved dragon_strike ID and Inner Dragons display name.'
Assert-Wiring (-not [regex]::IsMatch($dragonMove, 'ChargeComponent|ContinuousComponent|CooldownComponent\.startCooldown:|IAbilityData\.getEquippedAbility:')) 'The passive must not require manual charge, hold, cooldown, or an equipped slot.'
$dragonTick = Read-MethodBody $dragonMove 'tick'
Assert-Wiring ($dragonTick.Contains('PassiveAbility2.tick:') -and $dragonTick.Contains('Method canMaintainSummons:') -and $dragonTick.Contains('Method dismiss:')) 'Inactive summons must clean up while passive components keep ticking so temporary disables can expire.'
$dragonOwnership = Read-MethodBody $dragonMove 'ownsDragons'
Assert-Wiring ($dragonOwnership.Contains('Method canMaintainSummons:') -and $dragonOwnership.Contains('List.contains:')) 'Dragon ownership must require the current eligible passive and its tracked summons.'
Assert-Wiring (-not [regex]::IsMatch($dragonMove, '\.(setBlock|destroyBlock|explode):')) 'Inner Dragons must not destroy terrain.'
Assert-Wiring (-not $dragonMove.Contains('DragonStrikeShape')) 'The obsolete single-attack envelope must not control summons.'
Assert-Wiring ($dragons.Contains('extends net.minecraft.entity.FlyingEntity')) 'Each dragon must be a living flying entity.'
foreach ($required in @('findTarget:', 'attackCooldown:', 'PathNavigator.moveTo:', 'attackersOn:', 'DragonStrikeAbility.attack:', 'DragonStrikeAbility.dispelProjectiles:', 'noCollision:', 'hasChunkAt:', 'getLastHurtByMob:')) {
    Assert-Wiring ($dragons.Contains($required)) "Independent dragon AI requires $required."
}
Assert-Wiring (-not $dragons.Contains('noPhysics:')) 'Dragon flight must retain solid-block collisions.'
Assert-Wiring (-not $dragonMove.Contains('invulnerableTime:')) 'Eight dragons must not bypass damage immunity frames.'

# Both attack routes must apply MMNM's native Internal Destruction before resolving damage.
# Keep owner attribution; activating Haki on a summon alone does not affect owner-sourced hits.
$dragonNormalHit = Read-MethodBody $dragonMove 'attack'
$dragonSpecialHit = Read-MethodBody $dragonMove 'specialHit'
$dragonInternalHit = Read-MethodBody $dragonMove 'hurtWithInternalDestruction'
Assert-Wiring ($dragonNormalHit.Contains('Method hurtWithInternalDestruction:') -and $dragonSpecialHit.Contains('Method hurtWithInternalDestruction:')) 'Normal and special dragon attacks must both carry permanent Internal Destruction.'
Assert-Wiring ([regex]::Matches($dragonMove, 'DealDamageComponent\.hurtTarget:').Count -eq 1) 'All dragon damage must pass through the shared Internal Destruction route.'
Assert-Wiring ([regex]::IsMatch($dragonInternalHit, '(?s)DragonStrikeEntity\.getCaster:.*?astore\s+4.*?aload\s+4\s+\d+: aload_0\s+\d+: invokestatic[^\r\n]*ModDamageSource\.causeAbilityDamage:')) 'Internal Destruction must retain the summon owner as the attributed damage source.'
Assert-Wiring ($dragonInternalHit.Contains('AbilityDamageSource.setInternal:') -and $dragonInternalHit.IndexOf('AbilityDamageSource.setInternal:') -lt $dragonInternalHit.IndexOf('DealDamageComponent.hurtTarget:')) 'Native internal, armor-piercing and Logia flags must be set before damage is calculated.'
Assert-Wiring ([regex]::IsMatch($dragonInternalHit, '(?s)DealDamageComponent\.hurtTarget:.*?ifeq.*?AbilityHelper\.isDodging:.*?iconst_0\s+\d+: ireturn.*?BusoshokuHakiInternalDestructionAbility\.onHitEffect:')) 'The native burst must run only after a successful hit that was not dodged.'

# Hunting reach, navigation reach and recall must agree. A larger search box alone
# is ineffective if the old owner leash or total-pursuit timeout still cancels flight.
$dragonHunt = Read-MethodBody $dragonMove 'shouldHunt'
$dragonSearch = Read-MethodBody $dragons 'findTarget'
$dragonAi = Read-MethodBody $dragons 'customServerAiStep'
$dragonAttributeFactory = Read-MethodBody $dragons 'createAttributes'
$dragonConstants = (& $Javap -classpath $classesPath -p -constants 'net.kazi.kazimod.abilities.NagiAwakening.DragonStrikeAbility') -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Compile DragonStrikeAbility before checking its hunting radius.' }
Assert-Wiring ($dragonConstants.Contains('public static final double RANGE = 100.0d;')) 'The shared dragon hunting range must be 100 blocks.'
Assert-Wiring ([regex]::IsMatch($dragonHunt, 'LivingEntity\.distanceToSqr:[^\r\n]*\s+\d+: ldc2_w[^\r\n]*double 10000\.0d')) 'Hunting must enforce a 100-block sphere measured from the caster, not only an axis-aligned search box.'
Assert-Wiring ($dragonHunt.Contains('Method canHit:')) 'The extended hunting sphere must retain ally, PvP and protection checks.'
Assert-Wiring ([regex]::IsMatch($dragonAttributeFactory, 'Attributes\.FOLLOW_RANGE:[^\r\n]*\s+\d+: ldc2_w[^\r\n]*double 100\.0d')) 'Flying navigation must have the same 100-block follow range as target acquisition.'
Assert-Wiring ([regex]::IsMatch($dragonSearch, 'aload_1\s+\d+: invokevirtual[^\r\n]*LivingEntity\.getBoundingBox:[^\r\n]*\s+\d+: ldc2_w[^\r\n]*double 100\.0d\s+\d+: invokevirtual[^\r\n]*AxisAlignedBB\.inflate:')) 'Target searches must use the caster-centered 100-block broadphase, not the dragon position.'
Assert-Wiring ($dragonSearch.Contains('DragonStrikeAbility.shouldHunt:') -and $dragonSearch.Contains('DragonStrikeAbility.clearPath:')) 'Broadphase candidates must still pass the spherical enemy gate and line-of-sight check.'
Assert-Wiring ([regex]::IsMatch($dragonAi, 'Method distanceToSqr:[^\r\n]*\s+\d+: ldc2_w[^\r\n]*double 13456\.0d')) 'Recall must allow 116 blocks from the caster, including room to circle prey at the 100-block boundary.'
Assert-Wiring (-not [regex]::IsMatch($dragonAi, 'double (1600|2304)\.0d')) 'The old 40-block target leash and 48-block recall must not override the extended hunt radius.'
Assert-Wiring ($dragons.Contains('private double pursuitDistance;')) 'Each dragon must track its own remaining pursuit distance.'
$progressReset = [regex]::Match($dragonAi, '(?s)getfield[^\r\n]*pursuitDistance:D\s+\d+: dconst_1\s+\d+: dsub\s+\d+: dcmpg\s+\d+: ifge[^\r\n]*\s+\d+: aload_0\s+\d+: dload(?:_\d|\s+\d+)\s+\d+: putfield[^\r\n]*pursuitDistance:D\s+\d+: aload_0\s+\d+: iconst_0\s+\d+: putfield[^\r\n]*pursuitTicks:I')
Assert-Wiring ($progressReset.Success) 'Closing more than one block must save the new pursuit distance and reset the stuck timer.'
$stalledPursuit = [regex]::Match($dragonAi, '(?s)getfield[^\r\n]*pursuitTicks:I\s+\d+: iconst_1\s+\d+: iadd\s+\d+: dup_x1\s+\d+: putfield[^\r\n]*pursuitTicks:I\s+\d+: sipush\s+180\s+\d+: if_icmple.*?putfield[^\r\n]*blockedTarget:I')
Assert-Wiring ($stalledPursuit.Success -and $progressReset.Index -lt $stalledPursuit.Index) 'The 180-tick blacklist must measure time without forward progress, not total chase duration.'
Assert-Wiring ([regex]::Matches($dragonAi, 'double Infinityd\s+\d+: putfield[^\r\n]*pursuitDistance:D').Count -ge 4) 'Invalidation, recall, retargeting and successful attacks must reset the distance baseline.'
$dragonSight = Read-MethodBody $dragonMove 'clearPath'
$dragonTravel = Read-MethodBody $dragons 'travel'
$dragonVisibility = Read-MethodBody $dragons 'shouldRenderAtSqrDistance'
Assert-Wiring ($dragonSight.Contains('IBlockReader.traverseBlocks:') -and $dragonMove.Contains('World.hasChunkAt:') -and $dragonSight.IndexOf('IBlockReader.traverseBlocks:') -lt $dragonSight.IndexOf('World.clip:')) 'Long-distance sight checks must verify loaded chunks before reading blocks along the ray.'
Assert-Wiring ($dragonTravel.Contains('AxisAlignedBB.expandTowards:') -and $dragonTravel.Contains('World.hasChunkAt:') -and $dragonTravel.IndexOf('World.hasChunkAt:') -lt $dragonTravel.IndexOf('Method move:')) 'Flight must check the swept collision box for unloaded chunks before moving.'
Assert-Wiring ($dragonVisibility.Contains('double 128.0d') -and $dragonVisibility.Contains('getViewScale:')) 'The model visibility range must cover distant attacks while respecting the client entity-distance setting.'

$attributes = Read-Bytecode 'net.kazi.kazimod.init.KaziEntityAttributes'
Assert-Wiring ($attributes.Contains('DragonStrikeEntity.createAttributes:')) 'Living dragon attributes must be registered on both sides.'
$renderer = Read-Bytecode 'net.kazi.kazimod.renderers.entities.DragonStrikeRenderer'
Assert-Wiring ($renderer.Contains('DragonStrikeEntity.getKind:') -and -not $renderer.Contains('armPosition:') -and -not $renderer.Contains('getCaster:')) 'Each model must follow its own dragon, not the caster.'
Assert-Wiring ($renderer.Contains('DragonSummonModels.render:') -and $renderer.Contains('DragonSummonModels.texture:')) 'Every dragon must render its own textured Blockbench export.'
Assert-Wiring (-not $renderer.Contains('DragonStrikeMesh')) 'The obsolete procedural dragon mesh must not render alongside the new models.'
Assert-Wiring ($renderer.Contains('DragonStrikeEntity.getAttack:') -and $renderer.Contains('DragonStrikeEntity.getAge:')) 'Swim and bite animations must use synchronized entity timing.'
Assert-Wiring ($renderer.Contains('DragonStrikeEntity.getViewVector:') -and $renderer.Contains('GaeBolgVfxRenderer.orient:')) 'Each dragon must face its own interpolated flight direction.'
Assert-Wiring ($renderer.Contains('double 10.0d') -and $renderer.Contains('VfxDetail.visible:')) 'Culling must include the full animated dragon rather than only its collision box.'
Assert-Wiring ($renderer.Contains('DragonSummonModels.installReloadListener:')) 'Only the client renderer should install the model resource-reload handler.'

$models = Read-Bytecode 'net.kazi.kazimod.client.nagi.DragonSummonModels'
$drawNormalModels = Read-MethodBody $models 'render' 'boolean'
$drawModels = Read-MethodBody $models 'render' 'boolean, float'
$drawFaces = Read-MethodBody $models 'draw'
$reloadModels = Read-MethodBody $models 'installReloadListener'
Assert-Wiring ($models.Contains('RenderType.entityTranslucent:') -and -not $models.Contains('RenderType.entityCutout')) 'Every dragon variant must use an alpha-blended material rather than opaque cutout rendering.'
Assert-Wiring ([regex]::IsMatch($drawNormalModels, 'fconst_1\s+\d+: invokestatic[^\r\n]*Method render:')) 'Live dragons must forward full emergence opacity to the shared model renderer.'
Assert-Wiring ($models.Contains('MODEL_OPACITY;') -and [regex]::IsMatch($drawFaces, 'float 0\.55f\s+\d+: fload\s+6\s+\d+: fmul\s+\d+: invokeinterface[^\r\n]*IVertexBuilder\.color:\(FFFF\)')) 'All dragon faces, including emissive details, must multiply the named 55-percent-opacity setting by the cinematic emergence opacity.'
Assert-Wiring ($drawModels.Contains('MODELS:') -and $drawModels.Contains('java/util/Map.containsKey:') -and $drawModels.Contains('java/util/Map.get:')) 'Dragon models, including failed lookups, must be cached rather than loaded every frame.'
Assert-Wiring (-not $drawModels.Contains('IResourceManager.getResource:') -and -not $drawModels.Contains('CerberusRig.read:')) 'Rendering must not parse model JSON or open resources on every frame.'
Assert-Wiring ($drawModels.Contains('java/util/Arrays.fill:') -and -not $drawModels.Contains('CerberusRig.pose:')) 'Summons must reuse their baked pose arrays instead of recreating maps every frame.'
Assert-Wiring ($drawModels.Contains('animatedBones:') -and -not $drawModels.Contains('java/util/Map.values:')) 'Pose reset must use the baked bone array without per-frame map iterators.'
Assert-Wiring ($drawModels.Contains('String swim') -and $drawModels.Contains('String bite') -and $drawModels.Contains('CerberusRig.apply:')) 'The Blockbench swim and bite animations must both be sampled.'
Assert-Wiring ([regex]::Matches($drawModels, 'float 0\.0625f').Count -eq 3) 'Blockbench coordinates must use positive 1/16 scale on all three axes, without mirrored winding.'
foreach ($required in @('IVertexBuilder.uv:', 'IVertexBuilder.uv2:', 'IVertexBuilder.normal:', 'IVertexBuilder.overlayCoords:')) {
    Assert-Wiring ($drawFaces.Contains($required)) "Textured dragon faces require $required."
}
Assert-Wiring ($drawFaces.Contains('int 15728880') -and $drawFaces.Contains('emissive:')) 'Emissive features must remain lit without making the entire dragon full-bright.'
Assert-Wiring ($models.Contains('String detail_') -and $models.Contains('String glow_')) 'The model exporter detail/glow naming contract must be supported.'
Assert-Wiring ($reloadModels.Contains('registeredManager:') -and $reloadModels.Contains('IReloadableResourceManager.registerReloadListener:') -and $models.Contains('java/util/Map.clear:')) 'F3+T must invalidate cached models without registering duplicate reload listeners.'
Assert-Wiring ($models.Contains('Class.getResourceAsStream:') -and $models.Contains('IOException')) 'A broken model resource override must have a bundled-export loading fallback.'
$validateRig = Read-MethodBody $models 'validateRig'
$validateNodes = Read-MethodBody $models 'validateNodes'
$validateVector = Read-MethodBody $models 'validateVector'
$transformBone = Read-MethodBody $models 'transform'
Assert-Wiring ($models.Contains('Method validateRig:') -and $validateRig.Contains('Method validateNodes:')) 'Every model must validate its animation and geometry before accepting a resource override.'
Assert-Wiring ($validateRig.Contains('java/util/Set.contains:') -and $validateRig.Contains('String rotation') -and $validateRig.Contains('String position')) 'Animation tracks must target existing bones and supported channels.'
Assert-Wiring ($validateRig.Contains('java/lang/Float.isFinite:') -and $validateRig.Contains('CerberusRig$Key.time:') -and $validateRig.Contains('CerberusRig$Clip.loop:')) 'Clip lengths/key times must be finite and use the intended looping semantics.'
Assert-Wiring ($validateNodes.Contains('String Invalid dragon vertex UV:') -and $validateNodes.Contains('String Dragon mesh requires unit normals:')) 'Malformed texture coordinates and non-unit normals must be rejected at load time.'
Assert-Wiring ($validateNodes.Contains('CerberusRig$Quad.vertices:') -and $validateNodes.Contains('arraylength') -and $validateVector.Contains('java/lang/Float.isFinite:')) 'Face and transform arrays must be shape-checked and finite before rendering.'
Assert-Wiring ([regex]::Matches($transformBone, '\bifeq\b').Count -ge 3) 'Identity rotations must skip quaternion creation for each unused axis.'

# The factory lives behind Forge's compiled CLIENT-only subscriber annotation.
# None of the server-side summon entry points may refer to the helper or renderer.
$rendererRegistration = (& $Javap -classpath $classesPath -p -v 'net.kazi.kazimod.init.KaziRenderers') -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Compile KaziRenderers before running this check.' }
Assert-Wiring ([regex]::IsMatch($rendererRegistration, '(?s)net\.minecraftforge\.fml\.common\.Mod\$EventBusSubscriber\(.*?value=\[Lnet/minecraftforge/api/distmarker/Dist;\.CLIENT\]')) 'Dragon renderer registration must remain a CLIENT-only Forge subscriber.'
Assert-Wiring ($rendererRegistration.Contains('DragonStrikeRenderer.') -and $rendererRegistration.Contains('KaziEntities.DRAGON_STRIKE:')) 'The client-only registry must still install the dragon renderer factory.'
foreach ($entry in @(
        @{ Name = 'entity'; Code = $dragons },
        @{ Name = 'ability'; Code = $dragonMove },
        @{ Name = 'progression'; Code = $progression },
        @{ Name = 'kind'; Code = (Read-Bytecode 'net.kazi.kazimod.abilities.NagiAwakening.DragonKind') })) {
    Assert-Wiring (-not [regex]::IsMatch($entry.Code, 'DragonSummonModels|DragonStrikeRenderer|net/minecraft/client/')) "Server-side dragon $($entry.Name) must not load client model classes."
}
$localization = Get-Content -Raw (Join-Path $repoPath 'src/main/resources/assets/kazimod/lang/en_us.json') | ConvertFrom-Json
Assert-Wiring ($localization.'ability.kazimod.dragon_strike.description'.Contains('within 100 blocks of you') -and -not $localization.'ability.kazimod.dragon_strike.description'.Contains('within 32 blocks')) 'The dragon tooltip must describe the updated caster-centered 100-block hunting radius.'
foreach ($kind in @('first','blind','cobra','four_eyed','corpse_flame','ice_crystal','spearhead','angel')) {
    Assert-Wiring (-not [string]::IsNullOrEmpty($localization."entity.kazimod.dragon_strike.$kind")) "$kind needs a translated name."
}
Assert-Wiring ([regex]::Matches($lifecycle, 'NagiAwakeningProgression\.sync:').Count -eq 2) 'Nagi must be granted on awakening and restored on login/respawn.'
$aura = Read-Bytecode 'net.kazi.kazimod.client.nagi.ImagineBreakerAuraRenderer'
Assert-Wiring ($aura.Contains('IllusionShatterAbility.INSTANCE:') -and $aura.Contains('IAbilityData.getPassiveAbility:')) 'The arm aura must follow Illusion Shatter in the passive list.'
Assert-Wiring (-not $aura.Contains('ImagineBreakerAbility')) 'Imagine Breaker must no longer control the arm aura.'
Assert-Wiring (-not $aura.Contains('canUse:') -and -not $aura.Contains('COOLDOWN:')) 'The constant passive aura must remain visible between suppression procs.'
Assert-Wiring ($aura.Contains('IllusionShatterAbility.isPaused:') -and $aura.Contains('ModAbilityKeys.DISABLE:')) 'Paused/disabled passives must not display the aura.'
Write-Output "Nagi awakening wiring: $checks compiled-code checks passed."
