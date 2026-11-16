package net.kazi.kazimod.yami;

import net.kazi.kazimod.abilities.YamiRework.LiberationRework;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.network.datasync.*;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class LiberationBlockProjectile extends AbilityProjectileEntity {
    private static final DataParameter<Integer> BLOCK=EntityDataManager.defineId(LiberationBlockProjectile.class,DataSerializers.INT);
    public LiberationBlockProjectile(EntityType<?> type,World world){super((EntityType)type,world);configure();}
    public LiberationBlockProjectile(World world,LivingEntity owner) {
        super((EntityType)KaziEntities.LIBERATION_BLOCK.get(),world,owner,LiberationRework.INSTANCE);configure();
    }
    private void configure() {
        setDamage(5);setMaxLife(35);setGravity(.015F);setEntityCollisionSize(.65F);setBlockCollisionSize(.35F);
        setHurtTime(2);
        onEntityImpactEvent=target->{};
        onBlockImpactEvent=position->{};
    }
    public void setBlock(BlockState state){entityData.set(BLOCK,Block.getId(state));}
    public BlockState block(){return Block.stateById(entityData.get(BLOCK));}
    @Override public void defineSynchedData(){super.defineSynchedData();entityData.define(BLOCK,Block.getId(Blocks.STONE.defaultBlockState()));}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<96*96;}
}
