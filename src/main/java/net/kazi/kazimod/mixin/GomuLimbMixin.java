package net.kazi.kazimod.mixin;

import net.kazi.kazimod.gomu.GomuLimbData;
import net.kazi.kazimod.gomu.GomuLimbs;
import net.minecraft.entity.Entity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

@Mixin(value = AbilityProjectileEntity.class, remap = false, priority = 900)
public abstract class GomuLimbMixin implements GomuLimbData {
    @Unique private static final DataParameter<Boolean> KAZI_GATLING = EntityDataManager.defineId(AbilityProjectileEntity.class, DataSerializers.BOOLEAN);
    @Override public boolean kazi$gatling() { return kazi$data().get(KAZI_GATLING); }
    @Override public void kazi$gatling(boolean gatling) { kazi$data().set(KAZI_GATLING, gatling); }
    @Unique private static final DataParameter<Integer> KAZI_ARM = EntityDataManager.defineId(AbilityProjectileEntity.class, DataSerializers.INT);
    @Unique private static final DataParameter<Float> KAZI_REACH = EntityDataManager.defineId(AbilityProjectileEntity.class, DataSerializers.FLOAT);
    @Unique private static final DataParameter<Float> KAZI_SPEED = EntityDataManager.defineId(AbilityProjectileEntity.class, DataSerializers.FLOAT);
    @Unique private static final DataParameter<Float> KAZI_SPREAD_X = EntityDataManager.defineId(AbilityProjectileEntity.class, DataSerializers.FLOAT);
    @Unique private static final DataParameter<Float> KAZI_SPREAD_Y = EntityDataManager.defineId(AbilityProjectileEntity.class, DataSerializers.FLOAT);
    @Unique private EntityDataManager kazi$data() { return ((AbilityProjectileEntity)(Object)this).getEntityData(); }
    @Override public int kazi$arm() { return kazi$data().get(KAZI_ARM); }
    @Override public void kazi$arm(int side) { kazi$data().set(KAZI_ARM, side); }
    @Override public float kazi$reach() { return kazi$data().get(KAZI_REACH); }
    @Override public void kazi$reach(float reach) { kazi$data().set(KAZI_REACH, reach); }
    @Override public float kazi$speed() { return kazi$data().get(KAZI_SPEED); }
    @Override public void kazi$speed(float speed) { kazi$data().set(KAZI_SPEED, speed); }
    @Override public float kazi$spreadX() { return kazi$data().get(KAZI_SPREAD_X); }
    @Override public float kazi$spreadY() { return kazi$data().get(KAZI_SPREAD_Y); }
    @Override public void kazi$spread(float x, float y) { kazi$data().set(KAZI_SPREAD_X, x); kazi$data().set(KAZI_SPREAD_Y, y); }

    @Inject(method = "defineSynchedData", at = @At("TAIL"), remap = true)
    private void kazi$defineLimbs(CallbackInfo ci) {
        kazi$data().define(KAZI_GATLING, false);
        kazi$data().define(KAZI_ARM, 0); kazi$data().define(KAZI_REACH, 0F); kazi$data().define(KAZI_SPEED, 0F);
        kazi$data().define(KAZI_SPREAD_X, 0F); kazi$data().define(KAZI_SPREAD_Y, 0F);
    }

    @Inject(method = "shootFromRotation(Lnet/minecraft/entity/Entity;FFFFF)V", at = @At("TAIL"), remap = true)
    private void kazi$launchLimb(Entity owner, float pitch, float yaw, float roll, float speed, float spread, CallbackInfo ci) {
        GomuLimbs.launch((AbilityProjectileEntity)(Object)this);
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = true, cancellable = true)
    private void kazi$aimLimb(CallbackInfo ci) {
        GomuLimbs.steer((AbilityProjectileEntity)(Object)this);
        if (kazi$arm() != 0 && ((AbilityProjectileEntity)(Object)this).removed) ci.cancel();
    }
}
