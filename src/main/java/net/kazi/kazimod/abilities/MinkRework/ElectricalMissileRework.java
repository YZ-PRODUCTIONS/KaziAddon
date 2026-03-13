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
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.DashAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.BonusOperation;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class ElectricalMissileRework extends DashAbility {
    private static final float COOLDOWN_BONUS = 0.5F;
    private static final float DAMAGE_BONUS = 2.5F;
    private static final ITextComponent[] DESCRIPTION;
    private static final int SHORT_COOLDOWN = 40;
    private static final int MAIN_COOLDOWN = 180;
    private static final float RANGE = 2.0F;
    private static final int DAMAGE = 20;
    private static final int ELECLAW_STACKS = 1;
    public static final AbilityCore<ElectricalMissileRework> INSTANCE;
    private double dashSpeed = (double)4.0F;
    private boolean eleClawBuff = false;

    public ElectricalMissileRework(AbilityCore<ElectricalMissileRework> core) {
        super(core);
        this.continuousComponent.addStartEvent(this::startContinuityEvent);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.eleClawBuff = false;
        CartEleclawAbility cartEleclawAbility = (CartEleclawAbility)AbilityDataCapability.get(entity).getEquippedAbility(CartEleclawAbility.INSTANCE);
        if (cartEleclawAbility != null && cartEleclawAbility.isContinuous()) {
            cartEleclawAbility.reduceUsage(entity, 1);
            this.eleClawBuff = true;
        }

        boolean hasSulongActive = CartElectroHelper.hasSulongActive(entity);
        this.dashSpeed = hasSulongActive ? (double)4.0F : (double)3.25F;
        this.dealDamageComponent.getBonusManager().removeBonus(CartElectroHelper.SULONG_DAMAGE_BONUS);
        this.cooldownComponent.getBonusManager().removeBonus(CartElectroHelper.SULONG_COOLDOWN_BONUS);
        if (hasSulongActive) {
            this.dealDamageComponent.getBonusManager().addBonus(CartElectroHelper.SULONG_DAMAGE_BONUS, "Sulong Damage Bonus", BonusOperation.MUL, 2.5F);
            this.cooldownComponent.getBonusManager().addBonus(CartElectroHelper.SULONG_COOLDOWN_BONUS, "Sulong Cooldown Bonus", BonusOperation.MUL, 0.5F);
        }

        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.LIGHTNING_TELEPORT.get(), SoundCategory.PLAYERS, 3.0F, 1.15F);
    }

    public void onTargetHit(LivingEntity entity, LivingEntity target, float damage, DamageSource source) {
        target.addEffect(new EffectInstance((Effect)ModEffects.PARALYSIS.get(), 40, 0, false, false, true));
    }

    public float getDashCooldown() {
        return 180.0F;
    }

    public float getDamage() {
        return this.eleClawBuff ? 20.0F * CartElectroHelper.getEleClawAmp() : 20.0F;
    }

    public float getRange() {
        return 2.0F;
    }

    public double getSpeed() {
        return this.dashSpeed;
    }

    private static boolean canUnlock(LivingEntity user) {
        IEntityStats props = EntityStatsCapability.get(user);
        return props.isMink() && props.getDoriki() >= (double)800.0F;
    }

    static {
        DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "electrical_missile", new Pair[]{ImmutablePair.of("Powerful and fast forward dash that will stun enemies.", (Object)null), ImmutablePair.of("While %s is active the cooldown of this ability is reduced by %s and the damage is increased by %s.", new Object[]{AbilityHelper.mentionAbility(CartSulongAbility.INSTANCE), AbilityHelper.mentionText(Math.round(50.0F) + "%"), AbilityHelper.mentionText(Math.round(Math.abs(-1.5F) * 100.0F) + "%")})});
        INSTANCE = (new AbilityCore.Builder("Electrical Missile", AbilityCategory.RACIAL, ElectricalMissileRework::new)).addDescriptionLine(new ITextComponent[]{DESCRIPTION[0]}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{IDescriptionLine.of(DESCRIPTION[1])}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(180.0F), RangeComponent.getTooltip(1.6F, RangeType.AOE), DealDamageComponent.getTooltip(35.0F)}).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(new SourceType[]{SourceType.FIST}).setSourceElement(SourceElement.LIGHTNING).setUnlockCheck(ElectricalMissileRework::canUnlock).build();
    }
}
