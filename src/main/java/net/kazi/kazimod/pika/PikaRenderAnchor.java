package net.kazi.kazimod.pika;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public final class PikaRenderAnchor extends Entity {
    public PikaRenderAnchor(EntityType<? extends PikaRenderAnchor> type,World world){super(type,world);noPhysics=true;noCulling=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){}
    @Override protected void readAdditionalSaveData(CompoundNBT data){}
    @Override protected void addAdditionalSaveData(CompoundNBT data){}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return true;}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
