package net.kazi.kazimod.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.mera.HeatDashAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

@Mixin(value = HeatDashAbility.class, remap = false)
public abstract class HeatDashAbilityMixin extends Ability {

    @Unique
    private static final int KAZI_HEAT_DASH_STACKS = 2;
    @Unique
    private static final float KAZI_HEAT_DASH_SHORT_COOLDOWN = 60.0F;
    @Unique
    private static final float KAZI_HEAT_DASH_LONG_COOLDOWN = 300.0F;

    @Unique
    private StackComponent kazi$heatDashStacks;
    @Unique
    private float kazi$pendingCooldown;

    protected HeatDashAbilityMixin(AbilityCore<? extends IAbility> core) {
        super(core);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void kazi$initStacks(AbilityCore<HeatDashAbility> core, CallbackInfo ci) {
        this.kazi$heatDashStacks = new StackComponent((IAbility) (Object) this, KAZI_HEAT_DASH_STACKS);
        this.addComponents(this.kazi$heatDashStacks);
    }

    @Inject(method = "onUseEvent", at = @At("TAIL"))
    private void kazi$consumeStack(LivingEntity entity, IAbility ability, CallbackInfo ci) {
        if (!AbilityHelper.canUseMomentumAbilities(entity) || this.kazi$heatDashStacks == null) {
            return;
        }

        this.kazi$heatDashStacks.addStacks(entity, (IAbility) (Object) this, -1);
        if (this.kazi$heatDashStacks.getStacks() <= 0) {
            this.kazi$pendingCooldown = KAZI_HEAT_DASH_LONG_COOLDOWN;
            this.kazi$heatDashStacks.setStacks(entity, (IAbility) (Object) this, KAZI_HEAT_DASH_STACKS);
        } else {
            this.kazi$pendingCooldown = KAZI_HEAT_DASH_SHORT_COOLDOWN;
        }
    }

    @Inject(method = "onContinuityEnd", at = @At("HEAD"), cancellable = true)
    private void kazi$replaceCooldown(LivingEntity entity, IAbility ability, CallbackInfo ci) {
        float cooldown = this.kazi$pendingCooldown > 0.0F ? this.kazi$pendingCooldown : KAZI_HEAT_DASH_LONG_COOLDOWN;
        this.cooldownComponent.startCooldown(entity, cooldown);
        this.kazi$pendingCooldown = 0.0F;
        ci.cancel();
    }

    @ModifyConstant(method = "onContinuityTick", constant = @Constant(floatValue = 15.0F))
    private float kazi$shorterHoldTime(float original) {
        return 13.0F;
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(floatValue = 15.0F))
    private static float kazi$updateHoldTooltip(float original) {
        return 13.0F;
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(floatValue = 200.0F))
    private static float kazi$updateCooldownTooltip(float original) {
        return 300.0F;
    }
}
