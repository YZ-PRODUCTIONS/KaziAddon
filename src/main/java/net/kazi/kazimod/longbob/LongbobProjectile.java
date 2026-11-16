package net.kazi.kazimod.longbob;

import net.kazi.kazimod.doku.DokuGasEntity;
import net.minecraft.entity.*;
import net.minecraft.network.datasync.*;
import net.minecraft.potion.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

public final class LongbobProjectile extends AbilityProjectileEntity {
    public static final int HYDRA=0,CHLORO=1;
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(LongbobProjectile.class,DataSerializers.INT);
    private boolean spent;
    private DokuGasEntity trail;
    public LongbobProjectile(EntityType<?> type,World world){super(type,world);}
    public LongbobProjectile(LongbobEntity boss,int kind){
        super(LongbobFeatures.PROJECTILE.get(),boss.level,boss,SourceElement.POISON,SourceHakiNature.SPECIAL,SourceType.PROJECTILE);
        entityData.set(KIND,kind);ModDamageSource source=boss.source(kind==HYDRA?"hydra":"chloro_ball",true).setProjectile();
        setDamage(boss.infused(source,kind==HYDRA?20:12));setDamageSource(source);setProjectile();setGravity(0);setMaxLife(kind==HYDRA?50:70);
        setEntityCollisionSize(kind==HYDRA?1.5:.7);setBlockCollisionSize(.4);
        if(kind==HYDRA)setPassThroughEntities();
        onEntityImpactEvent=target->{LongbobEntity.poison(target,kind==HYDRA?400:240,kind==HYDRA?1:0);
            if(kind==HYDRA){target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,160,1,false,false));target.addEffect(new EffectInstance(Effects.CONFUSION,160,0,false,false));}
            else cloud();};
        onBlockImpactEvent=pos->{if(kind==CHLORO)cloud();};
    }
    @Override public void defineSynchedData(){super.defineSynchedData();entityData.define(KIND,HYDRA);}
    public int kind(){return entityData.get(KIND);}
    @Override public void tick(){super.tick();if(!level.isClientSide&&isAlive())trail=DokuGasEntity.refresh(trail,this,DokuGasEntity.PROJECTILE,kind()==HYDRA?1.8F:1.1F,false,true).green(true);}
    private void cloud(){if(spent||level.isClientSide)return;spent=true;LivingEntity owner=getThrower();if(owner!=null)LongbobCloud.spawn(owner,position(),6,100);if(trail!=null)trail.finish();}
    @Override public void remove(){if(trail!=null)trail.finish();super.remove();}
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return super.getBoundingBoxForCulling().inflate(6);}
}
