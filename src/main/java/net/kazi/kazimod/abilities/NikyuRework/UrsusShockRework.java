package net.kazi.kazimod.abilities.NikyuRework;

import net.kazi.kazimod.entities.projectiles.UrsusShockReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
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
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.nikyu.ChargingUrsusShockEntity;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class UrsusShockRework extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "mineminenomi", "ursus_shock",
            new Pair[]{ImmutablePair.of(
                    "The user compresses air and sends it towards the opponent. " +
                            "Use the ability again while the projectile is in flight to detonate it.",
                    (Object) null)});

    private static final int COOLDOWN    = 500;
    private static final int CHARGE_TIME = 140;

    public static final AbilityCore<UrsusShockRework> INSTANCE;

    private final ChargeComponent chargeComponent = (new ChargeComponent(this,
            (comp) -> comp.getChargePercentage() >= 0.5F))
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::duringChargeEvent)
            .addEndEvent(this::endChargeEvent);

    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private ChargingUrsusShockEntity ursusShockEntity;

    // Stores the charge multiplier so the projectile's cooldown callback can use it
    private float lastMultiplier = 1.0F;

    public UrsusShockRework(AbilityCore<UrsusShockRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent, this.animationComponent, this.projectileComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            // If a projectile is already live, detonate it instead of charging again
            UrsusShockReworkProjectile proj =
                    UrsusShockReworkProjectile.ACTIVE_PROJECTILES.get(entity.getUUID());
            if (proj != null && proj.isAlive() && !proj.isFinished()) {
                proj.detonate();
                return;
            }
        }
        this.chargeComponent.startCharging(entity, CHARGE_TIME);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.RAISE_ARMS, CHARGE_TIME);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.URSUS_SHOCK_SFX.get(), SoundCategory.PLAYERS, 5.0F, 0.75F);

        ChargingUrsusShockEntity chargingUrsusShock = new ChargingUrsusShockEntity(entity.level);
        chargingUrsusShock.setOwner(entity);
        chargingUrsusShock.setPos(entity.getX(), entity.getY() + 2.0, entity.getZ());
        entity.level.addFreshEntity(chargingUrsusShock);
        this.ursusShockEntity = chargingUrsusShock;
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.ursusShockEntity == null) {
            this.chargeComponent.forceStopCharging(entity);
        } else {
            boolean atThreshold = this.chargeComponent.getChargePercentage() < 0.4;
            float currentCharge = this.ursusShockEntity.getCharge();
            currentCharge += atThreshold ? 0.065f : -0.055f;
            currentCharge = MathHelper.clamp(currentCharge, -1.4F, 10.0F);
            this.ursusShockEntity.setCharge(currentCharge);
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        float multiplier = this.chargeComponent.getChargePercentage();
        this.lastMultiplier = multiplier;

        if (this.ursusShockEntity != null) {
            UrsusShockReworkProjectile projectile = new UrsusShockReworkProjectile(entity.level, entity);
            projectile.multiplier = multiplier;
            projectile.setSize(multiplier > 0.75F ? 0.6F : 5.0F * (1.0F - multiplier));
            entity.level.addFreshEntity(projectile);
            projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 2.0F, 0.0F);
            this.ursusShockEntity.remove();
        }

        // Cooldown is NOT started here — it fires when the projectile detonates
    }

    private UrsusShockReworkProjectile createProjectile(LivingEntity entity) {
        return new UrsusShockReworkProjectile(entity.level, entity);
    }

    /**
     * Called by UrsusShockReworkProjectile when it detonates,
     * so the cooldown only starts after the explosion.
     */
    public static void triggerCooldownForEntity(LivingEntity entity) {
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) return;
        UrsusShockRework ability = (UrsusShockRework) data.getEquippedAbility(INSTANCE);
        if (ability == null) return;
        ability.cooldownComponent.startCooldown(entity, 500.0F); // 25 seconds fixed
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Ursus Shock", AbilityCategory.DEVIL_FRUITS, UrsusShockRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceElement(SourceElement.AIR)
                .setSourceType(new SourceType[]{SourceType.PROJECTILE, SourceType.INTERNAL})
                .build();
    }
}