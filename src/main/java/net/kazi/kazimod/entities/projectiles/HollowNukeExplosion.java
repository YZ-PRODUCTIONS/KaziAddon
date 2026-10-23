package net.kazi.kazimod.entities.projectiles;

import java.util.List;
import net.kazi.kazimod.entities.KokuVfxEntity;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.saiyan.SaiyanFeatures;
import net.kazi.kazimod.saiyan.SaiyanProjectile;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.*;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Shared damage and terrain impact; each ability supplies its own visuals. */
public final class HollowNukeExplosion {
    private static final BlockProtectionRule GRIEF_RULE=new BlockProtectionRule.Builder(new BlockProtectionRule[]{
            AirBlockProtectionRule.INSTANCE,LiquidBlockProtectionRule.INSTANCE,CoreBlockProtectionRule.INSTANCE,
            FoliageBlockProtectionRule.INSTANCE,OreBlockProtectionRule.INSTANCE}).build();
    private HollowNukeExplosion(){}

    public static void detonate(AbilityProjectileEntity source){
        if(source.level.isClientSide)return;
        for(ServerPlayerEntity player:((ServerWorld)source.level).players())
            if(player.distanceTo(source)<=100)KaziPacketHandler.sendFlashbang(player);
        KokuVfxEntity.impact(source.level,source.position(),KokuVfxEntity.NUKE_IMPACT,60);
        applyImpact(source);
    }

    public static void applyImpact(AbilityProjectileEntity source){
        if(source.level.isClientSide)return;
        int explosionRadius=48,shockwaveRadius=54;
        boolean spiritBomb=source instanceof SaiyanProjectile&&((SaiyanProjectile)source).kind()==2;
        if(CommonConfig.INSTANCE.isAbilityGriefingEnabled()){
            AbilityHelper.createSphere(source.level,source.blockPosition().below(explosionRadius/8),
                    explosionRadius,explosionRadius,false,Blocks.AIR,3,GRIEF_RULE);
            AbilityHelper.createSphere(source.level,source.blockPosition().below(explosionRadius/4),
                    (int)(explosionRadius*1.2F),(int)(explosionRadius*.4F),false,Blocks.AIR,3,GRIEF_RULE);
        }
        List<Entity> targets=WyHelper.getNearbyEntities(source.position(),source.level,(double)shockwaveRadius,null,new Class[]{Entity.class});
        for(Entity target:targets){
            if(target==source)continue;
            if(target instanceof ThrowableEntity||target instanceof AbstractArrowEntity){target.remove();continue;}
            if(target instanceof LivingEntity){
                if(spiritBomb)SaiyanFeatures.markSpiritBombTarget((LivingEntity)target);
                ((LivingEntity)target).addEffect(new EffectInstance(Effects.HARM,20,3,false,false));
                Vector3d speed=target.getLookAngle().scale(-1).multiply(5,0,5);
                AbilityHelper.setDeltaMovement(target,speed.x,1,speed.z);
            }
        }
    }
}
