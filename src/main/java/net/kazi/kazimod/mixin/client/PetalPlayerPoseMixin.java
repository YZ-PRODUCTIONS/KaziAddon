package net.kazi.kazimod.mixin.client;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerModel.class)
public abstract class PetalPlayerPoseMixin {
 @Inject(method="setupAnim",at=@At("TAIL"))
 private void petalPose(LivingEntity p,float walk,float amount,float time,float yaw,float pitch,CallbackInfo ci){net.kazi.kazimod.petal.PetalClient.pose((PlayerModel<?>)(Object)this,p,time);}
}
