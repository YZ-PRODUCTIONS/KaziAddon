//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.onirework;

import net.MrMagicalCart.cartaddon.abilities.oni.OniHelper;
import net.MrMagicalCart.cartaddon.entities.projectiles.oni.ThunderingRoarProjectile;
import net.MrMagicalCart.cartaddon.init.CartValues;
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
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RepeaterComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class ViciousRoarRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "vicious_roar", new Pair[]{ImmutablePair.of("The user roars with all their might, disorientating any opponent. (Charge time decreases with §aOni Awakening§r)", new Object[]{"§a" + Math.round(19.999998F) + "%§r"})});
    private static final float COOLDOWN = 500.0F;
    private static final float CHARGE_TIME = 20.0F;
    public static final AbilityCore<ViciousRoarRework> INSTANCE;
    protected final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::onChargeStart).addTickEvent(this::onChargeTick).addEndEvent(this::onChargeEnd);
    private final RepeaterComponent repeaterComponent = (new RepeaterComponent(this)).addTriggerEvent(this::triggerRepeaterEvent).addStopEvent(this::stopRepeaterEvent);
    private final PoolComponent poolComponent;

    public ViciousRoarRework(AbilityCore<ViciousRoarRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.repeaterComponent, this.continuousComponent, this.chargeComponent, this.projectileComponent});
        this.addUseEvent(200, this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else if (OniHelper.hasAwakeningActive(entity)) {
            this.chargeComponent.startCharging(entity, 10.0F);
        } else {
            this.chargeComponent.startCharging(entity, 20.0F);
        }

    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 0, false, false));
        }

    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.continuousComponent.startContinuity(entity);
        }

    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.repeaterComponent.start(entity, 5, 10);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5, 2, false, false));
        }

    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 500.0F);
    }

    private void triggerRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity, 0.95F, 2.0F);
    }

    private void stopRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.stopContinuity(entity);
    }

    private ThunderingRoarProjectile createProjectile(LivingEntity entity) {
        ThunderingRoarProjectile proj = new ThunderingRoarProjectile(entity.level, entity, this);
        return proj;
    }

    private static boolean canUnlock(LivingEntity entity) {
        IEntityStats props = EntityStatsCapability.get(entity);
        boolean race = props.getRace().equals(CartValues.ONI);
        return race && props.getDoriki() >= (double)5500.0F;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Vicious Roar", AbilityCategory.RACIAL, ViciousRoarRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ChargeComponent.getTooltip(20.0F), CooldownComponent.getTooltip(500.0F)}).setSourceElement(SourceElement.AIR).setUnlockCheck(ViciousRoarRework::canUnlock).build();
    }
}
