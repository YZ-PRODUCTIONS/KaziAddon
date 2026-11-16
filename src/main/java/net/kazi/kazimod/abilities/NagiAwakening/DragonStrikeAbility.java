package net.kazi.kazimod.abilities.NagiAwakening;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import net.kazi.kazimod.entities.DragonStrikeEntity;
import net.kazi.kazimod.entities.DragonAttackVfxEntity;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.abilities.haki.BusoshokuHakiInternalDestructionAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** Maintains eight passive summons. Each dragon owns its movement, decisions and attack timers. */
public final class DragonStrikeAbility extends PassiveAbility2 {
    public static final double RANGE = 100;
    public static final double ACTIVATION_HEALTH_FRACTION = InnerDragonsSequence.HEALTH_FRACTION;
    private static final int SPEARHEAD_PARALYSIS_TICKS = 2 * 20;
    private static final String NEXT_CUTSCENE_TICK = "kazimod:inner_dragons_next_cutscene";
    // Keep the saved ability ID stable when changing its displayed name.
    public static final AbilityCore<DragonStrikeAbility> INSTANCE = new AbilityCore.Builder<>(
            "Inner Dragons", "dragon_strike", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, DragonStrikeAbility::new)
            .setUnlockCheck(NagiAwakeningSupport::canUnlock)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/dragons_roar.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.dragon_strike.description"),
                    new TranslationTextComponent("ability.kazimod.dragon_strike.specials.0"),
                    new TranslationTextComponent("ability.kazimod.dragon_strike.specials.1"),
                    new TranslationTextComponent("ability.kazimod.dragon_strike.specials.2"),
                    new TranslationTextComponent("ability.kazimod.dragon_strike.specials.3"))
            .build();

    private final DealDamageComponent damage = new DealDamageComponent(this);
    private final BusoshokuHakiInternalDestructionAbility internalDestruction =
            BusoshokuHakiInternalDestructionAbility.INSTANCE.createAbility();
    private final List<DragonStrikeEntity> dragons = new ArrayList<>(8);
    private final InnerDragonsSequence awakeningSequence = new InnerDragonsSequence();
    private net.minecraft.world.World sequenceWorld;
    private net.minecraft.world.World armWorld;
    private boolean transformedArm;
    private int summonRetryTicks;

    public DragonStrikeAbility(AbilityCore<DragonStrikeAbility> core) {
        super(core);
        addComponents(damage);
        addDuringPassiveEvent(this::summonTick);
        addRemoveEvent((user, ability) -> {
            if (!user.level.isClientSide) dismiss(user);
        });
    }

    @Override public void tick(LivingEntity user) {
        if (!user.level.isClientSide) {
            clearLegacyCooldown(user);
            if (sequenceWorld != null && sequenceWorld != user.level) {
                InnerDragonsCutscenes.cancel(user);
                awakeningSequence.stop();
                sequenceWorld = null;
            }
            boolean eligible = canMaintainSummons(user);
            boolean started = awakeningSequence.update(user.getHealth(), user.getMaxHealth(),
                    user.level.getGameTime(), eligible);
            // Passive callbacks are skipped while suppressed, but their summons still need cleanup.
            if (!eligible) dismiss(user);
            else if (started) {
                if (reserveCutscene(user)) {
                    sequenceWorld = user.level;
                    InnerDragonsCutscenes.start(user, awakeningSequence.startTick());
                } else {
                    // Skip only the cinematic. The health latch remains consumed and summons
                    // activate immediately, without a deferred replay when the cooldown expires.
                    awakeningSequence.stop();
                }
            }
            updateArm(user, awakeningSequence.shouldTransformArm(user.level.getGameTime(), eligible));
        }
        // Always tick the base components so temporary suppression can expire normally.
        super.tick(user);
    }

    private static boolean reserveCutscene(LivingEntity user) {
        if (!(user.level instanceof ServerWorld)) return false;
        // One authoritative clock across dimensions. Unlike day time, /time set cannot reset it.
        long now = ((ServerWorld) user.level).getServer().overworld().getGameTime();
        CompoundNBT data = user.getPersistentData();
        if (user instanceof PlayerEntity) {
            // Forge copies PlayerPersisted on respawn; the timestamp also survives login and saves.
            if (!data.contains(PlayerEntity.PERSISTED_NBT_TAG, 10))
                data.put(PlayerEntity.PERSISTED_NBT_TAG, new CompoundNBT());
            data = data.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        }
        if (!InnerDragonsCutsceneCooldown.ready(now, data.getLong(NEXT_CUTSCENE_TICK))) return false;
        data.putLong(NEXT_CUTSCENE_TICK, InnerDragonsCutsceneCooldown.nextAllowedTick(now));
        return true;
    }

    /** Clear only the saved replay timer; an active scene and its health-cycle latch stay intact. */
    public static void resetCutsceneCooldown(LivingEntity user) {
        if (user.level.isClientSide) return;
        CompoundNBT data = user.getPersistentData();
        if (user instanceof PlayerEntity) data = data.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        data.remove(NEXT_CUTSCENE_TICK);
    }

    private boolean canMaintainSummons(LivingEntity user) {
        if (user == null || !user.isAlive() || user.removed || user.isSpectator()
                || (!(user.getHealth() < user.getMaxHealth() * ACTIVATION_HEALTH_FRACTION)
                    && !awakeningSequence.isPlaying(user.level.getGameTime()))
                || disableComponent.isDisabled() || isPaused() || !NagiAwakeningSupport.canUnlock(user)
                || user.hasEffect(KaziEffects.IMAGINE_BROKEN.get())
                || user.hasEffect(KaziEffects.LOST_WORKS_FRUIT_LOCK.get()) || !allowedAt(user)) return false;
        IAbilityData data = AbilityDataCapability.get(user);
        return data != null && data.getPassiveAbility(INSTANCE) == this;
    }

    /** Tracking snapshots use the same eligibility as the summons, including suppression. */
    public boolean hasTransformedArm(LivingEntity user) {
        return transformedArm && canMaintainSummons(user);
    }

    private void updateArm(LivingEntity user, boolean transformed) {
        if (transformedArm != transformed || transformed && armWorld != user.level) {
            InnerDragonsArmSync.send(user, transformed);
        }
        transformedArm = transformed;
        armWorld = user.level;
    }

    private void summonTick(LivingEntity user) {
        if (user.level.isClientSide) return;
        if (!canMaintainSummons(user)) { dismiss(user); return; }
        // The cinematic dragons emerge first; combat summons take over after the reveal.
        if (awakeningSequence.isPlaying(user.level.getGameTime())) return;
        EnumSet<DragonKind> present = EnumSet.noneOf(DragonKind.class);
        for (Iterator<DragonStrikeEntity> iterator = dragons.iterator(); iterator.hasNext();) {
            DragonStrikeEntity dragon = iterator.next();
            if (dragon.removed || !dragon.isAlive() || dragon.level != user.level
                    || dragon.getCaster() != user || !present.add(dragon.getKind())) {
                dragon.remove();
                iterator.remove();
            }
        }
        if (summonRetryTicks > 0) summonRetryTicks--;
        if (present.size() == DragonKind.values().length || summonRetryTicks > 0) return;
        // Retry blocked placements without rescanning for free space every tick.
        summonRetryTicks = 20;
        boolean firstSummon = present.isEmpty();
        boolean spawned = false;
        for (DragonKind kind : DragonKind.values()) {
            if (present.contains(kind)) continue;
            DragonStrikeEntity dragon = new DragonStrikeEntity(KaziEntities.DRAGON_STRIKE.get(), user.level);
            dragon.begin(user, this, kind);
            if (dragon.moveNearOwner(user) && user.level.addFreshEntity(dragon)) {
                dragons.add(dragon);
                spawned = true;
            } else dragon.remove();
        }
        if (firstSummon && spawned) user.level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundCategory.PLAYERS, 1F, 1.3F);
    }

    /** Old saves can carry the former active move's cooldown into the passive slot. */
    public boolean clearLegacyCooldown(LivingEntity user) {
        if (!cooldownComponent.isOnCooldown()) return false;
        cooldownComponent.stopCooldown(user);
        return true;
    }

    public boolean ownsDragons(DragonStrikeEntity entity) {
        LivingEntity owner = entity.getCaster();
        return owner != null && entity.level == owner.level && dragons.contains(entity) && canMaintainSummons(owner);
    }

    /** Discourage a pile-up without forcing all eight summons to share one target. */
    public int attackersOn(LivingEntity target) {
        int count = 0;
        for (DragonStrikeEntity dragon : dragons) if (dragon.isAlive() && dragon.getTarget() == target) count++;
        return count;
    }

    public static boolean canHit(LivingEntity user, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isSpectator() || target.level != user.level
                || !NagiAwakeningSupport.isEnemy(user, target)) return false;
        if (target instanceof PlayerEntity && ((PlayerEntity) target).isCreative()) return false;
        LivingEntity allegiance = target instanceof DragonStrikeEntity ? ((DragonStrikeEntity) target).getCaster()
                : target instanceof TameableEntity ? ((TameableEntity) target).getOwner() : target;
        if (allegiance != null && !NagiAwakeningSupport.isEnemy(user, allegiance)) return false;
        if (user instanceof PlayerEntity && allegiance instanceof PlayerEntity
                && (!((PlayerEntity) user).canHarmPlayer((PlayerEntity) allegiance)
                || user.getServer() == null || !user.getServer().isPvpAllowed())) return false;
        return allowedAt(user) && allowedAt(target) && (allegiance == null || allowedAt(allegiance));
    }

    public static boolean shouldHunt(LivingEntity user, LivingEntity target) {
        if (!canHit(user, target) || target.distanceToSqr(user) > RANGE * RANGE) return false;
        // Leave neutral wildlife alone unless it has entered combat with the caster.
        return target instanceof PlayerEntity || target instanceof IMob || target instanceof DragonStrikeEntity
                || target == user.getLastHurtByMob() || target == user.getLastHurtMob()
                || target instanceof MobEntity && ((MobEntity) target).getTarget() == user;
    }

    private static boolean allowedAt(Entity entity) {
        ProtectedArea area = ProtectedAreasData.get(entity.level).getProtectedArea(
                entity.blockPosition().getX(), entity.blockPosition().getY(), entity.blockPosition().getZ());
        return area == null || area.canUseAbility(INSTANCE);
    }

    public static boolean clearPath(Entity source, Vector3d from, Vector3d to) {
        RayTraceContext context = new RayTraceContext(from, to, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, source);
        // World.clip reads blocks/fluids through chunk-loading accessors. Traverse its exact
        // block path first, including vanilla's tiny endpoint expansion and corner handling.
        // Only query availability once per crossed chunk; never load distant chunks to aim.
        int[] lastChunk = {Integer.MIN_VALUE, Integer.MIN_VALUE};
        boolean loaded = IBlockReader.traverseBlocks(context, (trace, position) -> {
            int chunkX = position.getX() >> 4, chunkZ = position.getZ() >> 4;
            if (chunkX == lastChunk[0] && chunkZ == lastChunk[1]) return null;
            if (!source.level.hasChunkAt(position)) return Boolean.FALSE;
            lastChunk[0] = chunkX;
            lastChunk[1] = chunkZ;
            return null;
        }, trace -> Boolean.TRUE);
        return loaded && source.level.clip(context).getType() == RayTraceResult.Type.MISS;
    }

    /** Called by a single dragon after its own range, sight and cooldown checks. */
    public boolean attack(DragonStrikeEntity dragon, LivingEntity target) {
        LivingEntity user = dragon.getCaster();
        if (user == null || !ownsDragons(dragon) || !canHit(user, target) || !allowedAt(dragon)
                || !clearPath(dragon, dragon.getEyePosition(1), target.getBoundingBox().getCenter())) return false;
        DragonKind kind = dragon.getKind();
        // Preserve normal invulnerability frames and ability attribution; no eight-hit bypass.
        if (!hurtWithInternalDestruction(dragon, target, kind.damage)) return false;
        switch (kind) {
            case FIRST:
                boolean fruit = NagiAwakeningSupport.hasFruitPowers(target);
                ImagineBrokenEffect.apply(target, 60);
                if (fruit) NagiAwakeningSupport.playNullificationSound(user, target.position());
                break;
            case BLIND:
                target.addEffect(new EffectInstance(Effects.BLINDNESS, 80, 0));
                if (target instanceof MobEntity) {
                    Vector3d away = target.position().subtract(dragon.position()).normalize();
                    ((MobEntity) target).getNavigation().moveTo(target.getX() + away.x * 6,
                            target.getY(), target.getZ() + away.z * 6, 1.2);
                }
                break;
            case COBRA:
                target.addEffect(new EffectInstance(Effects.POISON, 100, 0));
                target.clearFire();
                Vector3d wave = target.position().subtract(dragon.position());
                target.knockback(.5F, -wave.x, -wave.z);
                break;
            case FOUR_EYED:
                target.addEffect(new EffectInstance(Effects.CONFUSION, 140, 0));
                target.addEffect(new EffectInstance(Effects.WEAKNESS, 80, 0));
                if (target instanceof MobEntity) ((MobEntity) target).setTarget(null);
                break;
            case CORPSE_FLAME:
                // Life-force flame is ability damage, not unattributed vanilla fire damage.
                dragon.heal(4);
                target.addEffect(new EffectInstance(Effects.HUNGER, 80, 0));
                break;
            case ICE_CRYSTAL:
                target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 80, 2));
                target.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN, 80, 1));
                break;
            case SPEARHEAD:
                // A lightning lance, not a real lightning entity that ignites terrain.
                Vector3d push = target.position().subtract(dragon.position());
                target.knockback(.7F, -push.x, -push.z);
                break;
            case ANGEL:
                // Brief salt-like binding instead of permanent conversion/player mind control.
                target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 30, 4));
                target.addEffect(new EffectInstance(Effects.GLOWING, 80, 0));
                break;
        }
        attackParticles(dragon, target);
        return true;
    }

    /** Shared checks for every special, including each frame of Angel's grab. */
    public boolean canSpecialHit(DragonStrikeEntity dragon, LivingEntity target) {
        if (dragon == null || dragon.level.isClientSide || !dragon.isAlive()) return false;
        LivingEntity user = dragon.getCaster();
        return user != null && ownsDragons(dragon) && shouldHunt(user, target)
                && allowedAt(dragon) && !AbilityHelper.isDodging(target)
                && clearPath(dragon, dragon.getEyePosition(1), target.getBoundingBox().getCenter());
    }

    /** Special attacks share the summon owner's damage attribution and normal hit/dodge rules. */
    public boolean specialHit(DragonStrikeEntity dragon, LivingEntity target, float amount) {
        if (!canSpecialHit(dragon, target)) return false;
        return hurtWithInternalDestruction(dragon, target, amount);
    }

    /** Every dragon strike carries native Internal Destruction, independent of the owner's Haki toggles. */
    private boolean hurtWithInternalDestruction(DragonStrikeEntity dragon, LivingEntity target, float amount) {
        LivingEntity user = dragon.getCaster();
        // Apply the native internal/piercing flags before damage while retaining the owner's credit.
        ModDamageSource source = ModDamageSource.causeAbilityDamage(user, this).setInternal();
        if (!damage.hurtTarget(user, target, amount * 2.0F, source) || AbilityHelper.isDodging(target)) return false;
        // Reuse MMNM's burst and cosmetic explosion only after a hit lands; it adds no extra damage.
        internalDestruction.onHitEffect(dragon, target, source);
        if (dragon.getKind() == DragonKind.SPEARHEAD)
            target.addEffect(new EffectInstance(ModEffects.PARALYSIS.get(), SPEARHEAD_PARALYSIS_TICKS, 0));
        return true;
    }

    private void attackParticles(DragonStrikeEntity dragon, LivingEntity target) {
        DragonAttackVfxEntity.spawn(dragon, DragonVfxPhase.NORMAL,
                dragon.getEyePosition(1), target.getBoundingBox().getCenter());
    }

    public void dispelProjectiles(DragonStrikeEntity dragon) {
        LivingEntity user = dragon.getCaster();
        if (user == null || !ownsDragons(dragon) || !allowedAt(dragon)) return;
        for (Entity projectile : dragon.level.getEntities(dragon, dragon.getBoundingBox().inflate(2.5), entity ->
                NagiAwakeningSupport.canNegate(user, entity) && NagiAwakeningSupport.isFruitProjectile(entity))) {
            if (allowedAt(projectile) && clearPath(dragon, dragon.getEyePosition(1), projectile.position())) {
                projectile.remove();
                NagiAwakeningSupport.playNullificationSound(user, projectile.position());
            }
        }
    }

    private void dismiss(LivingEntity user) {
        updateArm(user, false);
        InnerDragonsCutscenes.cancel(user);
        awakeningSequence.stop();
        sequenceWorld = null;
        for (DragonStrikeEntity dragon : dragons) dragon.remove();
        dragons.clear();
        summonRetryTicks = 0;
    }
}
