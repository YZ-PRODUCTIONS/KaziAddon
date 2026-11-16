package net.kazi.kazimod.mixin.client;
import net.kazi.kazimod.ghoul.GhoulClient;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerModel.class)
public abstract class GhoulPlayerPoseMixin {
    @Inject(method="setupAnim",at=@At("TAIL"))
    private void ghoulPose(LivingEntity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci){GhoulClient.playerPose((PlayerModel<?>)(Object)this,entity,age);}
}
