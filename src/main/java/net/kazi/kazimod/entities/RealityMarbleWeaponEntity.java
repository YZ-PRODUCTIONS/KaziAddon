package net.kazi.kazimod.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * Reality Marble's embedded weapon projectile, based on Mahou Tsukai's
 * WeaponProjectileEntity. It is visual-only and can never be picked up.
 */
public class RealityMarbleWeaponEntity extends ArrowEntity {
    private static final DataParameter<ItemStack> WEAPON =
            EntityDataManager.defineId(RealityMarbleWeaponEntity.class, DataSerializers.ITEM_STACK);
    private static final DataParameter<Float> Y_ROTATION =
            EntityDataManager.defineId(RealityMarbleWeaponEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> Z_ROTATION =
            EntityDataManager.defineId(RealityMarbleWeaponEntity.class, DataSerializers.FLOAT);

    public RealityMarbleWeaponEntity(EntityType<? extends RealityMarbleWeaponEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
        this.pickup = AbstractArrowEntity.PickupStatus.DISALLOWED;
        this.setDeltaMovement(Vector3d.ZERO);
    }

    public void setWeapon(ItemStack stack) {
        this.getEntityData().set(WEAPON, stack.copy());
    }

    public ItemStack getWeapon() {
        return this.getEntityData().get(WEAPON);
    }

    public void setYRotation(float rotation) {
        this.getEntityData().set(Y_ROTATION, rotation);
    }

    public float getYRotation() {
        return this.getEntityData().get(Y_ROTATION);
    }

    public void setZRotation(float rotation) {
        this.getEntityData().set(Z_ROTATION, rotation);
    }

    public float getZRotation() {
        return this.getEntityData().get(Z_ROTATION);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(WEAPON, ItemStack.EMPTY);
        this.getEntityData().define(Y_ROTATION, 0.0F);
        this.getEntityData().define(Z_ROTATION, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vector3d.ZERO);
        this.setNoGravity(true);
        this.pickup = AbstractArrowEntity.PickupStatus.DISALLOWED;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void playerTouch(PlayerEntity player) {
        // Visual-only projectile: deliberately bypass all AbstractArrow pickup logic.
    }
}
