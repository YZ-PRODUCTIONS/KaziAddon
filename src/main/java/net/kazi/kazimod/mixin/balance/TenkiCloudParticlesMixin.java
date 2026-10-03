package net.kazi.kazimod.mixin.balance;

import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity;
import net.minecraft.entity.Entity;
import net.minecraft.particles.IParticleData;
import net.minecraft.world.server.ServerWorld;
import net.kazi.kazimod.preserved.tenki.TenkiVisualEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=WeatherCloudReworkEntity.class,remap=false)
public abstract class TenkiCloudParticlesMixin {
    @Inject(method="spawnParticles",at=@At("HEAD"),cancellable=true)
    private void kazimod$noTenkiParticles(CallbackInfo ci){if(((Entity)(Object)this).getPersistentData().getBoolean("KaziTenkiVfx"))ci.cancel();}
    @Redirect(method={"transformTo","doThunderVolley"},at=@At(value="INVOKE",target="Lnet/minecraft/world/server/ServerWorld;sendParticles(Lnet/minecraft/particles/IParticleData;DDDIDDDD)I",remap=true))
    private int kazimod$replaceThunderSparks(ServerWorld world,IParticleData particle,double x,double y,double z,int count,double dx,double dy,double dz,double speed){
        Entity cloud=(Entity)(Object)this;
        if(!cloud.getPersistentData().getBoolean("KaziTenkiVfx"))return world.sendParticles(particle,x,y,z,count,dx,dy,dz,speed);
        TenkiVisualEntity effect=TenkiVisualEntity.spawn(cloud,4,5,8);effect.setPos(x,y,z);return 0;
    }
}
