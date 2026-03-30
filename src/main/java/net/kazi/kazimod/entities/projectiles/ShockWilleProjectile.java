package net.kazi.kazimod.entities.projectiles;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kazi.kazimod.abilities.OpeRework.ShockWilleRework;
import net.kazi.kazimod.init.KaziEffects;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class ShockWilleProjectile extends AbilityProjectileEntity {

    private static final DataParameter<Float> LENGTH =
            EntityDataManager.defineId(ShockWilleProjectile.class, DataSerializers.FLOAT);

    private final Set<UUID> hitTargets = new HashSet<>();
    private LightningDischargeEntity discharge;
    private final float impactDamage;

    public ShockWilleProjectile(EntityType<? extends ShockWilleProjectile> type, World world) {
        super(type, world);
        this.impactDamage = 10.0F;
    }

    public ShockWilleProjectile(World world, LivingEntity thrower, float damage) {
        super((EntityType<? extends AbilityProjectileEntity>) KaziEntities.SHOCK_WILLE.get(), world, thrower, ShockWilleRework.INSTANCE);
        this.impactDamage = damage;
        this.setDamage(damage);
        this.setMaxLife(100);
        this.setHurtTime(0);
        this.setNoGravity(true);
        this.setPassThroughEntities();
        this.setLength(7.5F);
        this.setUnavoidable();
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onBlockImpactEvent;

        if (!world.isClientSide && thrower != null) {
            this.discharge = new LightningDischargeEntity(thrower, thrower.getX(), thrower.getY() + 1.0D, thrower.getZ(), thrower.yRot, thrower.xRot);
            this.discharge.setAliveTicks(-1);
            this.discharge.setUpdateRate(4);
            this.discharge.setLightningLength(4.5F);
            this.discharge.setColor(new Color(0, 0, 0, 100));
            this.discharge.setOutlineColor(new Color(14276096));
            this.discharge.setRenderTransparent();
            this.discharge.setDetails(16);
            this.discharge.setDensity(14);
            this.discharge.setSize(0.75F);
            this.discharge.setSkipSegments(1);
            world.addFreshEntity(this.discharge);
        }
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LENGTH, 7.5F);
    }

    @Override
    public void tick() {
        super.tick();
        this.noCulling = true;

        steerTowardOwnerAim();

        Vector3d motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            float yaw = (float) (MathHelper.atan2(motion.x, motion.z) * (180.0D / Math.PI));
            float pitch = (float) (MathHelper.atan2(motion.y, horizontal) * (180.0D / Math.PI));
            this.yRotO = this.yRot;
            this.xRotO = this.xRot;
            this.yRot = yaw;
            this.xRot = pitch;
        }

        if (!this.level.isClientSide && this.discharge != null) {
            this.discharge.setPos(this.getX(), this.getY(), this.getZ());
            this.setLength((float) Math.max(7.5D, Math.min(15.0D, this.tickCount * 0.18D + 7.5D)));
        }
    }

    private void steerTowardOwnerAim() {
        LivingEntity thrower = this.getThrower();
        if (thrower == null) {
            return;
        }

        Vector3d current = this.getDeltaMovement();
        double speed = current.length();
        if (speed <= 1.0E-6D) {
            return;
        }

        Vector3d desired = thrower.getLookAngle();
        if (desired.lengthSqr() <= 1.0E-6D) {
            return;
        }

        this.setDeltaMovement(desired.normalize().scale(speed));
        this.hurtMarked = true;
    }

    private void onEntityImpactEvent(LivingEntity target) {
        if (target == null || !target.isAlive() || target == this.getThrower()) {
            return;
        }

        if (!this.hitTargets.add(target.getUUID())) {
            return;
        }

        target.hurtTime = 0;
        target.invulnerableTime = 0;
        target.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 60, 0, false, false));
        target.addEffect(new EffectInstance((Effect) ModEffects.ANTI_KNOCKBACK.get(), 60, 0, false, false));
        target.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN, 160, 0, false, false));
        target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 160, 1, false, false));
        target.addEffect(new EffectInstance((Effect) KaziEffects.WEAKENED_MOVEMENT.get(), 160, 1, false, false));
        target.hurt(this.getDamageSource(), this.impactDamage);
        this.level.playSound(null, this.blockPosition(), net.minecraft.util.SoundEvents.TRIDENT_HIT, SoundCategory.PLAYERS, 1.0F, 1.1F);
    }

    private void onBlockImpactEvent(BlockPos hitPos) {
        this.remove();
    }

    public float getLength() {
        return this.entityData.get(LENGTH);
    }

    public void setLength(float value) {
        this.entityData.set(LENGTH, value);
    }

    @Override
    public void remove() {
        LivingEntity thrower = this.getThrower();
        if (thrower != null) {
            ShockWilleRework ability = getOwnerAbility(thrower);
            if (ability != null) {
                ability.clearProjectileState(thrower, this.getUUID());
            }
        }
        if (this.discharge != null) {
            this.discharge.setAliveTicks(0);
            this.discharge.remove();
            this.discharge = null;
        }
        super.remove();
    }

    private ShockWilleRework getOwnerAbility(LivingEntity thrower) {
        if (thrower == null) {
            return null;
        }

        xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData props =
                xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability.get(thrower);
        if (props == null) {
            return null;
        }

        return (ShockWilleRework) props.getEquippedAbility(ShockWilleRework.INSTANCE);
    }
}
