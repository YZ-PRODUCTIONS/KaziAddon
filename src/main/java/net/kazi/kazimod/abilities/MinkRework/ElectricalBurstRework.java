//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.MinkRework;

import net.MrMagicalCart.cartaddon.abilities.electroextra.CartEleclawAbility;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartElectroHelper;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartSulongAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class ElectricalBurstRework extends Ability {
    private static final ITextComponent[] DESCRIPTION;
    private static final int COOLDOWN = 240;
    private static final int CHARGE_TIME = 8;
    private static final float DAMAGE = 40.0F;
    private static final float COOLDOWN_BONUS = 0.8F;
    private static final float RANGE = 5.5F;
    private static final int WIDTH = 3;
    public static final AbilityCore<ElectricalBurstRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addEndEvent(this::stopChargeEvent);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final DealDamageComponent damageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private double dashSpeed = (double)2.25F;
    private double knockback = 1.15;
    private boolean eleClawBuff = false;

    public ElectricalBurstRework(AbilityCore<ElectricalBurstRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.animationComponent, this.damageComponent, this.rangeComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.startCharging(entity, 8.0F);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.eleClawBuff = false;
        CartEleclawAbility cartEleclawAbility = (CartEleclawAbility)AbilityDataCapability.get(entity).getEquippedAbility(CartEleclawAbility.INSTANCE);
        if (cartEleclawAbility != null && cartEleclawAbility.isContinuous()) {
            cartEleclawAbility.reduceUsage(entity, 1);
            this.eleClawBuff = true;
        }

        this.animationComponent.start(entity, ModAnimations.CHARGE_PUNCH);
    }

    private void stopChargeEvent(LivingEntity entity, IAbility ability) {
        boolean hasSulongActive = CartElectroHelper.hasSulongActive(entity);
        float dmg = hasSulongActive ? 50.0F : 40.0F;
        dmg = dmg == 40.0F && this.eleClawBuff ? 40.0F * CartElectroHelper.getEleClawAmp() : 40.0F;

        for(LivingEntity target : this.rangeComponent.getTargetsInLine(entity, 5.5F, 3.0F)) {
            if (this.damageComponent.hurtTarget(entity, target, dmg)) {
                target.addEffect(new EffectInstance((Effect)ModEffects.PARALYSIS.get(), 30, 0, false, false));
                target.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 15, 0, false, false));
            }

            AbilityHelper.disableAbilities(target, 100, (abl) -> abl.hasComponent(ModAbilityKeys.POOL) && ((PoolComponent)abl.getComponent(ModAbilityKeys.POOL).get()).containsPool(ModAbilityPools.TEKKAI_LIKE));
        }

        Vector3d speed = entity.getLookAngle().multiply(this.dashSpeed, (double)1.0F, this.dashSpeed);
        AbilityHelper.setDeltaMovement(entity, -speed.x, 0.2, -speed.z);
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 240.0F);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.LIGHTNING_TELEPORT.get(), SoundCategory.PLAYERS, 3.0F, 1.0F + entity.getRandom().nextFloat() / 3.0F);
    }

    private static boolean canUnlock(LivingEntity user) {
        IEntityStats props = EntityStatsCapability.get(user);
        return props.isMink() && props.getDoriki() >= (double)4500.0F;
    }

    static {
        DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "electrical_burst", new Pair[]{ImmutablePair.of("The user launches a burst of electricity in front of them and moves backwards.", (Object)null), ImmutablePair.of("While %s is active the damage is increased by %s.", new Object[]{AbilityHelper.mentionAbility(CartSulongAbility.INSTANCE), AbilityHelper.mentionText(Math.round(Math.abs(-1.5F) * 100.0F) + "%")})});
        INSTANCE = (new AbilityCore.Builder("Electrical Burst", AbilityCategory.RACIAL, ElectricalBurstRework::new)).addDescriptionLine(new ITextComponent[]{DESCRIPTION[0]}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{IDescriptionLine.of(DESCRIPTION[1])}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(40.0F), CooldownComponent.getTooltip(240.0F), RangeComponent.getTooltip(5.5F, RangeType.LINE)}).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(new SourceType[]{SourceType.FIST}).setSourceElement(SourceElement.LIGHTNING).setUnlockCheck(ElectricalBurstRework::canUnlock).build();
    }
}
