package net.kazi.kazimod.mammoth;

import net.minecraft.entity.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public final class MammothWindEntity extends AbilityProjectileEntity {
    public MammothWindEntity(EntityType<?> type,World world){super(type,world);}
    public MammothWindEntity(World world,LivingEntity user){
        super(MammothFeatures.WIND.get(),world,user,MammothFeatures.CORES.get(MammothAbility.Move.SWEEP));
        setDamage(MammothCombat.full(user)?20:15);setGravity(0);setMaxLife(28);setKnockbackStrength(3);setEntityCollisionSize(2.5);
    }
    @Override public void tick(){if(!level.isClientSide&&!level.hasChunkAt(blockPosition())){remove();return;}super.tick();}
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(4);}
}
