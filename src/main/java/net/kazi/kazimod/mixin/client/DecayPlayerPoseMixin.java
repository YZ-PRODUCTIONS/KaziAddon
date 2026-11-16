package net.kazi.kazimod.mixin.client;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerModel.class)
public abstract class DecayPlayerPoseMixin {
 @Unique private boolean kazi$decayOffset;
 @Inject(method="setupAnim",at=@At("HEAD"))
 private void resetDecayPose(LivingEntity p,float walk,float amount,float time,float yaw,float pitch,CallbackInfo ci){
  if(!kazi$decayOffset)return;PlayerModel<?> m=(PlayerModel<?>)(Object)this;
  m.body.y=m.body.z=m.head.y=m.head.z=0;
  m.rightArm.y=m.leftArm.y=2;m.rightArm.z=m.leftArm.z=0;kazi$decayOffset=false;
 }
 @Inject(method="setupAnim",at=@At("TAIL"))
 private void decayPose(LivingEntity p,float walk,float amount,float time,float yaw,float pitch,CallbackInfo ci){net.kazi.kazimod.decay.DecayClient.pose((PlayerModel<?>)(Object)this,p,time);int action=net.kazi.kazimod.decay.DecayFeatures.action(p);kazi$decayOffset=action==1||action==5;}
}
