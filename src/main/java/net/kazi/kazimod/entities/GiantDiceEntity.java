package net.kazi.kazimod.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.AxisAlignedBB;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.kazi.kazimod.init.KaziEntities;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

import java.util.List;
import java.util.UUID;

public class GiantDiceEntity extends Entity {

    private static final int    HOVER_TICKS   = 60;
    private static final float  STOMP_DAMAGE  = 80.0f;
    private static final double HOVER_HEIGHT  = 10.0;
    private static final double STOMP_RADIUS  = 4.0;
    private static final int    STUN_TICKS    = 60;

    private UUID    ownerUUID;
    private UUID    targetUUID;
    private int     ticksAlive = 0;
    private boolean stomped    = false;
    private boolean descending = false;

    public GiantDiceEntity(EntityType<? extends GiantDiceEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
    }

    public GiantDiceEntity(World world, LivingEntity owner, LivingEntity target) {
        this(KaziEntities.GIANT_DICE.get(), world);
        this.ownerUUID  = owner.getUUID();
        this.targetUUID = target.getUUID();
        this.moveTo(target.getX(), target.getY() + HOVER_HEIGHT, target.getZ(), 0, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) return;
        ticksAlive++;

        if (stomped) {
            if (ticksAlive > HOVER_TICKS + 20) this.remove();
            return;
        }

        LivingEntity target = findByUUID(targetUUID);
        if (target == null || !target.isAlive()) { this.remove(); return; }

        if (!descending) {
            // Hover and track
            double lx = this.getX() + (target.getX() - this.getX()) * 0.25;
            double ly = this.getY() + ((target.getY() + HOVER_HEIGHT) - this.getY()) * 0.25;
            double lz = this.getZ() + (target.getZ() - this.getZ()) * 0.25;
            this.moveTo(lx, ly, lz);
            // No particle — renderer handles visuals
            if (ticksAlive >= HOVER_TICKS) descending = true;
        } else {
            double newY = this.getY() - 2.0;
            if (newY <= target.getY()) {
                doStomp(target);
            } else {
                this.moveTo(this.getX(), newY, this.getZ());
            }
        }
    }

    private void doStomp(LivingEntity primaryTarget) {
        stomped = true;
        LivingEntity owner = findByUUID(ownerUUID);
        List<LivingEntity> victims = this.level.getEntitiesOfClass(
                LivingEntity.class,
                new AxisAlignedBB(
                        this.getX()-STOMP_RADIUS, this.getY()-1, this.getZ()-STOMP_RADIUS,
                        this.getX()+STOMP_RADIUS, this.getY()+4,  this.getZ()+STOMP_RADIUS),
                e -> e != owner && e.isAlive()
        );
        for (LivingEntity v : victims) {
            net.minecraft.util.DamageSource src = owner != null
                    ? AbilityDamageSource.causeAbilityDamage(owner, CasinoRollAbility.INSTANCE).bypassArmor()
                    : net.minecraft.util.DamageSource.MAGIC;
            v.hurt(src, STOMP_DAMAGE);
            v.addEffect(new EffectInstance(ModEffects.DIZZY.get(), STUN_TICKS, 0, false, true));
            v.setDeltaMovement(v.getDeltaMovement().x, 0.7, v.getDeltaMovement().z);
        }
    }

    private LivingEntity findByUUID(UUID uuid) {
        if (uuid == null) return null;
        for (LivingEntity e : this.level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(this.getX()-64, this.getY()-64, this.getZ()-64,
                        this.getX()+64, this.getY()+64, this.getZ()+64))) {
            if (uuid.equals(e.getUUID())) return e;
        }
        return null;
    }

    public boolean isDescending() { return descending; }

    @Override protected void defineSynchedData() {}

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        ticksAlive  = nbt.getInt("TicksAlive");
        stomped     = nbt.getBoolean("Stomped");
        descending  = nbt.getBoolean("Descending");
        if (nbt.hasUUID("OwnerUUID"))  ownerUUID  = nbt.getUUID("OwnerUUID");
        if (nbt.hasUUID("TargetUUID")) targetUUID = nbt.getUUID("TargetUUID");
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        nbt.putInt("TicksAlive", ticksAlive);
        nbt.putBoolean("Stomped", stomped);
        nbt.putBoolean("Descending", descending);
        if (ownerUUID  != null) nbt.putUUID("OwnerUUID",  ownerUUID);
        if (targetUUID != null) nbt.putUUID("TargetUUID", targetUUID);
    }
}