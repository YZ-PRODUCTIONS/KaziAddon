package net.kazi.kazimod.entities;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

public class KamaVfxEntity
extends Entity {
    public static final int SLASH = 0;
    public static final int WEB = 1;
    public static final int FUGA_CHARGE = 2;
    public static final int DISMANTLE = 3;
    public static final int SHRINE_CHARGE = 4;
    private static final DataParameter<Integer> MODE = EntityDataManager.defineId(KamaVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> AGE = EntityDataManager.defineId(KamaVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> LIFE = EntityDataManager.defineId(KamaVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> OWNER = EntityDataManager.defineId(KamaVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> SIZE = EntityDataManager.defineId(KamaVfxEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> PROGRESS = EntityDataManager.defineId(KamaVfxEntity.class, DataSerializers.FLOAT);
    private IAbility ability;
    private int refreshed;

    public KamaVfxEntity(EntityType<? extends KamaVfxEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static KamaVfxEntity spawn(LivingEntity owner, int mode, Vector3d pos, float size, int life) {
        if (owner.level.isClientSide) {
            return null;
        }
        KamaVfxEntity effect = new KamaVfxEntity((EntityType<? extends KamaVfxEntity>)((EntityType)KaziEntities.KAMA_VFX.get()), owner.level);
        effect.entityData.set(MODE, mode);
        effect.entityData.set(SIZE, Float.valueOf(size));
        effect.entityData.set(LIFE, life);
        effect.setPos(pos.x, pos.y, pos.z);
        effect.yRot = owner.yRot;
        effect.xRot = owner.xRot;
        owner.level.addFreshEntity((Entity)effect);
        return effect;
    }

    public static void slash(LivingEntity owner, double x, double y, double z) {
        KamaVfxEntity.spawn(owner, 0, new Vector3d(x, y, z), 5.0f, 8);
    }

    public static void dismantle(LivingEntity owner, double x, double y, double z) {
        KamaVfxEntity.spawn(owner, 3, new Vector3d(x, y, z), 5.0f, 8);
    }

    public static KamaVfxEntity charge(LivingEntity owner, IAbility ability, int life) {
        KamaVfxEntity effect = KamaVfxEntity.spawn(owner, 2, KamaVfxEntity.origin(owner, 1.0f), 1.0f, life);
        if (effect != null) {
            effect.entityData.set(OWNER, owner.getId());
            effect.ability = ability;
        }
        return effect;
    }

    /** A non-living construction preview cannot become an extra combat target. */
    public static KamaVfxEntity shrineCharge(LivingEntity owner, IAbility ability, int duration, float behind) {
        KamaVfxEntity effect = spawn(owner, SHRINE_CHARGE, owner.position(), behind, duration);
        if (effect != null) {
            effect.entityData.set(OWNER, owner.getId());
            effect.ability = ability;
            Vector3d position = effect.visualOrigin(owner, 1.0F);
            effect.setPos(position.x, position.y, position.z);
        }
        return effect;
    }

    public Vector3d visualOrigin(LivingEntity owner, float partial) {
        if (getMode() != SHRINE_CHARGE) return origin(owner, partial);
        // Avoid LivingEntity#getPosition(float): its SRG mapping is absent on
        // some 1.16.5 dedicated-server runtimes, including Arclight.
        double x = owner.xOld + (owner.getX() - owner.xOld) * partial;
        double y = owner.yOld + (owner.getY() - owner.yOld) * partial;
        double z = owner.zOld + (owner.getZ() - owner.zOld) * partial;
        Vector3d position = new Vector3d(x, y, z);
        Vector3d look = owner.getViewVector(partial);
        return position.add(-look.x * getSize(), 0.0D, -look.z * getSize());
    }

    public static Vector3d origin(LivingEntity owner, float partial) {
        return owner.getEyePosition(partial).add(0.0, -0.3, 0.0).add(owner.getViewVector(partial).scale(1.2));
    }

    public void refresh(float progress) {
        this.refreshed = this.tickCount;
        this.entityData.set(PROGRESS, Float.valueOf(progress));
    }

    public LivingEntity getOwner() {
        Entity owner = this.level.getEntity(((Integer)this.entityData.get(OWNER)).intValue());
        return owner instanceof LivingEntity ? (LivingEntity)owner : null;
    }

    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            return;
        }
        if (this.getMode() == FUGA_CHARGE || this.getMode() == SHRINE_CHARGE) {
            LivingEntity owner = this.getOwner();
            if (owner == null || !owner.isAlive() || this.ability == null || this.tickCount - this.refreshed > 5 || this.ability.getComponent(ModAbilityKeys.DISABLE).map(disable -> disable.isDisabled()).orElse(false).booleanValue()) {
                this.remove();
                return;
            }
            Vector3d p = this.visualOrigin(owner, 1.0f);
            this.setPos(p.x, p.y, p.z);
        } else if (this.tickCount >= (Integer)this.entityData.get(LIFE)) {
            this.remove();
            return;
        }
        this.entityData.set(AGE, this.tickCount);
    }

    public int getMode() {
        return (Integer)this.entityData.get(MODE);
    }

    public float getAge(float partial) {
        return (float)((Integer)this.entityData.get(AGE)).intValue() + partial;
    }

    public int getLife() {
        return (Integer)this.entityData.get(LIFE);
    }

    public float getSize() {
        return ((Float)this.entityData.get(SIZE)).floatValue();
    }

    public float getProgress(float partial) {
        return Math.min(1.0f, ((Float)this.entityData.get(PROGRESS)).floatValue() + partial / (float)this.getLife());
    }

    protected void defineSynchedData() {
        this.entityData.define(MODE, 0);
        this.entityData.define(AGE, 0);
        this.entityData.define(LIFE, 8);
        this.entityData.define(OWNER, -1);
        this.entityData.define(SIZE, Float.valueOf(1.0f));
        this.entityData.define(PROGRESS, Float.valueOf(0.0f));
    }

    protected void readAdditionalSaveData(CompoundNBT tag) {
    }

    protected void addAdditionalSaveData(CompoundNBT tag) {
    }

    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }
}
