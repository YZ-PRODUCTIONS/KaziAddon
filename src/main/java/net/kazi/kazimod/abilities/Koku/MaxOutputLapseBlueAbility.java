package net.kazi.kazimod.abilities.Koku;

import net.kazi.kazimod.entities.projectiles.LapseBlueProjectile;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class MaxOutputLapseBlueAbility extends Ability {

    private static final float HOLLOW_NUKE_COOLDOWN = 1800.0F;

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "max_output_lapse_blue",
            new Pair[]{ImmutablePair.of("Fires a guided Blue projectile that pulls in entities and absorbs blocks. Use the ability again to toggle the projectile between moving and stopped.", (Object) null)}
    );

    private static final float CHARGE_TIME = 20.0F;

    public static final AbilityCore<MaxOutputLapseBlueAbility> INSTANCE;

    private final ContinuousComponent chargeComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);

    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final KokuChargeVisual chargeVisual = new KokuChargeVisual();
    private ProjectileComponent projectileComponent;

    public MaxOutputLapseBlueAbility(AbilityCore<MaxOutputLapseBlueAbility> core) {
        super(core);
        this.projectileComponent = new ProjectileComponent(this, this::createProjectile);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.projectileComponent,
                this.animationComponent
        });
        this.addCanUseCheck(this::canUseCheck);
        this.addUseEvent(this::onUseEvent);
        this.addTickEvent(this::onAbilityTick);
        this.addRemoveEvent((entity, ability) -> this.chargeVisual.stop());
    }

    private AbilityUseResult canUseCheck(LivingEntity entity, IAbility ability) {
        IAbilityData data = AbilityDataCapability.get(entity);
        HollowPurpleAbility hollowPurple = (HollowPurpleAbility) data.getEquippedAbility(HollowPurpleAbility.INSTANCE);
        if (hollowPurple != null && hollowPurple.isCharging()) {
            return AbilityUseResult.fail(null);
        }
        // Shared cooldown with Lapse: Blue
        LapseBlueAbility lapseBlue = (LapseBlueAbility) data.getEquippedAbility(LapseBlueAbility.INSTANCE);
        if (lapseBlue != null && lapseBlue.isOnCooldown()) {
            return AbilityUseResult.fail(null);
        }
        return AbilityUseResult.success();
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            LapseBlueProjectile proj = LapseBlueProjectile.ACTIVE_PROJECTILES.get(entity.getUUID());
            if (proj != null && proj.isAlive()) {
                proj.toggleStopped();
                return;
            }
        }
        if (!this.chargeComponent.isContinuous()) {
            this.chargeComponent.triggerContinuity(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.animationComponent.start(entity, KaziAnimations.GOJO_BLUE);
            this.chargeVisual.start(entity, ability, net.kazi.kazimod.entities.KokuVfxEntity.BLUE_CHARGE, (int) CHARGE_TIME);
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        AbilityHelper.slowEntityFall(entity);
        this.chargeVisual.update(this.chargeComponent.getContinueTime() / CHARGE_TIME);
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.chargeVisual.stop();
        if (!entity.level.isClientSide) {
            this.projectileComponent.shoot(entity, 0.5F, 0.1F);
            this.animationComponent.start(entity, KaziAnimations.GOJO_BLUE);
        }
    }

    private void onAbilityTick(LivingEntity entity, IAbility ability) {
        LapseBlueProjectile proj = LapseBlueProjectile.ACTIVE_PROJECTILES.get(entity.getUUID());
        if (proj != null && proj.isAlive() && proj.shouldRestrictCasterMovement()) {
            AbilityHelper.slowEntityFall(entity);
            AbilityHelper.setDeltaMovement(entity, 0.0, entity.getDeltaMovement().y, 0.0);
            entity.addEffect(new net.minecraft.potion.EffectInstance(
                    (net.minecraft.potion.Effect) ModEffects.MOVEMENT_BLOCKED.get(), 5, 0, false, false));
        } else if (proj != null && proj.isAlive()) {
            // Clear the short refreshed effect as soon as the intended 100-tick
            // restriction ends instead of waiting for its remaining duration.
            entity.removeEffect(ModEffects.MOVEMENT_BLOCKED.get());
        }
    }

    private LapseBlueProjectile createProjectile(LivingEntity entity) {
        return new LapseBlueProjectile(entity.level, entity, this);
    }

    /** Expose cooldown state so LapseBlueAbility can check it. */
    public boolean isOnCooldown() {
        return this.cooldownComponent.isOnCooldown();
    }

    public void startCooldown(PlayerEntity entity) {
        this.cooldownComponent.startCooldown(entity, 700.0F);
    }

    public static void startHollowNukeCooldown(LivingEntity entity) {
        IAbilityData data = AbilityDataCapability.get(entity);
        MaxOutputLapseBlueAbility blue = (MaxOutputLapseBlueAbility) data.getEquippedAbility(INSTANCE);
        if (blue != null) {
            blue.cooldownComponent.startCooldown(entity, HOLLOW_NUKE_COOLDOWN);
        }
    }

    public static final ResourceLocation MAX_OUTPUT_BLUE_ICON =
            new ResourceLocation("kazimod", "textures/abilities/max_output_lapse_blue.png");

    static {
        INSTANCE = (new AbilityCore.Builder<>("Maximum Output: Lapse Blue", AbilityCategory.DEVIL_FRUITS, MaxOutputLapseBlueAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(CHARGE_TIME),
                        CooldownComponent.getTooltip(700.0F)
                })
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INDIRECT})
                .setIcon(MAX_OUTPUT_BLUE_ICON)
                .build();
    }
}
