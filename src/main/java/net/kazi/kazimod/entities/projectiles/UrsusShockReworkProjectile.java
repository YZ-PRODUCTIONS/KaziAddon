package net.kazi.kazimod.entities.projectiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.damagesource.ModIndirectEntityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.AirBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.CoreBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.FoliageBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.OreBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.IFlexibleSizeProjectile;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class UrsusShockReworkProjectile extends AbilityProjectileEntity implements IFlexibleSizeProjectile {

    public static final Map<UUID, UrsusShockReworkProjectile> ACTIVE_PROJECTILES = new HashMap<>();

    private static final DataParameter<Float>   SIZE     = EntityDataManager.defineId(UrsusShockReworkProjectile.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> FINISHED = EntityDataManager.defineId(UrsusShockReworkProjectile.class, DataSerializers.BOOLEAN);

    private static final BlockProtectionRule GRIEF_RULE =
            (new BlockProtectionRule.Builder(new BlockProtectionRule[]{
                    AirBlockProtectionRule.INSTANCE,
                    CoreBlockProtectionRule.INSTANCE,
                    FoliageBlockProtectionRule.INSTANCE,
                    OreBlockProtectionRule.INSTANCE
            })).build();

    public float multiplier = 0.0F;
    private int ticksInAir  = 0;

    public UrsusShockReworkProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public UrsusShockReworkProjectile(World world, LivingEntity player) {
        super((EntityType) NikyuReworkProjectiles.URSUS_SHOCK.get(), world, player,
                net.kazi.kazimod.abilities.NikyuRework.UrsusShockRework.INSTANCE);
        this.setDamage(9.0F);
        this.setMaxLife(400);
        this.setArmorPiercing(1.0F);
        this.setCanGetStuckInGround();
        this.onBlockImpactEvent = this::onBlockImpactEvent;
    }

    @Override
    public void tick() {
        super.tick();
        this.noCulling = true;

        if (!this.level.isClientSide) {
            if (getThrower() != null && this.isAlive()) {
                ACTIVE_PROJECTILES.put(getThrower().getUUID(), this);
            }
            if (!this.isFinished()) {
                ticksInAir++;
            }
        }

        if (this.isFinished()) {
            this.setSize(Math.min(this.getSize() + 0.5F, 50.0F));
            AbilityHelper.setDeltaMovement(this, 0.0F, 0.0F, 0.0F);
            this.teleportTo(this.position().x, this.position().y, this.position().z);
            this.setRot(90.0F, 0.0F);
            this.xRotO = this.xRot;
            this.yRotO = this.yRot;
        }
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide && getThrower() != null) {
            ACTIVE_PROJECTILES.remove(getThrower().getUUID());
        }
        super.remove();
    }

    /** Manual detonation from the ability's second use. */
    public void detonate() {
        if (!this.isFinished()) {
            doExplosion();
        }
    }

    private void onBlockImpactEvent(BlockPos hit) {
        // Always auto-detonate on block/ground contact
        if (!this.isFinished()) {
            doExplosion();
        }
    }

    private void doExplosion() {
        this.level.playSound((PlayerEntity) null, this.blockPosition(),
                (SoundEvent) ModSounds.PAD_HO_SFX.get(), SoundCategory.PLAYERS, 10.0F, 0.25F);

        AbilityHelper.createSphere(this.level, this.blockPosition(), 55, 5, false, Blocks.AIR, 2, GRIEF_RULE);

        List<LivingEntity> damageList = WyHelper.getNearbyLiving(
                this.position(), this.level, 13.75,
                ModEntityPredicates.getEnemyFactions(this.getThrower()));
        List<LivingEntity> knockbackList = WyHelper.getNearbyLiving(
                this.position(), this.level, 22.0,
                ModEntityPredicates.getEnemyFactions(this.getThrower()));

        ModDamageSource shockwaveSource = (ModDamageSource) (new ModIndirectEntityDamageSource(
                super.getDamageSource().msgId, this, super.getThrower()))
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setHakiNature(SourceHakiNature.IMBUING)
                .setSourceTypes(new ArrayList<>(Arrays.asList(SourceType.INTERNAL)))
                .setUnavoidable()
                .setPiercing(1.0F);

        for (LivingEntity target : damageList) {
            target.hurtTime = target.invulnerableTime = 0;
            target.hurt(shockwaveSource, 51.0F);
        }

        for (LivingEntity target : knockbackList) {
            Vector3d speed = target.getLookAngle()
                    .multiply(-1.0, -1.0, -1.0)
                    .multiply(1.0, 0.0, 1.0);
            AbilityHelper.setDeltaMovement(target, speed.x, 0.25, speed.z);
        }

        if (getThrower() != null) {
            net.kazi.kazimod.abilities.NikyuRework.UrsusShockRework.triggerCooldownForEntity(getThrower());
        }

        this.setFinished();
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SIZE, 0.0F);
        this.entityData.define(FINISHED, false);
    }

    @Override public void setSize(float size) { this.entityData.set(SIZE, size); }
    @Override public float getSize()          { return this.entityData.get(SIZE); }

    public boolean isFinished() { return this.entityData.get(FINISHED); }
    public void setFinished()   { this.entityData.set(FINISHED, true); }
}
