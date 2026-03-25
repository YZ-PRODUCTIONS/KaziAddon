package net.kazi.kazimod.abilities.boss.luffy;

import net.kazi.kazimod.entities.boss.BossAimHelper;
import net.kazi.kazimod.entities.projectiles.GomuGomuNoKaminariProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

import java.awt.Color;

/**
 * Boss-specific version of GomuGomuNoKaminariAbility.
 *
 * Differences from the player version:
 * - NO vertical launch: the player version sends the boss 4 blocks into the air
 *   making aim unreliable. This version fires from the ground/current position.
 * - Aim is locked to target BEFORE each beam tick via aimAtTarget(), ensuring
 *   LightningEntity and projectileComponent always shoot toward the player.
 * - Shorter charge (30t) and beam duration (60t) to feel responsive as a boss move.
 * - No animation component (NPC animation system differs from player).
 */
public class BossKaminariAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "boss_kaminari",
            new Pair[]{ImmutablePair.of(
                    "Boss version of Gomu Gomu no Kaminari. Fires a sustained lightning beam.", (Object) null)}
    );

    private static final int   COOLDOWN      = 600;
    private static final float CHARGE_TIME   = 30.0F;   // shorter than player (60t)
    private static final float BEAM_DURATION = 60.0F;   // shorter than player (80t)
    private static final float RANGE         = 90.0F;
    private static final float DAMAGE        = 14.0F;
    private static final float BEAM_SIZE     = 0.45F;
    private static final double PROJ_SPEED   = 4.0;

    public static final AbilityCore<BossKaminariAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::startChargingEvent)
                    .addEndEvent(this::endChargingEvent);

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addTickEvent(this::tickBeamEvent)
                    .addEndEvent(this::endBeamEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent      rangeComponent      = new RangeComponent(this);
    private final ProjectileComponent projectileComponent =
            new ProjectileComponent(this, this::createProjectile);

    private LightningEntity boltInner;
    private LightningEntity boltOuter;

    private final Interval damageInterval  = new Interval(10);
    private final Interval particleInterval = new Interval(4);

    public BossKaminariAbility(AbilityCore<BossKaminariAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.continuousComponent,
                this.dealDamageComponent,
                this.rangeComponent,
                this.projectileComponent
        });
        this.addUseEvent(this::useEvent);
    }

    public ChargeComponent     getChargeComponent()     { return this.chargeComponent; }
    public ContinuousComponent getContinuousComponent() { return this.continuousComponent; }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void startChargingEvent(LivingEntity entity, IAbility ability) {
        // NO vertical launch — boss stays grounded for accurate aim
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS,
                1.5F, 0.6F + entity.getRandom().nextFloat() * 0.4F);
    }

    private void endChargingEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, BEAM_DURATION);
        if (!entity.level.isClientSide) {
            ((ServerWorld) entity.level).getChunkSource()
                    .broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }
    }

    private void tickBeamEvent(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        // Re-aim at target every tick so beam tracks the player
        aimAtTarget(entity);

        this.projectileComponent.shoot(entity, (float) PROJ_SPEED, 3.0F);

        if (this.boltOuter == null) {
            spawnBeam(entity);
        } else {
            // Update beam direction to follow our updated rotation
            this.boltInner.moveTo(entity.getX(), entity.getEyeY(), entity.getZ(),
                    entity.yRot, entity.xRot);
            this.boltOuter.moveTo(entity.getX(), entity.getEyeY(), entity.getZ(),
                    entity.yRot, entity.xRot);
        }

        if (this.damageInterval.canTick()) {
            for (LivingEntity target : this.rangeComponent.getTargetsInLine(entity, RANGE, 3.5F)) {
                boolean hit = this.dealDamageComponent.hurtTarget(entity, target, DAMAGE);
                if (hit) {
                    target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 40, 2));
                }
            }
        }
    }

    /**
     * Force boss rotation to aim at its current target using lead-prediction.
     * Called before every projectile/beam tick so the boss always shoots at the player.
     */
    private static void aimAtTarget(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.entity.MobEntity)) return;
        LivingEntity target = ((net.minecraft.entity.MobEntity) entity).getTarget();
        if (target == null || !target.isAlive()) return;

        Vector3d dir = BossAimHelper.leadTarget(entity, target, PROJ_SPEED);
        float yaw   = (float)(Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.asin(dir.y)));

        entity.yRot     = yaw;
        entity.yHeadRot = yaw;
        entity.yBodyRot = yaw;
        entity.xRot     = pitch;
    }

    private void spawnBeam(LivingEntity entity) {
        int segments = (int)(RANGE * 0.6F);

        this.boltInner = new LightningEntity(entity,
                entity.getX(), entity.getEyeY(), entity.getZ(),
                entity.yRot, entity.xRot, RANGE, 20.0F, this.getCore());
        this.boltInner.setSize(BEAM_SIZE * 0.75F);
        this.boltInner.setColor(new Color(200, 230, 255));
        this.boltInner.setDamage(0.0F);
        this.boltInner.setSegments(segments);
        this.boltInner.setBranches(2);
        this.boltInner.setAngle(85);
        this.boltInner.setBoxSizeDivision(0.22);
        this.boltInner.setCollideWithEntities(false);
        this.boltInner.setLightningMimic(false);
        this.boltInner.setMaxLife(120);

        this.boltOuter = new LightningEntity(entity,
                entity.getX(), entity.getEyeY(), entity.getZ(),
                entity.yRot, entity.xRot, RANGE, 20.0F, this.getCore());
        this.boltOuter.setSize(BEAM_SIZE);
        this.boltOuter.setColor(new Color(30, 144, 255));
        this.boltOuter.setDamage(DAMAGE);
        this.boltOuter.setSegments(segments + 4);
        this.boltOuter.setBranches(4);
        this.boltOuter.setAngle(100);
        this.boltOuter.setBoxSizeDivision(0.22);
        this.boltOuter.setExplosion(3, true, 0.3F);
        this.boltOuter.disableExplosionKnockback();
        this.boltOuter.setCollideWithEntities(false);
        this.boltOuter.setLightningMimic(false);
        this.boltOuter.setMaxLife(120);
        this.boltOuter.seed = this.boltInner.seed;

        entity.level.addFreshEntity(this.boltInner);
        entity.level.addFreshEntity(this.boltOuter);
    }

    private void endBeamEvent(LivingEntity entity, IAbility ability) {
        if (this.boltInner != null) { this.boltInner.remove(); this.boltInner = null; }
        if (this.boltOuter != null) { this.boltOuter.remove(); this.boltOuter = null; }
        this.cooldownComponent.startCooldown(entity, (float) COOLDOWN);
    }

    private GomuGomuNoKaminariProjectile createProjectile(LivingEntity entity) {
        return new GomuGomuNoKaminariProjectile(entity.level, entity);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Gomu Gomu no Kaminari (Boss)",
                AbilityCategory.DEVIL_FRUITS, BossKaminariAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        DealDamageComponent.getTooltip(DAMAGE, DAMAGE * 2),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(BEAM_DURATION),
                        CooldownComponent.getTooltip((float) COOLDOWN),
                        RangeComponent.getTooltip(RANGE, RangeType.LINE)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceElement(SourceElement.LIGHTNING)
                .build();
    }
}