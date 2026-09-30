package net.kazi.kazimod.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * A visual-only entity which stores any item stack for its client-side renderer.
 */
public class EmbeddedItemDisplayEntity extends net.minecraft.entity.Entity
        implements IEntityAdditionalSpawnData {
    private static final DataParameter<ItemStack> DISPLAYED_ITEM =
            EntityDataManager.defineId(EmbeddedItemDisplayEntity.class, DataSerializers.ITEM_STACK);

    public EmbeddedItemDisplayEntity(EntityType<? extends EmbeddedItemDisplayEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public void setDisplayedItem(ItemStack stack) {
        this.getEntityData().set(DISPLAYED_ITEM, stack.copy());
    }

    public ItemStack getDisplayedItem() {
        return this.getEntityData().get(DISPLAYED_ITEM);
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(DISPLAYED_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {
        if (tag.contains("DisplayedItem")) {
            this.setDisplayedItem(ItemStack.of(tag.getCompound("DisplayedItem")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) {
        ItemStack stack = this.getDisplayedItem();
        if (!stack.isEmpty()) tag.put("DisplayedItem", stack.save(new CompoundNBT()));
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeItem(this.getDisplayedItem());
    }

    @Override
    public void readSpawnData(PacketBuffer buffer) {
        this.setDisplayedItem(buffer.readItem());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}
