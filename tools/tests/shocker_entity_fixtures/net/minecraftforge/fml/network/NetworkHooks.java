package net.minecraftforge.fml.network;
import net.minecraft.entity.Entity;
import net.minecraft.network.IPacket;
public final class NetworkHooks { public static IPacket<?> getEntitySpawningPacket(Entity entity) { return null; } }
