package net.kazi.kazimod.mixin.balance;

import net.minecraft.entity.Entity;
import net.kazi.kazimod.entities.projectiles.WindGustProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;

@Mixin(targets={"net.kazi.kazimod.entities.projectiles.WindGustProjectile","net.kazi.kazimod.entities.projectiles.GomuGomuNoKaminariProjectile","xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity"},remap=false)
public abstract class TenkiExplosionVfxMixin {
    @org.spongepowered.asm.mixin.Unique private int kazi$lastElectricImpact = -100;
    @Redirect(method={"onTickEvent","onBlockImpactEvent","onHitBlock","onFirstImpact"},require=1,at=@At(value="INVOKE",target="Lxyz/pixelatedw/mineminenomi/api/abilities/ExplosionAbility;doExplosion()V"))
    private void kazimod$noSmoke(ExplosionAbility explosion){
        Entity e=(Entity)(Object)this;
        if(e instanceof xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity
                && net.kazi.kazimod.goro.GoroVisuals.kind((xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity)e)!=0) {
            boolean visible=e.tickCount-kazi$lastElectricImpact>=4;
            ((net.kazi.kazimod.goro.GoroExplosionVisuals)explosion).kazi$goroExplosion(visible);
            if(visible)kazi$lastElectricImpact=e.tickCount;
        }
        if(e instanceof WindGustProjectile||e.getPersistentData().getBoolean("KaziTenkiVfx"))explosion.setSmokeParticles(null);
        if(e instanceof xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity
                && ((xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity)e).getParent()
                == net.kazi.kazimod.abilities.GoroRework.SangoRework.INSTANCE) {
            net.kazi.kazimod.goro.GoroVisuals.blueImpact(explosion);
        } else explosion.doExplosion();
    }
}
