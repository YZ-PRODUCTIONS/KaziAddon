package net.kazi.kazimod.shikigami;

import net.kazi.kazimod.agito.*;
import net.kazi.kazimod.mahoraga.*;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.*;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerEvent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;

/** Per-player taming progress and death locks, shared by all six Kage summons. */
public final class TenShadowsTaming {
    public static final int DEATH_COOLDOWN=24000;
    private static final String ROOT="kazimodTenShadows";

    private static CompoundNBT state(LivingEntity owner,String id){
        CompoundNBT data=owner.getPersistentData(),all=data.getCompound(ROOT);
        if(!data.contains(ROOT,10))data.put(ROOT,all);
        CompoundNBT state=all.getCompound(id);
        if(!all.contains(id,10))all.put(id,state);
        return state;
    }
    private static long time(LivingEntity owner){
        if(owner.level instanceof ServerWorld){
            ServerWorld overworld=((ServerWorld)owner.level).getServer().getLevel(World.OVERWORLD);
            if(overworld!=null)return overworld.getGameTime();
        }
        return owner.level.getGameTime();
    }
    public static boolean tamed(LivingEntity owner,String id){return owner!=null&&state(owner,id).getBoolean("Tamed");}
    public static boolean isTaming(Entity entity){return entity instanceof TenShadowsBeast&&((TenShadowsBeast)entity).isTaming();}
    public static int remaining(LivingEntity owner,String id){
        return (int)Math.max(0,Math.min(DEATH_COOLDOWN,state(owner,id).getLong("CooldownUntil")-time(owner)));
    }
    public static int endCooldown(LivingEntity owner,String id,boolean defeated){
        int remaining=remaining(owner,id);
        return remaining>0?remaining:defeated?DEATH_COOLDOWN:200;
    }
    public static boolean canSummon(LivingEntity owner,String id){
        int left=remaining(owner,id);
        if(left==0)return true;
        tell(owner,"This shadow is recovering. Try again in "+((left+19)/20)+" seconds.");return false;
    }
    public static AbilityUseResult canUse(LivingEntity owner,String id,boolean active){
        if(owner.level.isClientSide){
            if(active)for(LivingEntity entity:owner.level.getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(64))){
                if(entity.isAlive()&&entity instanceof TenShadowsBeast){TenShadowsBeast beast=(TenShadowsBeast)entity;
                    if(beast.isTaming()&&id.equals(beast.shadowId())&&beast.owner()!=null&&beast.owner().getUUID().equals(owner.getUUID()))
                        return AbilityUseResult.fail(new StringTextComponent("Defeat this shadow to complete its taming ritual."));}
            }
            return AbilityUseResult.success();
        }
        int left=remaining(owner,id);
        if(!active&&left>0)return AbilityUseResult.fail(new StringTextComponent("This shadow is recovering. Try again in "+((left+19)/20)+" seconds."));
        if(active&&!tamed(owner,id))return AbilityUseResult.fail(new StringTextComponent("Defeat this shadow to complete its taming ritual."));
        return AbilityUseResult.success();
    }
    public static void prepare(LivingEntity entity){
        TenShadowsBeast beast=(TenShadowsBeast)entity;
        beast.setTaming(!tamed(beast.owner(),beast.shadowId()));
    }
    public static void started(LivingEntity entity){
        TenShadowsBeast beast=(TenShadowsBeast)entity;
        if(beast.isTaming()){
            beast.owner().getPersistentData().remove(reserveTag(beast.shadowId()));
            tell(beast.owner(),"Taming ritual: defeat "+title(beast.shadowId())+" to earn its loyalty.");
        }
    }
    private static boolean credited(LivingEntity owner,Entity killer){
        for(int depth=0;killer instanceof ProjectileEntity&&depth<4;depth++)killer=((ProjectileEntity)killer).getOwner();
        if(killer==owner)return true;
        if(killer instanceof TenShadowsBeast){TenShadowsBeast beast=(TenShadowsBeast)killer;
            return !beast.isTaming()&&beast.owner()!=null&&owner.getUUID().equals(beast.owner().getUUID());}
        return killer instanceof TameableEntity&&owner.getUUID().equals(((TameableEntity)killer).getOwnerUUID());
    }
    public static void died(LivingEntity entity,DamageSource source){
        if(entity.getPersistentData().getBoolean("TenShadowsDeathResolved"))return;
        entity.getPersistentData().putBoolean("TenShadowsDeathResolved",true);
        TenShadowsBeast beast=(TenShadowsBeast)entity;LivingEntity owner=beast.owner();if(owner==null)return;
        String id=beast.shadowId();CompoundNBT state=state(owner,id);
        owner.getPersistentData().remove(reserveTag(id));
        Entity killer=source.getEntity()!=null?source.getEntity():entity.getKillCredit();
        boolean victory=beast.isTaming()&&credited(owner,killer);
        state.putBoolean("Tamed",victory);
        if(victory){
            state.remove("CooldownUntil");owner.getPersistentData().remove(defeatedTag(id));
            tell(owner,title(id)+" has been tamed. You can now summon it as an ally.");
        }else{
            state.putLong("CooldownUntil",time(owner)+DEATH_COOLDOWN);
            owner.getPersistentData().putBoolean(defeatedTag(id),true);
            tell(owner,title(id)+" was defeated. It must be tamed again after its 20-minute cooldown.");
        }
    }
    public static void clonePlayer(PlayerEvent.Clone event){
        CompoundNBT original=event.getOriginal().getPersistentData();
        if(original.contains(ROOT,10))event.getPlayer().getPersistentData().put(ROOT,original.getCompound(ROOT).copy());
    }
    private static void tell(LivingEntity owner,String text){
        if(owner instanceof ServerPlayerEntity)((ServerPlayerEntity)owner).displayClientMessage(new StringTextComponent(text),false);
    }
    private static ShikigamiKind kind(String id){for(ShikigamiKind k:ShikigamiKind.values())if(k.id.equals(id))return k;throw new IllegalArgumentException(id);}
    private static String title(String id){return id.equals("mahoraga")?"Mahoraga":id.equals("agito")?"Agito":kind(id).title;}
    private static String reserveTag(String id){return id.equals("mahoraga")?MahoragaFeatures.RESERVE_TAG:id.equals("agito")?AgitoFeatures.RESERVE_TAG:kind(id).reserveTag();}
    private static String defeatedTag(String id){return id.equals("mahoraga")?MahoragaFeatures.DEFEATED_TAG:id.equals("agito")?AgitoFeatures.DEFEATED_TAG:kind(id).defeatedTag();}
    private TenShadowsTaming(){}
}
