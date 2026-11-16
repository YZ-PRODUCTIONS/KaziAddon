package net.kazi.kazimod.yami;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/** A single client-local draw anchor puts all cached volumes in the entity render pass. */
public final class YamiRenderAnchor extends Entity {
    public YamiRenderAnchor(EntityType<? extends YamiRenderAnchor> type,World world) {
        super(type,world);noPhysics=true;noCulling=true;setNoGravity(true);
    }
    @Override protected void defineSynchedData(){}
    @Override protected void readAdditionalSaveData(CompoundNBT data){}
    @Override protected void addAdditionalSaveData(CompoundNBT data){}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return true;}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
