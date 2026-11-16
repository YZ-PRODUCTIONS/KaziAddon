package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedAmputateAbility;
import net.kazi.kazimod.ope.OpeAmputate;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;

@Mixin(value = ReworkedAmputateAbility.class, remap = false)
public abstract class OpeAmputateMixin {
    @Redirect(method = "useEvent", at = @At(value = "INVOKE", target = "Lxyz/pixelatedw/mineminenomi/api/abilities/components/ProjectileComponent;shoot(Lnet/minecraft/entity/LivingEntity;FF)V"))
    private void kazi$instantCut(ProjectileComponent component, LivingEntity user, float speed, float inaccuracy) {
        OpeAmputate.use(user, (IAbility)this, component.getDamageBonusManager().applyBonus(3));
    }
}
