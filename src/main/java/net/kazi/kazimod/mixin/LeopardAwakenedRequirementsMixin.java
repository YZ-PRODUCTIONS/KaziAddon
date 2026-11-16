package net.kazi.kazimod.mixin;
import net.kazi.kazimod.leopard.LeopardFeatures;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;
@Mixin(value=RequireMorphComponent.class, remap=false)
public abstract class LeopardAwakenedRequirementsMixin {
    @Shadow private MorphInfo[] morphs;
    @Inject(method="checkRequirements", at=@At("HEAD"), cancellable=true)
    private void kazi$awakenedLeopard(LivingEntity user, CallbackInfoReturnable<Boolean> ci) {
        if (!LeopardFeatures.empowered(user)) return;
        for (MorphInfo morph : morphs) if (morph == ModMorphs.LEOPARD_HEAVY.get() || morph == ModMorphs.LEOPARD_WALK.get()) {
            ci.setReturnValue(true);
            return;
        }
    }
}
