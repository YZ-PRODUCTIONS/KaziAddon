package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.saber.CircleParryAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(value = CircleParryAbility.class, remap = false)
public class CircleParryAbilityMixin {

    private static final Map<UUID, Integer> IFRAME_TICKS = new HashMap<>();

    @Shadow @Final
    private ContinuousComponent continuousComponent;

    @Shadow @Final
    private StackComponent stackComponent;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 5, ordinal = 0), remap = false)
    private int kazi$useThreeParryStages(int original) {
        return 3;
    }

    @Inject(method = "onTickContinuityEvent", at = @At("HEAD"), remap = false)
    private void kazi$tickParryIframes(LivingEntity entity, IAbility ability, CallbackInfo ci) {
        UUID id = entity.getUUID();
        Integer ticks = IFRAME_TICKS.get(id);
        if (ticks == null) {
            return;
        }
        if (ticks <= 1) {
            IFRAME_TICKS.remove(id);
        } else {
            IFRAME_TICKS.put(id, ticks - 1);
        }
    }

    @Inject(method = "onEndContinuityEvent", at = @At("HEAD"), remap = false)
    private void kazi$clearParryIframes(LivingEntity entity, IAbility ability, CallbackInfo ci) {
        IFRAME_TICKS.remove(entity.getUUID());
    }

    @Inject(method = "onDamageTakenEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void kazi$uniformParryReflect(LivingEntity entity, IAbility ability, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (AbilityHelper.isDodging(entity) || !this.continuousComponent.isContinuous()) {
            return;
        }

        Integer iframeTicks = IFRAME_TICKS.get(entity.getUUID());
        if (iframeTicks != null && iframeTicks > 0) {
            cir.setReturnValue(0.0F);
            return;
        }

        if (!entity.getMainHandItem().isEmpty()) {
            entity.getMainHandItem().hurtAndBreak(1, entity, user -> user.broadcastBreakEvent(net.minecraft.inventory.EquipmentSlotType.MAINHAND));
        }

        entity.level.playSound(null, entity.blockPosition(), ModSounds.GUARD.get(), SoundCategory.PLAYERS, 3.0F, 1.0F);
        IFRAME_TICKS.put(entity.getUUID(), 4);

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity && attacker != entity) {
            ((LivingEntity) attacker).hurt(DamageSource.mobAttack(entity), amount * 0.5F);
        }

        cir.setReturnValue(amount * 0.5F);
    }
}
