//
// GalaxyImpactReworkProjectile
// Behavior: fires in aimed direction, detonates on block impact
// Visual: Hiken-style black/dark particles, smaller size
//

package net.kazi.kazimod.entities.projectiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.MrMagicalCart.cartaddon.abilities.brawlerextra.GalaxyImpactAbility;
import net.MrMagicalCart.cartaddon.entities.mobs.marines.GarpEntity;
import net.MrMagicalCart.cartaddon.entities.projectiles.brawlerextra.BrawlerExtraProjectiles;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import java.awt.Color;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
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
import xyz.pixelatedw.mineminenomi.init.*;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GalaxyImpactReworkProjectile extends AbilityProjectileEntity implements IFlexibleSizeProjectile {
    private static final DataParameter<Float> SIZE;
    private static final DataParameter<Boolean> FINISHED;
    private static final BlockProtectionRule GRIEF_RULE;

    public GalaxyImpactReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    private LightningDischargeEntity discharge;

    public GalaxyImpactReworkProjectile(World world, LivingEntity player) {
        super((EntityType) BrawlerExtraProjectiles.GALAXY_IMPACT.get(), world, player, GalaxyImpactAbility.INSTANCE);
        this.setDamage(160.0F);     // Doubled from 80
        this.setMaxLife(200);
        this.setArmorPiercing(1.0F);
        this.setCanGetStuckInGround();
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onTickEvent = this::onTickEvent;

        // Spawn haki lightning discharge that trails the projectile
        if (!world.isClientSide) {
            Color hakiColor = new Color(0, 0, 0); // fallback black
            if (player instanceof PlayerEntity) {
                hakiColor = new Color(HakiHelper.getHaoshokuColour(player));
            }

            this.discharge = new LightningDischargeEntity(player,
                    player.getX(), player.getY() + 1.0F, player.getZ(),
                    player.yRot, player.xRot);
            this.discharge.setAliveTicks(-1);       // keep alive until we kill it
            this.discharge.setUpdateRate(4);
            this.discharge.setLightningLength(3.0F);
            this.discharge.setColor(new Color(0, 0, 0, 100));
            this.discharge.setOutlineColor(hakiColor);
            this.discharge.setRenderTransparent();
            this.discharge.setDetails(16);
            this.discharge.setDensity(16);
            this.discharge.setSize(0.8F);
            this.discharge.setSkipSegments(1);
            world.addFreshEntity(this.discharge);
        }
    }

    // Follow projectile with haki discharge each tick
    private void onTickEvent() {
        if (!this.level.isClientSide && !this.isFinished()) {
            if (this.discharge != null) {
                this.discharge.setPos(this.getX(), this.getY(), this.getZ());
            }
        }
    }

    private void onBlockImpactEvent(BlockPos hit) {
        if (!this.isFinished()) {
            this.level.playSound((PlayerEntity) null, this.blockPosition(),
                    (SoundEvent) ModSounds.GENERIC_EXPLOSION.get(), SoundCategory.PLAYERS, 10.0F, 0.25F);

            // Terrain destruction - halved sphere radius (110 -> 55), Y offset scaled proportionally (-6 -> -3)
            if (this.getThrower() != null &&
                    (this.getThrower() instanceof PlayerEntity || this.getThrower() instanceof GarpEntity)) {
                BlockPos pos = new BlockPos(
                        this.blockPosition().getX(),
                        this.blockPosition().getY() - 3,
                        this.blockPosition().getZ());
                AbilityHelper.createSphere(this.level, pos, 55, 25, false, Blocks.AIR, 2, GRIEF_RULE);
            }

            // Damage in 27.5 block radius (halved from 55)
            List<LivingEntity> damageList = WyHelper.getNearbyLiving(
                    this.position(), this.level, 15.469,
                    ModEntityPredicates.getEnemyFactions(this.getThrower()));

            // Knockback in 55 block radius (halved from 110)
            List<LivingEntity> knockbackList = WyHelper.getNearbyLiving(
                    this.position(), this.level, 55.0,
                    ModEntityPredicates.getEnemyFactions(this.getThrower()));

            ModDamageSource shockwaveSource = (new ModIndirectEntityDamageSource(
                    super.getDamageSource().msgId, this, super.getThrower()))
                    .setSourceElement(SourceElement.SHOCKWAVE)
                    .setHakiNature(SourceHakiNature.IMBUING)
                    .setSourceTypes(new ArrayList(Arrays.asList(SourceType.INTERNAL)))
                    .setUnavoidable()
                    .setPiercing(1.0F);

            for (LivingEntity target : damageList) {
                AbilityHelper.disableAbilities(target, 100, (abl) ->
                        abl.hasComponent(ModAbilityKeys.POOL) &&
                                ((PoolComponent) abl.getComponent(ModAbilityKeys.POOL).get())
                                        .containsPool(ModAbilityPools.TEKKAI_LIKE));
                target.hurtTime = target.invulnerableTime = 0;
                target.hurt(shockwaveSource, this.getDamage());
            }

            for (LivingEntity target : knockbackList) {
                Vector3d speed = target.getLookAngle()
                        .multiply(-1.0, -1.0, -1.0)
                        .multiply(1.0, 0.0, 1.0);
                AbilityHelper.setDeltaMovement(target, speed.x, 0.25, speed.z);
            }

            if (this.discharge != null) {
                this.discharge.setAliveTicks(0);
                this.discharge.remove();
                this.discharge = null;
            }
            this.setFinished();
            this.remove();
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.noCulling = true;

        // Clean up discharge if projectile expired without hitting a block
        if ((this.isFinished() || !this.isAlive()) && this.discharge != null) {
            this.discharge.setAliveTicks(0);
            this.discharge.remove();
            this.discharge = null;
        }

        if (this.isFinished()) {
            this.setSize(Math.min(this.getSize() + 0.5F, 14.0625F));
            AbilityHelper.setDeltaMovement(this, 0.0, 0.0, 0.0);
            this.teleportTo(this.position().x, this.position().y, this.position().z);
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
    }

    @Override
    public void setSize(float size) {
        this.entityData.set(SIZE, size);
    }

    @Override
    public float getSize() {
        return (Float) this.entityData.get(SIZE);
    }

    public boolean isFinished() {
        return (Boolean) this.entityData.get(FINISHED);
    }

    public void setFinished() {
        this.entityData.set(FINISHED, true);
    }

    static {
        SIZE = EntityDataManager.defineId(GalaxyImpactReworkProjectile.class, DataSerializers.FLOAT);
        FINISHED = EntityDataManager.defineId(GalaxyImpactReworkProjectile.class, DataSerializers.BOOLEAN);
        GRIEF_RULE = (new BlockProtectionRule.Builder(new BlockProtectionRule[]{
                AirBlockProtectionRule.INSTANCE,
                CoreBlockProtectionRule.INSTANCE,
                FoliageBlockProtectionRule.INSTANCE,
                OreBlockProtectionRule.INSTANCE
        })).build();
    }
}