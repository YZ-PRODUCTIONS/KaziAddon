package net.kazi.kazimod.abilities.Toki;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.kazi.kazimod.entities.TimeBubbleEntity;
import net.kazi.kazimod.abilities.Toki.TimeBarAbility;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ChronostasisGrigoraAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "chronostasis_grigora",
            new Pair[]{
                    ImmutablePair.of(
                            "Freezes up to 10 projectiles within a 30-block radius in time, " +
                                    "suspending each one inside a temporal bubble. " +
                                    "After 5 seconds, the frozen projectiles are erased.",
                            (Object) null
                    )
            }
    );

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final float COOLDOWN          = 360.0F; // 18 s
    private static final float HOLD_TICKS        = 100.0F; // 5 s
    private static final float RANGE             = 30.0F;
    /** Bubble life slightly longer than hold so it outlasts the freeze visually. */
    private static final int   BUBBLE_LIFE       = 110;
    /** Fixed display scale for the small bubble around a projectile. */
    private static final float PROJ_BUBBLE_SCALE = 0.6F;
    private static final float TIME_COST         = 75.0F;
    /** Maximum number of projectiles that can be frozen at once. */
    private static final int   MAX_FROZEN        = 20;
    private static final float BUBBLE_PITCH      = 1.8F;
    /**
     * Volume passed to ServerWorld#playSound. Minecraft derives audible range
     * as volume * 16 blocks, so 3.125F would cover exactly 50 blocks.
     * Using 4.0F gives a comfortable margin (64-block range).
     */
    private static final float BUBBLE_VOLUME     = 4.0F;

    public static final AbilityCore<ChronostasisGrigoraAbility> INSTANCE;

    // ── Components ────────────────────────────────────────────────────────────
    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onStart)
                    .addTickEvent(this::onTick)
                    .addEndEvent(this::onEnd);

    private final RangeComponent rangeComponent = new RangeComponent(this);

    // ── Runtime state ─────────────────────────────────────────────────────────
    private SphereEntity sphereEntity;
    /** IDs of every projectile we have already spawned a bubble for. */
    private final List<Integer> frozenIds = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────
    public ChronostasisGrigoraAbility(AbilityCore<ChronostasisGrigoraAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.rangeComponent
        });
        this.addUseEvent(this::onUse);
    }

    // ── Use ───────────────────────────────────────────────────────────────────
    private void onUse(LivingEntity entity, IAbility ability) {
        // Allow cancel without re-spending.
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.triggerContinuity(entity, HOLD_TICKS);
            return;
        }
        TimeBarAbility bar = getTimeBar(entity);
        if (bar == null || !bar.spendTimePoints(entity, 50F)) {
            if (entity instanceof PlayerEntity) {
                ((PlayerEntity) entity).displayClientMessage(
                        new net.minecraft.util.text.TranslationTextComponent("Not Enough Time Points"),
                        true
                );
            }
            return;
        }

        this.continuousComponent.triggerContinuity(entity, HOLD_TICKS);
    }

    // ── Start ─────────────────────────────────────────────────────────────────
    private void onStart(LivingEntity entity, IAbility ability) {
        this.frozenIds.clear();

        // Large indicator sphere showing the frozen radius.
        if (CommonConfig.INSTANCE.isExperiementalSpheresEnabled()) {
            this.sphereEntity = new SphereEntity(entity.level, entity);
            this.sphereEntity.setColor(new Color(100, 200, 255, 30));
            this.sphereEntity.setRadius(RANGE);
            this.sphereEntity.setDetailLevel(32);
            this.sphereEntity.setAnimationSpeed(1);
            entity.level.addFreshEntity(this.sphereEntity);
        }

        entity.level.playSound(
                (PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.ROOM_CREATE_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 1.1F
        );
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    private void onTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        // Scan for both mod AbilityProjectileEntity and vanilla ProjectileEntity.
        List<Entity> nearby = WyHelper.getNearbyEntities(
                entity.position(), entity.level,
                (double) RANGE, (double) RANGE, (double) RANGE,
                null,
                new Class[]{ AbilityProjectileEntity.class, ProjectileEntity.class }
        );

        for (Entity raw : nearby) {
            // Don't freeze the caster's own projectiles.
            if (raw instanceof AbilityProjectileEntity) {
                if (((AbilityProjectileEntity) raw).getThrower() == entity) continue;
            } else if (raw instanceof ProjectileEntity) {
                if (((ProjectileEntity) raw).getOwner() == entity) continue;
            }

            // Zero velocity every tick — counteracts gravity pulling it down.
            raw.setDeltaMovement(Vector3d.ZERO);

            // For mod projectiles: kill gravity and keep life counter alive
            // so the projectile doesn't naturally despawn mid-hold.
            if (raw instanceof AbilityProjectileEntity) {
                AbilityProjectileEntity proj = (AbilityProjectileEntity) raw;
                proj.setGravity(0.0F);
                if (proj.getLife() < 10) {
                    proj.setLife(20);
                }
            }

            // Spawn a TimeBubble the first time we encounter this projectile,
            // up to the MAX_FROZEN cap.
            if (!this.frozenIds.contains(raw.getId())) {
                if (this.frozenIds.size() >= MAX_FROZEN) continue;
                this.frozenIds.add(raw.getId());
                spawnBubble(entity, raw);
            }
        }
    }

    // ── End ───────────────────────────────────────────────────────────────────
    private void onEnd(LivingEntity entity, IAbility ability) {
        removeSphere();

        entity.level.playSound(
                (PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.ROOM_CLOSE_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 0.9F
        );

        if (!entity.level.isClientSide) {
            for (int id : this.frozenIds) {
                Entity proj = entity.level.getEntity(id);
                if (proj != null && proj.isAlive()) {
                    proj.remove();
                }
            }
        }

        this.frozenIds.clear();
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Spawns a TimeBubbleEntity centred on a frozen projectile and plays a
     * high-pitched trap sound audible up to 50 blocks away.
     */
    private void spawnBubble(LivingEntity caster, Entity proj) {
        TimeBubbleEntity bubble = new TimeBubbleEntity(
                KaziEntities.TIME_BUBBLE.get(), caster.level
        );
        bubble.setPos(proj.getX(), proj.getY(), proj.getZ());
        bubble.getEntityData().set(TimeBubbleEntity.TARGET_ID_PARAM,  proj.getId());
        bubble.getEntityData().set(TimeBubbleEntity.LIFE_TICKS_PARAM, BUBBLE_LIFE);
        bubble.getEntityData().set(TimeBubbleEntity.SCALE_INT_PARAM,  (int)(PROJ_BUBBLE_SCALE * 100));
        caster.level.addFreshEntity(bubble);

        // Play a high-pitched trap sound at the projectile's position.
        // Uses ServerWorld#playSound with an explicit volume so the audible
        // range (volume * 16) comfortably reaches 50 blocks.
        ((ServerWorld) caster.level).playSound(
                null,
                proj.getX(), proj.getY(), proj.getZ(),
                (SoundEvent) KaziSounds.CHRONOSTASIS_SFX.get(),
                SoundCategory.PLAYERS,
                BUBBLE_VOLUME,
                BUBBLE_PITCH
        );
    }

    private void removeSphere() {
        if (this.sphereEntity != null) {
            this.sphereEntity.remove();
            this.sphereEntity = null;
        }
    }

    // ── Time Bar helper ───────────────────────────────────────────────────────
    private static TimeBarAbility getTimeBar(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return null;
        xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData data =
                xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability
                        .getLazy(entity).orElse(null);
        if (data == null) return null;
        for (IAbility abl : data.getEquippedAndPassiveAbilities()) {
            if (abl instanceof TimeBarAbility) return (TimeBarAbility) abl;
        }
        return null;
    }

    // ── Static initialiser ────────────────────────────────────────────────────
    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Chronostasis Grigora",
                AbilityCategory.DEVIL_FRUITS,
                ChronostasisGrigoraAbility::new
        ))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ContinuousComponent.getTooltip(HOLD_TICKS),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{ SourceType.FIST })
                .build();
    }
}