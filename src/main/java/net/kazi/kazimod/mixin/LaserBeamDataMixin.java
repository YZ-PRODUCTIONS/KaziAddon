package net.kazi.kazimod.mixin;

import net.kazi.kazimod.effects.LaserBeamProfile;
import net.kazi.kazimod.effects.LaserBeamVisualData;
import net.minecraft.entity.EntityType;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

/** The custom JAR's entity-data synchronization for laser classification and width. */
@Mixin(value = LightningEntity.class, remap = false)
public abstract class LaserBeamDataMixin extends AbilityProjectileEntity implements LaserBeamVisualData, net.kazi.kazimod.goro.GoroBoltVisuals {
    @Unique private static final DataParameter<Integer> KAZI_GORO_KIND =
            EntityDataManager.defineId(LightningEntity.class, DataSerializers.INT);
    @Override public int kazi$goroKind() { return entityData.get(KAZI_GORO_KIND); }
    @Override public void kazi$goroKind(int kind) { entityData.set(KAZI_GORO_KIND, kind); }
    @Unique
    private static final DataParameter<Float> KAZI_BEAM_DAMAGE =
            EntityDataManager.defineId(LightningEntity.class, DataSerializers.FLOAT);
    @Unique
    private static final DataParameter<Boolean> KAZI_IS_LASER =
            EntityDataManager.defineId(LightningEntity.class, DataSerializers.BOOLEAN);
    @Unique
    private static final DataParameter<Boolean> KAZI_IS_KAMEHAMEHA =
            EntityDataManager.defineId(LightningEntity.class, DataSerializers.BOOLEAN);

    protected LaserBeamDataMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(KAZI_GORO_KIND, 0);
        this.entityData.define(KAZI_BEAM_DAMAGE, 0.0F);
        this.entityData.define(KAZI_IS_LASER, false);
        this.entityData.define(KAZI_IS_KAMEHAMEHA, false);
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = true)
    private void kazimod$syncBeamVisuals(CallbackInfo ci) {
        if (this.level.isClientSide) {
            if(kazimod$isKamehameha())this.noCulling=true;
            return;
        }
        LightningEntity beam = (LightningEntity) (Object) this;
        net.kazi.kazimod.goro.GoroVisuals.mark(beam);
        this.entityData.set(KAZI_BEAM_DAMAGE,
                Float.isFinite(this.getDamage()) ? Math.max(0.0F, this.getDamage()) : 0.0F);
        this.entityData.set(KAZI_IS_LASER, LaserBeamProfile.isLaser(
                this.getParent() == null ? "" : this.getParent().getId(),
                beam.getSegments(), beam.getBranches(), beam.getMimicVanilla()));
        this.entityData.set(KAZI_IS_KAMEHAMEHA,
                this.getParent()==net.kazi.kazimod.saiyan.SaiyanAbility.core(net.kazi.kazimod.saiyan.SaiyanAbility.Move.KAMEHAMEHA));
    }

    @Override
    public float kazimod$getBeamDamage() {
        return this.entityData.get(KAZI_BEAM_DAMAGE);
    }

    @Override
    public boolean kazimod$isLaser() {
        return this.entityData.get(KAZI_IS_LASER);
    }

    @Override
    public boolean kazimod$isKamehameha() {
        return this.entityData.get(KAZI_IS_KAMEHAMEHA);
    }
}
