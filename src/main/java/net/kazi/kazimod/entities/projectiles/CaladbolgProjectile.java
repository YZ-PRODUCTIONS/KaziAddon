package net.kazi.kazimod.entities.projectiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kazi.kazimod.abilities.SupaRework.ProjectionAbility;
import net.kazi.kazimod.entities.CaladbolgImpactEntity;
import net.kazi.kazimod.entities.GaeBolgVfxEntity;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.ModIndirectEntityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.IFlexibleSizeProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Projection's independent clone of the Kazi Galaxy Impact projectile. */
public class CaladbolgProjectile extends AbilityProjectileEntity implements IFlexibleSizeProjectile {
    private static final float DAMAGE = 52.0F;
    private static final double DAMAGE_RADIUS = 15.469D;
    private static final double KNOCKBACK_RADIUS = 27.5D;
    private static final DataParameter<Float> SIZE =
            EntityDataManager.defineId(CaladbolgProjectile.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> FINISHED =
            EntityDataManager.defineId(CaladbolgProjectile.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> VISUALS =
            EntityDataManager.defineId(CaladbolgProjectile.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> CRIMSON =
            EntityDataManager.defineId(CaladbolgProjectile.class, DataSerializers.BOOLEAN);

    public CaladbolgProjectile(EntityType<? extends CaladbolgProjectile> type, World world) {
        super(type, world);
    }

    public CaladbolgProjectile(World world, LivingEntity owner) {
        this(world, owner, ProjectionAbility.INSTANCE, true);
        this.setDamage(26.0F);
    }

    /** Reuses the combat mechanics without requiring Caladbolg's presentation. */
    public CaladbolgProjectile(World world, LivingEntity owner, AbilityCore<? extends IAbility> ability, boolean visuals) {
        super(KaziEntities.CALADBOLG.get(), world, owner, ability,
                ProjectionAbility.INSTANCE.getSourceElement(), ProjectionAbility.INSTANCE.getSourceHakiNature(),
                ProjectionAbility.INSTANCE.getSourceTypes());
        this.entityData.set(VISUALS, visuals);
        this.setDamage(DAMAGE);
        this.setMaxLife(200);
        this.setArmorPiercing(1.0F);
        this.setCanGetStuckInGround();
        this.onBlockImpactEvent = this::onBlockImpact;
    }

    private void onBlockImpact(BlockPos hit) {
        if (this.level.isClientSide || this.isFinished()) {
            return;
        }

        // Spawn once on the server. Rendering and electrical branches stay client-only.
        this.setFinished();
        if (hasCrimsonVisuals()) {
            GaeBolgVfxEntity impact = new GaeBolgVfxEntity(KaziEntities.GAE_BOLG_VFX.get(), this.level);
            impact.beginImpact(position(), getDeltaMovement());
            this.level.addFreshEntity(impact);
        } else if (hasVisuals()) {
            CaladbolgImpactEntity impact = new CaladbolgImpactEntity(KaziEntities.CALADBOLG_IMPACT.get(), this.level);
            impact.setPos(this.getX(), this.getY(), this.getZ());
            this.level.addFreshEntity(impact);
        }

        List<LivingEntity> damageTargets = WyHelper.getNearbyLiving(
                this.position(), this.level, DAMAGE_RADIUS,
                ModEntityPredicates.getEnemyFactions(this.getThrower()));
        List<LivingEntity> knockbackTargets = WyHelper.getNearbyLiving(
                this.position(), this.level, KNOCKBACK_RADIUS,
                ModEntityPredicates.getEnemyFactions(this.getThrower()));

        ModDamageSource damageSource = new ModIndirectEntityDamageSource(
                super.getDamageSource().msgId, this, super.getThrower())
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setHakiNature(SourceHakiNature.IMBUING)
                .setSourceTypes(new ArrayList<>(Arrays.asList(SourceType.INTERNAL)))
                .setUnavoidable()
                .setPiercing(1.0F);

        for (LivingEntity target : damageTargets) {
            AbilityHelper.disableAbilities(target, 100, candidate ->
                    candidate.hasComponent(ModAbilityKeys.POOL)
                            && ((PoolComponent) candidate.getComponent(ModAbilityKeys.POOL).get())
                            .containsPool(ModAbilityPools.TEKKAI_LIKE));
            target.hurtTime = 0;
            target.invulnerableTime = 0;
            target.hurt(damageSource, this.getDamage());
        }

        for (LivingEntity target : knockbackTargets) {
            Vector3d velocity = target.getLookAngle()
                    .multiply(-1.0D, -1.0D, -1.0D)
                    .multiply(1.0D, 0.0D, 1.0D);
            AbilityHelper.setDeltaMovement(target, velocity.x, 0.25D, velocity.z);
        }

        this.remove();
    }

    @Override
    public void tick() {
        super.tick();
        this.noCulling = true;
        if (this.isFinished()) {
            this.setSize(Math.min(this.getSize() + 0.5F, 14.0625F));
            AbilityHelper.setDeltaMovement(this, 0.0D, 0.0D, 0.0D);
            this.teleportTo(this.getX(), this.getY(), this.getZ());
            this.setRot(90.0F, 0.0F);
            this.xRotO = this.xRot;
            this.yRotO = this.yRot;
        }
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SIZE, 0.0F);
        this.entityData.define(FINISHED, false);
        this.entityData.define(VISUALS, true);
        this.entityData.define(CRIMSON, false);
    }

    public boolean hasCrimsonVisuals() { return this.entityData.get(CRIMSON); }

    public void setCrimsonVisuals() {
        this.entityData.set(VISUALS, false);
        this.entityData.set(CRIMSON, true);
    }

    public boolean hasVisuals() {
        return this.entityData.get(VISUALS);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeBoolean(hasVisuals());
        buffer.writeBoolean(hasCrimsonVisuals());
    }

    @Override
    public void readSpawnData(PacketBuffer buffer) {
        super.readSpawnData(buffer);
        this.entityData.set(VISUALS, buffer.readBoolean());
        this.entityData.set(CRIMSON, buffer.readBoolean());
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }

    @Override
    public void setSize(float size) {
        this.entityData.set(SIZE, size);
    }

    @Override
    public float getSize() {
        return this.entityData.get(SIZE);
    }

    public boolean isFinished() {
        return this.entityData.get(FINISHED);
    }

    private void setFinished() {
        this.entityData.set(FINISHED, true);
    }
}
