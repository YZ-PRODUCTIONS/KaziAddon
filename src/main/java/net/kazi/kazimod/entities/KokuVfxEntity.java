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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

public class KokuVfxEntity
extends Entity {
    public static final int RED_CHARGE = 0;
    public static final int BLUE_CHARGE = 1;
    public static final int PURPLE_CHARGE = 2;
    public static final int RED_IMPACT = 3;
    public static final int PURPLE_IMPACT = 4;
    public static final int BLUE_PULL = 5;
    public static final int NUKE_IMPACT = 6;
    private static final DataParameter<Integer> MODE = EntityDataManager.defineId(KokuVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> OWNER = EntityDataManager.defineId(KokuVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> AGE = EntityDataManager.defineId(KokuVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> DURATION = EntityDataManager.defineId(KokuVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> PROGRESS = EntityDataManager.defineId(KokuVfxEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SIZE = EntityDataManager.defineId(KokuVfxEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> GROUND_OFFSET = EntityDataManager.defineId(KokuVfxEntity.class, DataSerializers.FLOAT);
    private IAbility sourceAbility;
    private int lastRefresh;

    public KokuVfxEntity(EntityType<? extends KokuVfxEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static KokuVfxEntity charge(LivingEntity owner, IAbility ability, int mode, int duration) {
        if (owner.level.isClientSide) {
            return null;
        }
        KokuVfxEntity effect = new KokuVfxEntity((EntityType<? extends KokuVfxEntity>)((EntityType)KaziEntities.KOKU_VFX.get()), owner.level);
        effect.entityData.set(MODE, mode);
        effect.entityData.set(OWNER, owner.getId());
        effect.entityData.set(DURATION, duration);
        effect.sourceAbility = ability;
        effect.follow(owner);
        owner.level.addFreshEntity((Entity)effect);
        return effect;
    }

    public static void impact(World world, Vector3d position, int mode, float radius) {
        if (world.isClientSide) {
            return;
        }
        KokuVfxEntity effect = new KokuVfxEntity((EntityType<? extends KokuVfxEntity>)((EntityType)KaziEntities.KOKU_VFX.get()), world);
        effect.entityData.set(MODE, mode);
        effect.entityData.set(DURATION, mode == NUKE_IMPACT ? 140 : mode == PURPLE_IMPACT ? 32 : 16);
        effect.entityData.set(SIZE, Float.valueOf(radius));
        effect.setPos(position.x, position.y, position.z);
        if (mode == NUKE_IMPACT) {
            int x = net.minecraft.util.math.MathHelper.floor(position.x);
            int z = net.minecraft.util.math.MathHelper.floor(position.z);
            effect.entityData.set(GROUND_OFFSET, (float) (world.getHeight(net.minecraft.world.gen.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - position.y));
        }
        world.addFreshEntity((Entity)effect);
    }

    public void refresh(float progress) {
        this.lastRefresh = this.tickCount;
        this.entityData.set(PROGRESS, Float.valueOf(MathHelper.clamp((float)progress, (float)0.0f, (float)1.0f)));
    }

    public LivingEntity getOwner() {
        Entity owner = this.level.getEntity(((Integer)this.entityData.get(OWNER)).intValue());
        return owner instanceof LivingEntity ? (LivingEntity)owner : null;
    }

    public static Vector3d castOrigin(LivingEntity owner, float partialTicks, int mode) {
        Vector3d look = owner.getViewVector(partialTicks);
        // Avoid MathHelper.rotLerp here: its SRG mapping is absent on some 1.16.5
        // dedicated-server runtimes (notably Arclight). Interpolate the wrapped
        // angle directly so client and server calculate the same cast origin.
        float yawDelta = owner.yRot - owner.yRotO;
        yawDelta = yawDelta - (float)Math.floor((yawDelta + 180.0F) / 360.0F) * 360.0F;
        float yaw = (owner.yRotO + partialTicks * yawDelta) * (float)Math.PI / 180.0f;
        double side = mode == 2 ? 0.0 : 0.22;
        return owner.getEyePosition(partialTicks).add((double)(-MathHelper.cos((float)yaw)) * side, -0.18, (double)(-MathHelper.sin((float)yaw)) * side).add(look.scale(mode == 2 ? 1.8 : 1.2));
    }

    private void follow(LivingEntity owner) {
        Vector3d origin = KokuVfxEntity.castOrigin(owner, 1.0f, this.getMode());
        this.setPos(origin.x, origin.y, origin.z);
        this.yRot = owner.yRot;
        this.xRot = owner.xRot;
    }

    public void tick() {
        super.tick();
        LivingEntity owner = this.getOwner();
        if ((Integer)this.entityData.get(OWNER) >= 0) {
            if (!(this.level.isClientSide || owner != null && owner.isAlive() && this.sourceAbility != null && !this.sourceAbility.getComponent(ModAbilityKeys.DISABLE).map(disable -> disable.isDisabled()).orElse(false).booleanValue() && this.tickCount - this.lastRefresh <= 4)) {
                this.remove();
                return;
            }
            if (owner != null) {
                this.follow(owner);
            }
        } else if (!this.level.isClientSide && this.tickCount >= (Integer)this.entityData.get(DURATION)) {
            this.remove();
            return;
        }
        if (!this.level.isClientSide) {
            this.entityData.set(AGE, this.tickCount);
        }
    }

    public float getNukeAge(float partial) { return 40.0F + getAge(partial); }

    public float getWaveRadius(float partial) {
        return getSize() * (float) Math.pow(MathHelper.clamp(getAge(partial) / 100.0F, 0.0F, 1.0F), 0.7D);
    }

    public float getVfxOpacity(float partial) {
        return MathHelper.clamp((140.0F - getAge(partial)) / 40.0F, 0.0F, 1.0F);
    }

    public float getGroundOffset() { return this.entityData.get(GROUND_OFFSET); }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256.0D * 256.0D; }

    public int getMode() {
        return (Integer)this.entityData.get(MODE);
    }

    public float getAge(float partial) {
        return (float)((Integer)this.entityData.get(AGE)).intValue() + partial;
    }

    public float getSize() {
        return ((Float)this.entityData.get(SIZE)).floatValue();
    }

    public float getProgress(float partial) {
        if ((Integer)this.entityData.get(OWNER) < 0) {
            return MathHelper.clamp((float)(this.getAge(partial) / (float)((Integer)this.entityData.get(DURATION)).intValue()), (float)0.0f, (float)1.0f);
        }
        return MathHelper.clamp((float)(((Float)this.entityData.get(PROGRESS)).floatValue() + partial / (float)((Integer)this.entityData.get(DURATION)).intValue()), (float)0.0f, (float)1.0f);
    }

    protected void defineSynchedData() {
        this.entityData.define(MODE, 0);
        this.entityData.define(OWNER, -1);
        this.entityData.define(AGE, 0);
        this.entityData.define(DURATION, 20);
        this.entityData.define(PROGRESS, Float.valueOf(0.0f));
        this.entityData.define(SIZE, Float.valueOf(1.0f));
        this.entityData.define(GROUND_OFFSET, -1.5F);
    }

    protected void readAdditionalSaveData(CompoundNBT nbt) {
    }

    protected void addAdditionalSaveData(CompoundNBT nbt) {
    }

    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }
}
