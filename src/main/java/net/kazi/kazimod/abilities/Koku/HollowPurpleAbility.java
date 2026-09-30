package net.kazi.kazimod.abilities.Koku;

import net.kazi.kazimod.abilities.GomuRework.GearFifthRework;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class HollowPurpleAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "hollow_purple",
            new Pair[]{ImmutablePair.of("Combines Red and Blue into a single imaginary mass, firing it as a massive purple projectile that destroys everything in its path.", (Object) null)}
    );

    private static final float CHARGE_TIME  = 140.0F;
    private static final float COOLDOWN     = 1800.0F;

    public static final AbilityCore<HollowPurpleAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this, (comp) -> comp.getChargePercentage() >= 1.0F))
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);

    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private ProjectileComponent projectileComponent;

    private final Interval particleInterval = new Interval(2);
    private final KokuChargeVisual chargeVisual = new KokuChargeVisual();
    private boolean fireAnimTriggered = false;
    private boolean chantTriggered    = false;

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
        this.addRemoveEvent((entity, ability) -> this.chargeVisual.stop());
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
        if (!entity.level.isClientSide) {
            // If a projectile is already live, detonate it instead of charging again
            HollowPurpleProjectile proj =
                    HollowPurpleProjectile.ACTIVE_PROJECTILES.get(entity.getUUID());
            if (proj != null && proj.isAlive()) {
                proj.detonate();
                return;
            }
        }
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        // No launch — just start the animation and reset flags
        this.animationComponent.start(entity, KaziAnimations.GOJO_HOLLOW_PURPLE);
        this.chargeVisual.start(entity, ability, net.kazi.kazimod.entities.KokuVfxEntity.PURPLE_CHARGE, (int) CHARGE_TIME);
        fireAnimTriggered = false;
        chantTriggered    = false;
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        AbilityHelper.slowEntityFall(entity);
        this.chargeVisual.update(this.chargeComponent.getChargePercentage());
        if (!entity.level.isClientSide && particleInterval.canTick()) {
            float progress = this.chargeComponent.getChargePercentage();
            if (progress >= 0.7F) {
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
                }
            }
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.chargeVisual.stop();
        net.kazi.kazimod.entities.KokuVfxEntity.impact(entity.level,
                net.kazi.kazimod.entities.KokuVfxEntity.castOrigin(entity, 1.0F, 2),
                net.kazi.kazimod.entities.KokuVfxEntity.PURPLE_IMPACT, 4.0F);
        HollowPurpleProjectile projectile = new HollowPurpleProjectile(entity.level, entity, this);
        projectile.moveTo(entity.getX(), entity.getY() + 8.0, entity.getZ());
        entity.level.addFreshEntity(projectile);
        projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 4.5F, 0.0F);
        // Cooldown starts when the projectile detonates, not when fired
        this.animationComponent.stop(entity);
        fireAnimTriggered = false;
        chantTriggered    = false;
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

    public static void triggerCooldownForEntity(LivingEntity entity) {
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) return;
        HollowPurpleAbility ability = (HollowPurpleAbility) data.getEquippedAbility(INSTANCE);
        if (ability == null) return;
        ability.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    public boolean isOnCooldown(LivingEntity entity) {
        return this.cooldownComponent.isOnCooldown();
    }

    public boolean isCharging() {
        return this.chargeComponent.isCharging();
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
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
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INDIRECT, SourceType.PROJECTILE})
                .setUnlockCheck(HollowPurpleAbility::canUnlock)
                .build();
    }
}
