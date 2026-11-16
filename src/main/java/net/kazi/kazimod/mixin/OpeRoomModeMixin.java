package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedRoomAbility;
import net.kazi.kazimod.abilities.OpeRework.KRoomAnesthesiaRework;
import net.kazi.kazimod.config.KaziConfig;
import net.kazi.kazimod.ope.OpeRoomMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

@Mixin(value = ReworkedRoomAbility.class, remap = false)
public abstract class OpeRoomModeMixin extends Ability {
    @Shadow @Final private ChargeComponent chargeComponent;
    @Shadow @Final private ContinuousComponent continuousComponent;
    @Unique private OpeRoomMode kazi$mode;
    @Unique private SkinOverlayComponent kazi$overlay;

    protected OpeRoomModeMixin(AbilityCore<?> core) { super(core); }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void kazi$addMode(AbilityCore<?> core, CallbackInfo ci) {
        if (KaziConfig.INSTANCE.disableFruitChanges.get()) return;
        kazi$mode = new OpeRoomMode(this);
        kazi$overlay = new SkinOverlayComponent(this, KRoomAnesthesiaRework.OVERLAY);
        addComponents(kazi$mode, kazi$overlay);
        continuousComponent.addStartEvent((user, ability) -> {
            if (OpeRoomMode.selected(this) && !user.level.isClientSide) {
                kazi$overlay.showAll(user);
                KRoomAnesthesiaRework.updateKRoomSwordTint(user, true);
            }
        });
        addRemoveEvent((user, ability) -> {
            if (OpeRoomMode.selected(this) && chargeComponent.isCharging()) chargeComponent.forceStopCharging(user);
        });
    }

    @Override public ITextComponent getDisplayName() {
        return OpeRoomMode.selected(this) ? KRoomAnesthesiaRework.INSTANCE.getLocalizedName() : super.getDisplayName();
    }

    @Override public ResourceLocation getIcon(LivingEntity user) {
        return OpeRoomMode.selected(this) ? OpeRoomMode.ICON : super.getIcon(user);
    }

    @Inject(method = "onUseEvent", at = @At("HEAD"), cancellable = true)
    private void kazi$useMode(LivingEntity user, IAbility ability, CallbackInfo ci) {
        if (!OpeRoomMode.selected(this)) return;
        ci.cancel();
        if (user.level.isClientSide) return;
        if (continuousComponent.isContinuous()) continuousComponent.stopContinuity(user);
        else if (chargeComponent.isCharging()) chargeComponent.forceStopCharging(user);
        else if (OpeRoomMode.unlocked(user)) chargeComponent.startCharging(user, 50);
        else kazi$mode.revertToDefault(user);
    }

    @Inject(method = "onChargeTick", at = @At("HEAD"), cancellable = true)
    private void kazi$chargeMode(LivingEntity user, IAbility ability, CallbackInfo ci) {
        if (!OpeRoomMode.selected(this)) return;
        ci.cancel();
        if (!user.level.isClientSide && user.tickCount % 18 == 0) {
            user.level.playSound(null, user.blockPosition(), ModSounds.ROOM_CHARGE_SFX.get(), SoundCategory.PLAYERS, 5F, .85F);
        }
    }

    @Inject(method = "onChargeEnd", at = @At("HEAD"), cancellable = true)
    private void kazi$finishModeCharge(LivingEntity user, IAbility ability, CallbackInfo ci) {
        if (!OpeRoomMode.selected(this)) return;
        ci.cancel();
        if (!user.level.isClientSide && OpeRoomMode.unlocked(user)) continuousComponent.startContinuity(user);
    }

    @Inject(method = "onContinuityTick", at = @At("HEAD"), cancellable = true)
    private void kazi$tickMode(LivingEntity user, IAbility ability, CallbackInfo ci) {
        if (!OpeRoomMode.selected(this)) return;
        ci.cancel();
        if (user.level.isClientSide) return;
        if (OpeRoomMode.unlocked(user)) KRoomAnesthesiaRework.updateKRoomSwordTint(user, true);
        else continuousComponent.stopContinuity(user);
    }

    @Inject(method = "onContinuityEnd", at = @At("HEAD"), cancellable = true)
    private void kazi$endMode(LivingEntity user, IAbility ability, CallbackInfo ci) {
        if (!OpeRoomMode.selected(this)) return;
        ci.cancel();
        if (!user.level.isClientSide) {
            kazi$overlay.hideAll(user);
            KRoomAnesthesiaRework.updateKRoomSwordTint(user, false);
        }
    }
}
