//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KitsuneRework;

import net.kazi.kazimod.entities.projectiles.FoxfireBallReworkProjectile;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SwingTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class FoxfireBallRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "foxfire_ball", new Pair[]{ImmutablePair.of("The user fires a ball of blue flames at the opponent that erupt into an explosion", (Object)null)});
    private static final float COOLDOWN = 900.0F;
    private static final float CHARGE_TIME = 75.0F;
    private static final float HOLD_TIME = 150.0F;
    private static final float startHeight = 10.5F;
    public static AbilityCore<FoxfireBallRework> INSTANCE;
    private final ChargeComponent chargeComponent;
    private final ContinuousComponent continuousComponent;
    private final SwingTriggerComponent swingTriggerComponent;
    private final AnimationComponent animationComponent;
    private final ProjectileComponent projectileComponent;
    private FoxfireBallReworkProjectile FoxfireBallReworkProjectile;
    private final RequireMorphComponent requireMorphComponent;

    public FoxfireBallRework(AbilityCore<FoxfireBallRework> core) {
        super(core);
        this.requireMorphComponent = new RequireMorphComponent(this, (MorphInfo)CartMorphs.KITSUNE_HYBRID.get(), new MorphInfo[]{(MorphInfo)CartMorphs.KITSUNE_WALK.get()});
        this.chargeComponent = (new ChargeComponent(this)).addStartEvent(this::onChargeStart).addTickEvent(this::onChargeTick).addEndEvent(this::onChargeEnd);
        this.continuousComponent = (new ContinuousComponent(this, true)).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
        this.swingTriggerComponent = (new SwingTriggerComponent(this)).addSwingEvent(this::onSwing);
        this.animationComponent = new AnimationComponent(this);
        this.projectileComponent = new ProjectileComponent(this, this::createProjectile);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.requireMorphComponent, this.continuousComponent, this.chargeComponent, this.swingTriggerComponent, this.animationComponent, this.projectileComponent});
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && !this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, 75.0F);
        }

    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.FoxfireBallReworkProjectile = (FoxfireBallReworkProjectile)this.projectileComponent.getNewProjectile(entity);
        this.FoxfireBallReworkProjectile.setPos(entity.getX(), entity.getY() + (double)entity.getEyeHeight() + (double)5.0F, entity.getZ());
        entity.level.addFreshEntity(this.FoxfireBallReworkProjectile);
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (this.FoxfireBallReworkProjectile == null || !this.FoxfireBallReworkProjectile.isAlive()) {
            this.chargeComponent.stopCharging(entity);
            return;
        }

        this.animationComponent.start(entity, ModAnimations.RAISE_RIGHT_ARM);
        this.FoxfireBallReworkProjectile.setLife(this.FoxfireBallReworkProjectile.getMaxLife());
        this.FoxfireBallReworkProjectile.increaseSize();
        this.FoxfireBallReworkProjectile.setPos(entity.getX(), entity.getY() + (double)entity.getEyeHeight() + (double)5.0F, entity.getZ());
        this.FoxfireBallReworkProjectile.setDeltaMovement(0.0, 0.0, 0.0);
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, 150.0F);
    }

    private void onSwing(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.FoxfireBallReworkProjectile.setLaunched(true); // Enable tracking
            this.FoxfireBallReworkProjectile.shootFromRotation(entity, entity.xRot + 0.0F, entity.yRot, 0.0F, 3.0F, 1.0F);
            this.continuousComponent.stopContinuity(entity);
        }

    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (this.FoxfireBallReworkProjectile != null && this.FoxfireBallReworkProjectile.isAlive()) {
            this.FoxfireBallReworkProjectile.setDamage(80.0F);
            this.FoxfireBallReworkProjectile.setLife(this.FoxfireBallReworkProjectile.getMaxLife());
            this.FoxfireBallReworkProjectile.setPos(entity.getX(), entity.getY() + (double)entity.getEyeHeight() + (double)5.0F, entity.getZ());
            this.FoxfireBallReworkProjectile.setDeltaMovement(0.0, 0.0, 0.0);
        } else {
            this.continuousComponent.stopContinuity(entity);
        }

        entity.addEffect(new EffectInstance(Effects.FIRE_RESISTANCE, 5, 5, false, false));
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.FoxfireBallReworkProjectile != null && this.FoxfireBallReworkProjectile.isAlive() && this.FoxfireBallReworkProjectile.getLife() < this.FoxfireBallReworkProjectile.getMaxLife()) {
                this.FoxfireBallReworkProjectile.setLaunched(true); // Enable tracking
                this.FoxfireBallReworkProjectile.shootFromRotation(entity, entity.xRot + 10.0F, entity.yRot, 0.0F, 3.0F, 1.0F);
            }

            this.FoxfireBallReworkProjectile = null;
            this.animationComponent.stop(entity);
            super.cooldownComponent.startCooldown(entity, 400.0F);
        }

    }

    private FoxfireBallReworkProjectile createProjectile(LivingEntity entity) {
        FoxfireBallReworkProjectile proj = new FoxfireBallReworkProjectile(entity.level, entity, this);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Foxfire Ball", AbilityCategory.DEVIL_FRUITS, FoxfireBallRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{DealDamageComponent.getTooltip(80.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{ChargeComponent.getTooltip(75.0F)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{ContinuousComponent.getTooltip(150.0F)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{CooldownComponent.getTooltip(600.0F)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, RequireMorphComponent.getTooltip()}).setSourceElement(SourceElement.FIRE).setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    }
}