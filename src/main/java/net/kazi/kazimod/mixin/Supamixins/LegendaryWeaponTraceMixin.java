package net.kazi.kazimod.mixin.Supamixins;

import java.util.function.Consumer;
import net.MrMagicalCart.cartaddon.abilities.dagger.FinalExecutionAbility;
import net.MrMagicalCart.cartaddon.abilities.greatblade.VoidShatteringSlashAbility;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.MixedBumakiAbility;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.ParadiseTotsukaAbility;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.kazi.kazimod.abilities.BludgeonRework.DestroyerOfDeathThunderBaguaRework;
import net.kazi.kazimod.abilities.BludgeonRework.ShinsokuHakujakuRework;
import net.kazi.kazimod.abilities.BrawlerRework.GalaxyImpactRework;
import net.kazi.kazimod.abilities.SaberRework.DivineDepartureClone;
import net.kazi.kazimod.abilities.SpearRework.AbsolutePierceRework;
import net.kazi.kazimod.abilities.SupaRework.LegendaryWeaponTrace;
import net.kazi.kazimod.abilities.SupaRework.TracedFinalExecution;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

/**
 * Adapts direct prerequisites in traced moves without changing the player's
 * equipment, fighting style, Haki state, or checks made by other abilities.
 */
public final class LegendaryWeaponTraceMixin {
    private LegendaryWeaponTraceMixin() {}

    @Mixin(value = AbilityHelper.class, remap = false)
    public abstract static class ValidityChecks {
        // A previously earned move keeps its real unlock type. Only the active loan
        // is treated as a temporary command grant during progression validation.
        // The helper's independent banned-ability checks still run normally.
        @Redirect(method = {"checkValidAbility", "checkAndUnlockAbility"},
                at = @At(value = "INVOKE",
                        target = "Lxyz/pixelatedw/mineminenomi/data/entity/ability/IAbilityData;getUnlockTypeForAbility(Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityCore;)Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUnlock;"),
                remap = false)
        private static AbilityUnlock kazi$validateTemporaryLoan(IAbilityData data, AbilityCore<?> core,
                LivingEntity entity, AbilityCore<?> checkedCore) {
            return LegendaryWeaponTrace.canUseRolledMove(entity, data.getEquippedAbility(core))
                    ? AbilityUnlock.COMMAND : data.getUnlockTypeForAbility(core);
        }
    }

    @Mixin(value = {GalaxyImpactRework.class, DestroyerOfDeathThunderBaguaRework.class,
            ParadiseTotsukaAbility.class, DivineDepartureClone.class,
            VoidShatteringSlashAbility.class}, remap = false)
    public abstract static class HakiRequirements {
        @Redirect(method = "*", at = @At(value = "INVOKE",
                target = "Lxyz/pixelatedw/mineminenomi/api/helpers/HakiHelper;hasInfusionActive(Lnet/minecraft/entity/LivingEntity;)Z"),
                remap = false)
        private boolean kazi$traceInfusionPrerequisite(LivingEntity entity) {
            return LegendaryWeaponTrace.canUseRolledMove(entity, (IAbility) (Object) this)
                    || HakiHelper.hasInfusionActive(entity);
        }

        @Redirect(method = "*", at = @At(value = "INVOKE",
                target = "Lxyz/pixelatedw/mineminenomi/api/helpers/HakiHelper;checkForHakiOveruse(Lnet/minecraft/entity/LivingEntity;I)Z"),
                remap = false)
        private boolean kazi$traceHakiPrerequisite(LivingEntity entity, int amount) {
            return !LegendaryWeaponTrace.canUseRolledMove(entity, (IAbility) (Object) this)
                    && HakiHelper.checkForHakiOveruse(entity, amount);
        }
    }

    @Mixin(value = {ParadiseTotsukaAbility.class, MixedBumakiAbility.class}, remap = false)
    public abstract static class NitoryuRequirements {
        @Redirect(method = "*", at = @At(value = "INVOKE",
                target = "Lnet/MrMagicalCart/cartaddon/api/helpers/AbilityLimits;canUseNitoryu(Lnet/minecraft/entity/LivingEntity;)Z"),
                remap = false)
        private boolean kazi$traceOffhandSword(LivingEntity entity) {
            return LegendaryWeaponTrace.isRolledMove(entity, (IAbility) (Object) this)
                    || AbilityLimits.canUseNitoryu(entity);
        }
    }

    @Mixin(value = {ParadiseTotsukaAbility.class, MixedBumakiAbility.class,
            DivineDepartureClone.class}, remap = false)
    public abstract static class SwordRequirements {
        @Redirect(method = "*", at = @At(value = "INVOKE",
                target = "Lxyz/pixelatedw/mineminenomi/api/helpers/AbilityHelper;canUseSwordsmanAbilities(Lnet/minecraft/entity/LivingEntity;)Z"),
                remap = false)
        private boolean kazi$traceMainhandSword(LivingEntity entity) {
            return LegendaryWeaponTrace.isRolledMove(entity, (IAbility) (Object) this)
                    || AbilityHelper.canUseSwordsmanAbilities(entity);
        }
    }

    @Mixin(value = ShinsokuHakujakuRework.class, remap = false)
    public abstract static class BluntRequirements {
        @Redirect(method = {"tickChargeEvent", "endChargeEvent"},
                at = @At(value = "INVOKE",
                        target = "Lnet/MrMagicalCart/cartaddon/api/helpers/AbilityLimits;canUseBlunt(Lnet/minecraft/entity/LivingEntity;)Z"),
                remap = false)
        private boolean kazi$traceBluntWeapon(LivingEntity entity) {
            return LegendaryWeaponTrace.isRolledMove(entity, (IAbility) (Object) this)
                    || AbilityLimits.canUseBlunt(entity);
        }
    }

    // Cancel the offensive callback, not ContinuousComponent.stopContinuity: the
    // component must still clear its pool, reset its state, and send its stop packet.
    @Mixin(value = AbsolutePierceRework.class, remap = false)
    public abstract static class PierceCancellation {
        @Inject(method = "endContinuityEvent", at = @At("HEAD"), cancellable = true,
                remap = false)
        private void kazi$cancelTracedPierce(LivingEntity entity, IAbility ability, CallbackInfo ci) {
            if (LegendaryWeaponTrace.isReturningMove(entity, ability)) {
                ability.getComponent(ModAbilityKeys.ANIMATION).ifPresent(component -> component.stop(entity));
                ci.cancel();
            }
        }
    }

    @Mixin(value = MixedBumakiAbility.class, remap = false)
    public abstract static class BumakiCancellation {
        @Inject(method = "endContinuousEvent", at = @At("HEAD"), cancellable = true,
                remap = false)
        private void kazi$cancelTracedBumaki(LivingEntity entity, IAbility ability, CallbackInfo ci) {
            if (LegendaryWeaponTrace.isReturningMove(entity, ability)) {
                ability.getComponent(ModAbilityKeys.ANIMATION).ifPresent(component -> component.stop(entity));
                ci.cancel();
            }
        }
    }

    @Mixin(value = GalaxyImpactRework.class, remap = false)
    public abstract static class GalaxyCancellation {
        @Shadow @Final private ChangeStatsComponent statsComponent;
        @Shadow private LightningDischargeEntity discharge;
        @Shadow private SphereEntity galaxyImpactVisuals;

        @Inject(method = "endContinuityEvent", at = @At("HEAD"), cancellable = true,
                remap = false)
        private void kazi$cancelTracedGalaxy(LivingEntity entity, IAbility ability, CallbackInfo ci) {
            if (LegendaryWeaponTrace.isReturningMove(entity, ability)) {
                statsComponent.removeModifiers(entity);
                if (discharge != null) discharge.setAliveTicks(0);
                if (galaxyImpactVisuals != null) galaxyImpactVisuals.remove();
                ability.getComponent(ModAbilityKeys.ANIMATION).ifPresent(component -> component.stop(entity));
                ci.cancel();
            }
        }
    }

    @Mixin(value = FinalExecutionAbility.class, remap = false)
    public abstract static class FinalExecutionFinish {
        @Shadow @Final private DealDamageComponent dealDamageComponent;
        @Shadow @Final private RangeComponent rangeComponent;
        @Shadow @Final private HitTrackerComponent hitTrackerComponent;
        @Shadow @Final private AnimationComponent animationComponent;

        @Inject(method = "endChargeEvent", at = @At("HEAD"), cancellable = true, remap = false)
        private void kazi$finishTracedExecution(LivingEntity entity, IAbility ability, CallbackInfo ci) {
            if (LegendaryWeaponTrace.isReturningMove(entity, ability)) {
                animationComponent.stop(entity);
                ci.cancel();
            } else if (LegendaryWeaponTrace.isRolledMove(entity, ability)) {
                TracedFinalExecution.finish(entity, ability, dealDamageComponent, rangeComponent,
                        hitTrackerComponent, animationComponent);
                ci.cancel();
            }
        }
    }

    @Mixin(value = {ShinsokuHakujakuRework.class, ParadiseTotsukaAbility.class,
            MixedBumakiAbility.class, FinalExecutionAbility.class,
            DivineDepartureClone.class, VoidShatteringSlashAbility.class}, remap = false)
    public abstract static class WeaponDurability {
        @Redirect(method = "*", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/item/ItemStack;hurtAndBreak(ILnet/minecraft/entity/LivingEntity;Ljava/util/function/Consumer;)V",
                remap = true), remap = false)
        private <T extends LivingEntity> void kazi$preserveHeldItem(
                ItemStack stack, int damage, T entity, Consumer<T> onBreak) {
            IAbility ability = (IAbility) (Object) this;
            if (!LegendaryWeaponTrace.isRolledMove(entity, ability)
                    && !LegendaryWeaponTrace.isReturningMove(entity, ability)) {
                stack.hurtAndBreak(damage, entity, onBreak);
            }
        }
    }
}
