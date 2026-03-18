package net.kazi.kazimod.abilities.KachiRework;

import net.kazi.kazimod.entities.projectiles.CruelSunProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SwingTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class CruelSunAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "cruel_sun",
            new Pair[]{ ImmutablePair.of(
                    "At the peak of Sunshine's power, you compress power into a miniature sun. Requires 6 or more Sunshine points.",
                    (Object) null) }
    );

    private static final float CHARGE_TIME = 100.0F;
    private static final float HOLD_TIME   = 160.0F;
    private static final float COOLDOWN    = 500.0F;
    private static final int   MIN_POINTS  = 6;

    public static final AbilityCore<CruelSunAbility> INSTANCE;

    // ── Components — continuousComponent declared first so MMNM HUD renders correctly ──

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addTickEvent(this::onContinuityTick)
                    .addEndEvent(this::onContinuityEnd);

    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);

    private final SwingTriggerComponent swingTriggerComponent =
            (new SwingTriggerComponent(this))
                    .addSwingEvent(this::onSwing);

    private final AnimationComponent animationComponent = new AnimationComponent(this);

    private final ProjectileComponent projectileComponent =
            new ProjectileComponent(this, this::createProjectile);

    private final Interval particleInterval = new Interval(2);

    private CruelSunProjectile sunProjectile = null;

    // ── Constructor ───────────────────────────────────────────────────────────

    public CruelSunAbility(AbilityCore<CruelSunAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.chargeComponent,
                this.swingTriggerComponent,
                this.animationComponent,
                this.projectileComponent
        });
        this.addCanUseCheck(this::canUseCheck);
        this.addUseEvent(this::onUseEvent);
    }

    // ── Can-use guard ─────────────────────────────────────────────────────────

    private AbilityUseResult canUseCheck(LivingEntity entity, IAbility ability) {
        IAbilityData data = AbilityDataCapability.get(entity);
        SunshineAbility sunshine = (SunshineAbility) data.getPassiveAbility(SunshineAbility.INSTANCE);
        if (sunshine == null || sunshine.getCurrentPoints() < MIN_POINTS) {
            return AbilityUseResult.fail(null);
        }
        return AbilityUseResult.success();
    }

    // ── Use event — mirrors Entei exactly ─────────────────────────────────────

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
                this.chargeComponent.startCharging(entity, CHARGE_TIME);
            } else if (sunProjectile != null && sunProjectile.isAlive()) {
                sunProjectile.onBlockImpactEvent(sunProjectile.blockPosition());
                sunProjectile.remove();
            }
        }
    }

    // ── Charge start ──────────────────────────────────────────────────────────

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.particleInterval.restartIntervalToZero();
        this.animationComponent.start(entity, ModAnimations.RAISE_RIGHT_ARM);
        this.sunProjectile = (CruelSunProjectile) this.projectileComponent.getNewProjectile(entity);
        this.sunProjectile.setPos(
                entity.getX(),
                entity.getY() + entity.getEyeHeight() + 7.5,
                entity.getZ());
        entity.level.addFreshEntity(this.sunProjectile);
    }

    // ── Charge tick ───────────────────────────────────────────────────────────

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (sunProjectile == null || !sunProjectile.isAlive()) {
            this.chargeComponent.stopCharging(entity);
            return;
        }
        sunProjectile.setLife(sunProjectile.getMaxLife());
        sunProjectile.increaseSize();
        sunProjectile.setPos(
                entity.getX(),
                entity.getY() + entity.getEyeHeight() + 7.5,
                entity.getZ());
        if (!entity.level.isClientSide && this.particleInterval.canTick()) {
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) ModParticleEffects.DAI_ENKAI_2.get(),
                    entity, entity.getX(), entity.getY(), entity.getZ());
        }
    }

    // ── Charge end ────────────────────────────────────────────────────────────

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, HOLD_TIME);
    }

    // ── Swing to fire ─────────────────────────────────────────────────────────

    private void onSwing(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) return;
        sunProjectile.shootFromRotation(entity, entity.xRot + 10.0F, entity.yRot, 0.0F, 3.0F, 1.0F);
        entity.level.playSound(
                (PlayerEntity) null,
                entity.blockPosition(),
                ModSounds.MERA_SFX.get(),
                SoundCategory.PLAYERS,
                5.0F, 1.0F);
        if (!entity.level.isClientSide) {
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) ModParticleEffects.DAI_ENKAI_1.get(),
                    entity, entity.getX(), entity.getY(), entity.getZ());
        }
        this.continuousComponent.stopContinuity(entity);
    }

    // ── Continuity tick ───────────────────────────────────────────────────────

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (sunProjectile != null && sunProjectile.isAlive()) {
            sunProjectile.setLife(sunProjectile.getMaxLife());
            sunProjectile.setPos(
                    entity.getX(),
                    entity.getY() + entity.getEyeHeight() + 7.5,
                    entity.getZ());
        } else {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    // ── Continuity end ────────────────────────────────────────────────────────

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (sunProjectile != null && sunProjectile.isAlive()
                && sunProjectile.getLife() < sunProjectile.getMaxLife()) {
            sunProjectile.onBlockImpactEvent(sunProjectile.blockPosition());
            sunProjectile.remove();
        }
        sunProjectile = null;
        this.animationComponent.stop(entity);
        super.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    // ── Projectile factory ────────────────────────────────────────────────────

    private CruelSunProjectile createProjectile(LivingEntity entity) {
        return new CruelSunProjectile(entity.level, entity, this);
    }

    // ── Static registration ───────────────────────────────────────────────────

    static {
        INSTANCE = (new AbilityCore.Builder<>("Cruel Sun",
                AbilityCategory.DEVIL_FRUITS, CruelSunAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(HOLD_TIME),
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.FIRE)
                .setSourceType(new SourceType[]{ SourceType.INDIRECT, SourceType.PROJECTILE })
                .build();
    }
}