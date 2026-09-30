package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.abilities.lunarian.LunarianHelper;
import net.kazi.kazimod.abilities.Nusu.NusuEvents;
import net.kazi.kazimod.abilities.Nusu.SkillHunterEXAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import xyz.pixelatedw.mineminenomi.abilities.sui.FreeSwimmingAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireAbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.abilities.artofweather.ArtOfWeatherHelper;
import xyz.pixelatedw.mineminenomi.abilities.brawler.GenkotsuMeteorAbility;
import xyz.pixelatedw.mineminenomi.init.ModItems;
import xyz.pixelatedw.mineminenomi.abilities.kage.KageHelper;
import xyz.pixelatedw.mineminenomi.abilities.kage.NightmareSoldiersAbility;
import net.MrMagicalCart.cartaddon.abilities.ryutriceratops.TriceratopsHelper;

/**
 * Nusu fighting-style checks and Skill Hunter EX's scoped prerequisite bypass.
 * Each nested accessor keeps its own target because the fields belong to
 * different classes. Forms are provisioned by Skill Hunter EX.
 */
@Mixin(value = AbilityHelper.class, remap = false)
public abstract class NusuAbilityCheckBypassMixin {

    @Mixin(value = RequireAbilityComponent.class, remap = false)
    public interface RequireAbilityComponentAccessor {
        @Accessor("checks")
        RequireAbilityComponent.CheckData<?>[] kazi$getChecks();
    }

    @Mixin(value = RequireMorphComponent.class, remap = false)
    public interface RequireMorphComponentAccessor {
        @Accessor("morphs")
        MorphInfo[] kazi$getMorphs();
    }

    @Mixin(value = FreeSwimmingAbility.class, remap = false)
    public interface FreeSwimmingAbilityAccessor {
        @Accessor("isSwimming")
        boolean kazi$isSwimming();

        @Accessor("isSwimming")
        void kazi$setSwimming(boolean swimming);
    }

    @Inject(
            method = "canUseBrawlerAbilities(Lnet/minecraft/entity/LivingEntity;Lxyz/pixelatedw/mineminenomi/api/abilities/IAbility;)Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUseResult;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void kazi$bypassBrawlerRequirement(
            LivingEntity entity,
            IAbility ability,
            CallbackInfoReturnable<AbilityUseResult> cir
    ) {
        if (NusuEvents.isNusuUser(entity)) {
            cir.setReturnValue(AbilityUseResult.success());
        }
    }

    @Inject(
            method = "requireAbilityCheck(Lnet/minecraft/entity/LivingEntity;Lxyz/pixelatedw/mineminenomi/api/abilities/IAbility;[Lxyz/pixelatedw/mineminenomi/api/abilities/components/RequireAbilityComponent$CheckData;)Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUseResult;",
            at = @At("HEAD"), cancellable = true, remap = false
    )
    private static void kazi$bypassRolledAbilityPrerequisite(
            LivingEntity entity, IAbility ability, RequireAbilityComponent.CheckData<?>[] checks,
            CallbackInfoReturnable<AbilityUseResult> cir
    ) {
        if (SkillHunterEXAbility.isRolledMove(entity, ability)) {
            cir.setReturnValue(AbilityUseResult.success());
        }
    }

    @Inject(
            method = "canUseMorphAbility(Lnet/minecraft/entity/LivingEntity;Lxyz/pixelatedw/mineminenomi/api/abilities/IAbility;[Lxyz/pixelatedw/mineminenomi/api/morph/MorphInfo;)Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUseResult;",
            at = @At("HEAD"), cancellable = true, remap = false
    )
    private static void kazi$bypassRolledMorphPrerequisite(
            LivingEntity entity, IAbility ability, MorphInfo[] morphs,
            CallbackInfoReturnable<AbilityUseResult> cir
    ) {
        if (SkillHunterEXAbility.isRolledMove(entity, ability)) {
            cir.setReturnValue(AbilityUseResult.success());
        }
    }

    @Inject(
            method = "requiresClimaTact(Lnet/minecraft/entity/LivingEntity;Lxyz/pixelatedw/mineminenomi/api/abilities/IAbility;)Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUseResult;",
            at = @At("HEAD"), cancellable = true, remap = false
    )
    private static void kazi$bypassRolledClimaTactRequirement(LivingEntity entity, IAbility ability,
            CallbackInfoReturnable<AbilityUseResult> cir) {
        if (SkillHunterEXAbility.isRolledMove(entity, ability)) {
            SkillHunterEXAbility.ensureWeatherTool(entity, ability);
            cir.setReturnValue(AbilityUseResult.success());
        }
    }

    @Inject(
            method = "canUseSwordsmanAbilities(Lnet/minecraft/entity/LivingEntity;Lxyz/pixelatedw/mineminenomi/api/abilities/IAbility;)Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUseResult;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void kazi$bypassSwordsmanRequirement(
            LivingEntity entity,
            IAbility ability,
            CallbackInfoReturnable<AbilityUseResult> cir
    ) {
        if (NusuEvents.isNusuUser(entity)) {
            cir.setReturnValue(AbilityUseResult.success());
        }
    }

    @Inject(
            method = "canUseBrawlerAbilities(Lnet/minecraft/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void kazi$bypassBrawlerRuntimeRequirement(
            LivingEntity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (NusuEvents.isNusuUser(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "canUseSwordsmanAbilities(Lnet/minecraft/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void kazi$bypassSwordsmanRuntimeRequirement(
            LivingEntity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (NusuEvents.isNusuUser(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Mixin(value = RequireAbilityComponent.class, remap = false)
    public abstract static class RequireAbilityComponentMixin {
        @Inject(method = "checkRequirements(Lnet/minecraft/entity/LivingEntity;)Z",
                at = @At("HEAD"), cancellable = true, remap = false)
        private void kazi$keepRolledMoveRunning(LivingEntity entity,
                CallbackInfoReturnable<Boolean> cir) {
            IAbility ability = ((RequireAbilityComponent) (Object) this).getAbility();
            if (SkillHunterEXAbility.isRolledMove(entity, ability)) cir.setReturnValue(true);
        }
    }

    @Mixin(value = RequireMorphComponent.class, remap = false)
    public abstract static class RequireMorphComponentMixin {
        @Inject(method = "checkRequirements(Lnet/minecraft/entity/LivingEntity;)Z",
                at = @At("HEAD"), cancellable = true, remap = false)
        private void kazi$keepRolledMoveRunning(LivingEntity entity,
                CallbackInfoReturnable<Boolean> cir) {
            IAbility ability = ((RequireMorphComponent) (Object) this).getAbility();
            if (SkillHunterEXAbility.isRolledMove(entity, ability)) cir.setReturnValue(true);
        }
    }

    /**
     * Central requirement bypass for Skill Hunter EX. This skips every check registered by
     * an ability through addCanUseCheck while preserving cooldown, GCD, disable, pool, and
     * protected-area checks in Ability.canUse.
     */
    @Mixin(value = Ability.class, remap = false)
    public abstract static class AbilityMixin {
        @Redirect(
                method = "canUse(Lnet/minecraft/entity/LivingEntity;)Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUseResult;",
                at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"),
                remap = false
        )
        private java.util.stream.Stream<Ability.ICanUseEvent> kazi$skipRolledCanUseRequirements(
                java.util.List<Ability.ICanUseEvent> checks, LivingEntity entity) {
            IAbility ability = (IAbility) (Object) this;
            return SkillHunterEXAbility.isRolledMove(entity, ability)
                    ? java.util.stream.Stream.empty()
                    : checks.stream();
        }
    }

    /** Cart's style requirements share this Nusu mixin source but require their own target. */
    @Mixin(value = AbilityLimits.class, remap = false)
    public abstract static class AbilityLimitsMixin {
        @Inject(method = {"fruitless", "fruitlessHardCheck", "requiresSantoryu",
                "requiresBluntWeapon", "requiresTwinBarrel", "requiresGun", "requiresDagger",
                "requirestwoAxe", "requiresAxe", "requiresGreatBlade", "requiresSpear"},
                at = @At("HEAD"), cancellable = true, remap = false)
        private static void kazi$allowRolledAbility(LivingEntity entity, IAbility ability,
                CallbackInfoReturnable<AbilityUseResult> cir) {
            if (SkillHunterEXAbility.isRolledMove(entity, ability)) {
                cir.setReturnValue(AbilityUseResult.success());
            }
        }

        @Inject(method = {"ateFruit", "ateFruitHardCheck"}, at = @At("HEAD"),
                cancellable = true, remap = false)
        private static void kazi$keepRolledFruitlessMoveRunning(LivingEntity entity, IAbility ability,
                CallbackInfoReturnable<Boolean> cir) {
            if (SkillHunterEXAbility.isRolledMove(entity, ability)) cir.setReturnValue(false);
        }

        @Inject(method = {"canUseSantoryu", "hasThirdSword", "canUseNitoryu", "canUseBlunt",
                "canUseTwinBarrel"}, at = @At("HEAD"), cancellable = true, remap = false)
        private static void kazi$allowRolledRuntimeCheck(LivingEntity entity,
                CallbackInfoReturnable<Boolean> cir) {
            if (SkillHunterEXAbility.hasActiveRoll(entity)) cir.setReturnValue(true);
        }
    }

    /** The flame-on check is a lambda factory, so intercept its generated check directly. */
    @Mixin(value = LunarianHelper.class, remap = false)
    public abstract static class LunarianHelperMixin {
        @Inject(method = "lambda$requireFlameOn$1", at = @At("HEAD"), cancellable = true,
                remap = false)
        private static void kazi$bypassRolledFlameOnCheck(int requiredStacks, LivingEntity entity,
                IAbility ability, CallbackInfoReturnable<AbilityUseResult> cir) {
            if (SkillHunterEXAbility.isRolledMove(entity, ability)) {
                cir.setReturnValue(AbilityUseResult.success());
            }
        }
    }

    @Mixin(value = ArtOfWeatherHelper.class, remap = false)
    public abstract static class ArtOfWeatherHelperMixin {
        @Inject(method = {"needsClimaTact", "needsSorceryClimaTact"}, at = @At("HEAD"),
                cancellable = true, remap = false)
        private static void kazi$bypassRolledClimaTactRequirement(LivingEntity entity, IAbility ability,
                CallbackInfoReturnable<AbilityUseResult> cir) {
            if (SkillHunterEXAbility.isRolledMove(entity, ability)) {
                SkillHunterEXAbility.ensureWeatherTool(entity, ability);
                cir.setReturnValue(AbilityUseResult.success());
            }
        }
    }

    /** Supplies only an EX roll's virtual ammunition for Genkotsu Meteor. */
    @Mixin(value = GenkotsuMeteorAbility.class, remap = false)
    public abstract static class GenkotsuMeteorMixin {
        @Shadow private ItemStack cannonBalls;

        @Inject(method = "canUseAbility", at = @At("HEAD"), cancellable = true, remap = false)
        private void kazi$bypassRolledCannonBallCheck(LivingEntity entity, IAbility ability,
                CallbackInfoReturnable<AbilityUseResult> cir) {
            if (SkillHunterEXAbility.isRolledMove(entity, ability)) {
                cir.setReturnValue(AbilityUseResult.success());
            }
        }

        @Inject(method = "onUseEvent", at = @At("HEAD"), remap = false)
        private void kazi$provideRolledCannonBalls(LivingEntity entity, IAbility ability,
                org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
            if (SkillHunterEXAbility.isRolledMove(entity, ability) && cannonBalls == null) {
                cannonBalls = new ItemStack(ModItems.CANNON_BALL.get(), 64);
            }
        }
    }

    /** Gives a temporary Kage roll virtual shadows without consuming real shadow items. */
    @Mixin(value = KageHelper.class, remap = false)
    public abstract static class KageHelperMixin {
        @Inject(method = "hasEnoughShadows", at = @At("HEAD"), cancellable = true, remap = false)
        private static void kazi$bypassRolledShadowRequirement(LivingEntity entity, IAbility ability,
                int amount, CallbackInfoReturnable<AbilityUseResult> cir) {
            if (SkillHunterEXAbility.isRolledMove(entity, ability)) {
                cir.setReturnValue(AbilityUseResult.success());
            }
        }

        @Inject(method = "removeShadows", at = @At("HEAD"), cancellable = true, remap = false)
        private static void kazi$preserveShadowsForActiveRoll(LivingEntity entity, int amount,
                org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
            if (SkillHunterEXAbility.hasActiveRoll(entity)) ci.cancel();
        }
    }

    /** Nightmare Soldiers has two direct inventory checks instead of using KageHelper. */
    @Mixin(value = NightmareSoldiersAbility.class, remap = false)
    public abstract static class NightmareSoldiersMixin {
        @Redirect(
                method = {"canUseCheck", "duringChargingEvent"},
                at = @At(value = "INVOKE", target = "Lxyz/pixelatedw/mineminenomi/api/helpers/ItemsHelper;countItemInInventory(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/Item;)I"),
                remap = false
        )
        private int kazi$provideVirtualShadows(LivingEntity entity, net.minecraft.item.Item item) {
            IAbility ability = (IAbility) (Object) this;
            return SkillHunterEXAbility.isRolledMove(entity, ability)
                    ? Integer.MAX_VALUE
                    : xyz.pixelatedw.mineminenomi.api.helpers.ItemsHelper.countItemInInventory(entity, item);
        }
    }

    /** Triceratops attacks also require the separate Helicopter Frills toggle. */
    @Mixin(value = TriceratopsHelper.class, remap = false)
    public abstract static class TriceratopsHelperMixin {
        @Inject(method = "hasFrillsActive", at = @At("HEAD"), cancellable = true, remap = false)
        private static void kazi$bypassRolledFrillRequirement(LivingEntity entity, IAbility ability,
                CallbackInfoReturnable<AbilityUseResult> cir) {
            if (SkillHunterEXAbility.isRolledMove(entity, ability)) {
                cir.setReturnValue(AbilityUseResult.success());
            }
        }
    }
}
