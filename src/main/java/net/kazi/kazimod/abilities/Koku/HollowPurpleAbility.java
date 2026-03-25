package net.kazi.kazimod.abilities.Gojo;

import net.kazi.kazimod.animations.gojo.GojoHollowPurpleAnimation;
import net.kazi.kazimod.entities.projectiles.HollowPurpleProjectile;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class HollowPurpleAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "hollow_purple",
            new Pair[]{ImmutablePair.of("Combines Red and Blue into a single imaginary mass, firing it as a massive purple projectile that destroys everything in its path.", (Object) null)}
    );

    private static final float CHARGE_TIME  = 140.0F;
    private static final float COOLDOWN     = 1800.0F;
    private static final float LAUNCH_HEIGHT = 3.0F;

    public static final AbilityCore<HollowPurpleAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this, (comp) -> comp.getChargePercentage() >= 1.0F))
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);

    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private ProjectileComponent projectileComponent;

    private final Interval particleInterval = new Interval(2);
    private boolean fireAnimTriggered  = false;
    private boolean chantTriggered     = false; // tracks whether purplechant has played this charge

    public HollowPurpleAbility(AbilityCore<HollowPurpleAbility> core) {
        super(core);
        this.projectileComponent = new ProjectileComponent(this, this::createProjectile);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.animationComponent,
                this.projectileComponent
        });
        this.addCanUseCheck(this::domainCheck);
        this.addUseEvent(this::onUseEvent);
    }

    private AbilityUseResult domainCheck(LivingEntity entity, IAbility ability) {
        IAbilityData data = AbilityDataCapability.get(entity);
        DomainExpansionInfiniteVoidAbility domain =
                (DomainExpansionInfiniteVoidAbility) data.getEquippedAbility(
                        DomainExpansionInfiniteVoidAbility.INSTANCE);
        if (domain != null && domain.isDomainActive()) {
            return AbilityUseResult.fail(null);
        }
        return AbilityUseResult.success();
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        AbilityHelper.setDeltaMovement(entity,
                entity.getDeltaMovement().x,
                (double) LAUNCH_HEIGHT,
                entity.getDeltaMovement().z);
        this.animationComponent.start(entity, KaziAnimations.GOJO_HOLLOW_PURPLE);
        fireAnimTriggered = false;
        chantTriggered    = false;
        if (GojoHollowPurpleAnimation.INSTANCE != null) {
            GojoHollowPurpleAnimation.INSTANCE.reset();
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        AbilityHelper.slowEntityFall(entity);

        if (!entity.level.isClientSide && particleInterval.canTick()) {
            float progress = this.chargeComponent.getChargePercentage();

            Vector3d look  = entity.getLookAngle().normalize();
            Vector3d right = new Vector3d(-look.z, 0, look.x);

            if (progress < 0.7F) {
                Vector3d redPos = entity.position().add(0, 1.5, 0).add(right.scale(-2.5));
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_RED.get(),
                        entity, redPos.x, redPos.y, redPos.z
                );

                Vector3d bluePos = entity.position().add(0, 1.5, 0).add(right.scale(2.5));
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_BLUE.get(),
                        entity, bluePos.x, bluePos.y, bluePos.z
                );

            } else {
                float  mergeProgress = (progress - 0.7F) / 0.3F;
                double separation    = 2.5 * (1.0F - mergeProgress);

                Vector3d redPos = entity.position().add(0, 1.5, 0).add(right.scale(-separation));
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_RED.get(),
                        entity, redPos.x, redPos.y, redPos.z
                );

                Vector3d bluePos = entity.position().add(0, 1.5, 0).add(right.scale(separation));
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_BLUE.get(),
                        entity, bluePos.x, bluePos.y, bluePos.z
                );

                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_PURPLE.get(),
                        entity,
                        entity.getX(), entity.getY() + 1.5, entity.getZ()
                );

                // Play purplechant exactly once when the purple particle first appears
                if (!chantTriggered) {
                    chantTriggered = true;
                    entity.level.playSound(
                            null,
                            entity.blockPosition(),
                            KaziSounds.PURPLE_CHANT_SFX.get(),
                            SoundCategory.PLAYERS,
                            1.0F, 1.0F
                    );
                }

                if (!fireAnimTriggered) {
                    fireAnimTriggered = true;
                    if (GojoHollowPurpleAnimation.INSTANCE != null) {
                        GojoHollowPurpleAnimation.INSTANCE.triggerFire();
                    }
                }
            }
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        HollowPurpleProjectile projectile = new HollowPurpleProjectile(entity.level, entity, this);
        projectile.moveTo(entity.getX(), entity.getY() + 8.0, entity.getZ());
        entity.level.addFreshEntity(projectile);
        projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 4.5F, 0.0F);
        super.cooldownComponent.startCooldown(entity, COOLDOWN);
        this.animationComponent.stop(entity);
        fireAnimTriggered = false;
        chantTriggered    = false;
        if (GojoHollowPurpleAnimation.INSTANCE != null) {
            GojoHollowPurpleAnimation.INSTANCE.reset();
        }
    }

    private HollowPurpleProjectile createProjectile(LivingEntity entity) {
        return new HollowPurpleProjectile(entity.level, entity, this);
    }

    public static void startCooldownFromOutside(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            IAbilityData data = AbilityDataCapability.get(entity);
            HollowPurpleAbility ability =
                    (HollowPurpleAbility) data.getEquippedAbility(INSTANCE);
            if (ability != null) {
                ability.cooldownComponent.startCooldown(entity, COOLDOWN);
            }
        }
    }

    public boolean isOnCooldown(LivingEntity entity) {
        return this.cooldownComponent.isOnCooldown();
    }

    public boolean isCharging() {
        return this.chargeComponent.isCharging();
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Hollow Purple",
                AbilityCategory.DEVIL_FRUITS, HollowPurpleAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceType(new SourceType[]{SourceType.INDIRECT, SourceType.PROJECTILE})
                .build();
    }
}