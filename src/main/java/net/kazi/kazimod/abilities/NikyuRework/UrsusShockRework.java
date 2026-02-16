//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.NikyuRework;

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
import xyz.pixelatedw.mineminenomi.entities.projectiles.nikyu.ChargingUrsusShockEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.nikyu.UrsusShockProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class UrsusShockRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "ursus_shock", new Pair[]{ImmutablePair.of("The user compresses air and sends it towards the opponent to create a huge shockwave", (Object)null)});
    private static final int COOLDOWN = 400;
    private static final int CHARGE_TIME = 140;
    public static final AbilityCore<UrsusShockRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this, (comp) -> comp.getChargePercentage() >= 0.5F)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private ChargingUrsusShockEntity ursusShockEntity;

    public UrsusShockRework(AbilityCore<UrsusShockRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.animationComponent, this.projectileComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.startCharging(entity, 140.0F);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.RAISE_ARMS, 140);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.URSUS_SHOCK_SFX.get(), SoundCategory.PLAYERS, 5.0F, 0.75F);
        ChargingUrsusShockEntity chargingUrsusShock = new ChargingUrsusShockEntity(entity.level);
        chargingUrsusShock.setOwner(entity);
        chargingUrsusShock.setPos(entity.getX(), entity.getY() + (double)2.0F, entity.getZ());
        entity.level.addFreshEntity(chargingUrsusShock);
        this.ursusShockEntity = chargingUrsusShock;
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.ursusShockEntity == null) {
            this.chargeComponent.forceStopCharging(entity);
        } else {
            boolean atThreshold = (double)this.chargeComponent.getChargePercentage() < 0.4;
            float currentCharge = this.ursusShockEntity.getCharge();
            currentCharge = (float)((double)currentCharge + (atThreshold ? 0.065 : -0.055));
            currentCharge = MathHelper.clamp(currentCharge, -1.4F, 10.0F);
            this.ursusShockEntity.setCharge(currentCharge);
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        float multiplier = this.chargeComponent.getChargePercentage();
        if (this.ursusShockEntity != null) {
            UrsusShockProjectile projectile = new UrsusShockProjectile(entity.level, entity);
            projectile.multiplier = multiplier;
            projectile.setSize((double)multiplier > (double)0.75F ? 0.6F : 5.0F * (1.0F - multiplier));
            entity.level.addFreshEntity(projectile);
            projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 2.0F, 0.0F);
        }

        this.ursusShockEntity.remove();
        this.cooldownComponent.startCooldown(entity, 400.0F * multiplier);
    }

    private UrsusShockProjectile createProjectile(LivingEntity entity) {
        UrsusShockProjectile proj = new UrsusShockProjectile(entity.level, entity);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Ursus Shock", AbilityCategory.DEVIL_FRUITS, UrsusShockRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(400.0F)}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceElement(SourceElement.AIR).setSourceType(new SourceType[]{SourceType.PROJECTILE, SourceType.INTERNAL}).build();
    }
}
