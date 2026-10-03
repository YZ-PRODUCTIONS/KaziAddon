package net.kazi.kazimod.mixin.balance;

import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity;
import net.minecraft.entity.LivingEntity;
import net.kazi.kazimod.preserved.tenki.TenkiEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets={"net.kazi.kazimod.abilities.Tenki.CloudyDayAbility","net.kazi.kazimod.abilities.Tenki.GaleStormAbility","net.kazi.kazimod.abilities.Tenki.ThunderstormAbility"},remap=false)
public abstract class TenkiCloudVfxMixin {
    @Inject(method="spawnCloud",at=@At("RETURN"))
    private void kazimod$cloud(LivingEntity owner,double x,double y,double z,CallbackInfoReturnable<WeatherCloudReworkEntity> cir){
        WeatherCloudReworkEntity cloud=cir.getReturnValue();if(cloud==null)return;
        String form=cloud.getForm().name();int kind=form.equals("WINDY")?1:form.equals("THUNDER_STORM")?2:0;
        if(kind==0){
            // Cloudy Day keeps its nine gameplay clouds but needs only one visible canopy.
            if(Math.abs(x-owner.getX())>.01||Math.abs(z-owner.getZ())>.01){cloud.getPersistentData().putBoolean("KaziTenkiVfx",true);return;}
            cloud.getPersistentData().putBoolean("KaziTenkiVfx",true);
            net.kazi.kazimod.preserved.tenki.TenkiVisualEntity.spawnAt(owner,0,cloud.getRadius()+30,6000,new net.minecraft.util.math.vector.Vector3d(x,y,z));
            return;
        }
        TenkiEffects.attach(cloud,kind,cloud.getRadius());
    }
}
