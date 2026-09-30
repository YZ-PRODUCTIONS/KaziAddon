package net.kazi.kazimod.abilities.YamiRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.projectiles.yami.DarkMatterProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class DarkMatterRework extends Ability {
    private static final int COOLDOWN = 280;
    private static final int CHARGE_TIME = 80;
    private static final int HOLD_TIME = 80;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "dark_matter_rework",
            new Pair[]{ImmutablePair.of(
                    "Launches a ball of darkness that engulfs the opponent.", null)});

    public static final AbilityCore<DarkMatterRework> INSTANCE;

    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::duringChargeEvent)
            .addEndEvent(this::endChargeEvent);
    private final ProjectileComponent projectileComponent =
            new ProjectileComponent(this, this::createProjectile);
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addTickEvent(this::duringHoldEvent)
            .addEndEvent(this::endHoldEvent);
    private final SwingTriggerComponent swingTriggerComponent = new SwingTriggerComponent(this)
            .addSwingEvent(this::onSwingEvent);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final Interval particleInterval = new Interval(2);

    private DarkMatterProjectile projectile;
    private boolean launched;

    public DarkMatterRework(AbilityCore<DarkMatterRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(chargeComponent, projectileComponent, continuousComponent,
                swingTriggerComponent, animationComponent);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        chargeComponent.startCharging(entity, CHARGE_TIME);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        projectile = null;
        launched = false;
        particleInterval.restartIntervalToZero();
        animationComponent.start(entity, ModAnimations.RAISE_RIGHT_ARM);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (projectile == null) {
                projectile = (DarkMatterProjectile) projectileComponent.getNewProjectile(entity);
                entity.level.addFreshEntity(projectile);
            } else if (!projectile.isAlive()) {
                chargeComponent.stopCharging(entity);
                return;
            }
            holdAboveOwner(entity);
        }
        spawnChargingParticles(entity);
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && (projectile == null || !projectile.isAlive())) {
            animationComponent.stop(entity);
            cooldownComponent.startCooldown(entity, COOLDOWN);
            return;
        }
        continuousComponent.startContinuity(entity, HOLD_TIME);
    }

    private void duringHoldEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (projectile == null || !projectile.isAlive()) {
                continuousComponent.stopContinuity(entity);
                return;
            }
            holdAboveOwner(entity);
        }
        spawnChargingParticles(entity);
    }

    private void onSwingEvent(LivingEntity entity, IAbility ability) {
        if (!continuousComponent.isContinuous()) return;

        if (!entity.level.isClientSide && projectile != null && projectile.isAlive()) {
            launched = true;
            projectile.shootFromRotation(
                    entity, entity.xRot + 10.0F, entity.yRot, 0.0F, 3.0F, 1.0F);
        }
        continuousComponent.stopContinuity(entity);
    }

    private void endHoldEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && !launched && projectile != null && projectile.isAlive()) {
            projectile.remove();
        }
        projectile = null;
        launched = false;
        animationComponent.stop(entity);
        cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private void holdAboveOwner(LivingEntity entity) {
        projectile.setLife(projectile.getMaxLife());
        projectile.setPos(entity.getX(), entity.getEyeY() + 3.0D, entity.getZ());
        AbilityHelper.setDeltaMovement(projectile, 0.0D, 0.0D, 0.0D);
    }

    private void spawnChargingParticles(LivingEntity entity) {
        if (particleInterval.canTick()) {
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) ModParticleEffects.DARK_MATTER_CHARGING.get(),
                    entity, entity.getX(), entity.getY(), entity.getZ());
        }
    }

    private DarkMatterProjectile createProjectile(LivingEntity entity) {
        return new DarkMatterProjectile(entity.level, entity, this);
    }

    static {
        INSTANCE = new AbilityCore.Builder(
                "Dark Matter Rework", AbilityCategory.DEVIL_FRUITS, DarkMatterRework::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(HOLD_TIME))
                .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.GRAVITY)
                .build();
    }
}
