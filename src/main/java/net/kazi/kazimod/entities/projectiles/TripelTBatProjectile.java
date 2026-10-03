package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.helpers.ItemsHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class TripelTBatProjectile extends AbilityProjectileEntity {

    private ItemStack itemStack = ItemStack.EMPTY;
    private Vector3d lookVec = Vector3d.ZERO;

    public TripelTBatProjectile(EntityType<?> type, World world) {
        super(type, world);
        this.itemStack = ItemStack.EMPTY;
        this.lookVec = Vector3d.ZERO;
    }

    public TripelTBatProjectile(World world, LivingEntity thrower, float damage) {
        super((EntityType<? extends AbilityProjectileEntity>) KaziEntities.TRIPLE_T_BAT_PROJECTILE.get(), world, thrower, (AbilityCore<?>) null);
        this.itemStack = thrower.getMainHandItem().copy();
        this.lookVec = thrower.getLookAngle();
        float itemDamage = ItemsHelper.getItemDamage(this.itemStack);
        this.setDamage(itemDamage / 2.0F + damage);
        this.setMaxLife(13);
        this.setAffectedByImbuing();
        this.setEntityCollisionSize(2.0D, 2.0D, 2.0D);
    }

    private void onHitEntity(LivingEntity entity) {
        Vector3d speed = this.lookVec.normalize().multiply(2.5D, 1.0D, 2.5D).add(0.0D, 0.15D, 0.0D);
        entity.setDeltaMovement(speed);
    }

    public ItemStack getItem() {
        return this.itemStack;
    }

    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeItem(this.itemStack);
    }

    public void readSpawnData(PacketBuffer buffer) {
        super.readSpawnData(buffer);
        this.itemStack = buffer.readItem();
    }
}
