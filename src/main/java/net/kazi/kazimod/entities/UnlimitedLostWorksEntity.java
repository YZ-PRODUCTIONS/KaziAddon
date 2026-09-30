package net.kazi.kazimod.entities;

import net.kazi.kazimod.abilities.SupaRework.RealityMarbleAbility;
import net.kazi.kazimod.abilities.SupaRework.UnlimitedLostWorksAttack;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/** One synchronized target-bound sequence draws three gears and strikes with each piercing ray. */
public class UnlimitedLostWorksEntity extends Entity {
    public static final int GEAR_COUNT = 3;
    public static final int GEAR_START_TICKS = 12 * 20;
    public static final int MARK_DELAY_TICKS = 15 * 20;
    public static final int FORMATION_TICKS = 18;
    public static final int PIERCE_START_TICKS = MARK_DELAY_TICKS;
    public static final int RAY_COUNT = UnlimitedLostWorksAttack.HIT_COUNT;
    public static final int RAY_INTERVAL_TICKS = 5;
    public static final int RAY_GROW_TICKS = 3;
    public static final int RAY_SEQUENCE_END_TICKS = PIERCE_START_TICKS + RAY_COUNT * RAY_INTERVAL_TICKS;
    public static final int IMPACT_TICKS = MARK_DELAY_TICKS;
    public static final int FADE_START_TICKS = RAY_SEQUENCE_END_TICKS + 22;
    public static final int LIFETIME_TICKS = FADE_START_TICKS + 16;
    private static final DataParameter<Integer> TARGET = EntityDataManager.defineId(UnlimitedLostWorksEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> START = EntityDataManager.defineId(UnlimitedLostWorksEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> HEADING = EntityDataManager.defineId(UnlimitedLostWorksEntity.class, DataSerializers.FLOAT);
    private LivingEntity caster;
    private LivingEntity target;
    private RealityMarbleAbility ability;
    private boolean openingApplied;
    private boolean markSoundPlayed;
    private int nextHitIndex;
    private boolean hitLanded;

    public UnlimitedLostWorksEntity(EntityType<? extends UnlimitedLostWorksEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        setNoGravity(true);
    }

    public void begin(LivingEntity caster, LivingEntity target, RealityMarbleAbility ability) {
        this.caster = caster;
        this.target = target;
        this.ability = ability;
        this.entityData.set(TARGET, target.getId());
        this.entityData.set(START, (int) this.level.getGameTime());
        this.entityData.set(HEADING, caster.yRot);
        followTarget();
    }

    private void followTarget() {
        setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
    }

    public LivingEntity getTarget() {
        Entity entity = this.level.getEntity(this.entityData.get(TARGET));
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

    public float getAge(float partialTick) {
        return Math.max(0.0F, (int) this.level.getGameTime() - this.entityData.get(START) + partialTick);
    }

    public float getHeading() { return this.entityData.get(HEADING); }

    /** Called after the mark has spawned and its ability has started successfully. */
    public void playMarkSound() {
        if (markSoundPlayed || this.level.isClientSide || ability == null || !ability.isLostWorksActive(this)
                || !UnlimitedLostWorksAttack.isValidTarget(caster, target) || caster.level != this.level) return;
        markSoundPlayed = true;
        this.level.playSound(null, target.blockPosition(), KaziSounds.UNLIMITED_LOST_WORKS_HIT_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 1.0F);
    }

    public static float smooth(float value) {
        value = Math.max(0.0F, Math.min(1.0F, value));
        return value * value * (3.0F - 2.0F * value);
    }

    public static float opacity(float age) {
        return smooth((age - GEAR_START_TICKS) / FORMATION_TICKS)
                * (1.0F - smooth((age - FADE_START_TICKS) / (LIFETIME_TICKS - FADE_START_TICKS)));
    }

    public static int rayStartTick(int index) {
        return PIERCE_START_TICKS + index * RAY_INTERVAL_TICKS;
    }

    public static float piercingProgress(float age, int index) {
        if (index < 0 || index >= RAY_COUNT) return 0;
        return smooth((age - rayStartTick(index)) / RAY_GROW_TICKS);
    }

    public static float rayOpacity(float age, int index) {
        if (index < 0 || index >= RAY_COUNT) return 0;
        float rayAge = age - rayStartTick(index);
        if (rayAge <= 0 || rayAge >= RAY_INTERVAL_TICKS) return 0;
        // Each ray clears before the next one starts; no accumulated burst of twenty rays.
        return smooth(rayAge) * (1.0F - smooth((rayAge - RAY_GROW_TICKS)
                / (RAY_INTERVAL_TICKS - RAY_GROW_TICKS)));
    }

    private boolean canFinishAfterHit() {
        // A defeated target leaves a cosmetic sequence at its last position until all rays finish.
        return hitLanded && caster != null && caster.isAlive() && target != null
                && !target.isAlive() && target.level == caster.level;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vector3d.ZERO);
        if (this.level.isClientSide) return;
        if ((!UnlimitedLostWorksAttack.isValidTarget(caster, target) && !canFinishAfterHit()) || caster.level != this.level
                || ability == null || !ability.isLostWorksActive(this) || getAge(0) >= LIFETIME_TICKS) {
            remove();
            return;
        }
        followTarget();
        if (getAge(0) < MARK_DELAY_TICKS) return;
        if (!openingApplied) {
            openingApplied = true;
            KamaVfxEntity.dismantle(caster, target.getX(), target.getEyeY(), target.getZ());
            UnlimitedLostWorksAttack.applyOpeningControl(caster, target);
        }
        int rayIndex = ((int) getAge(0) - PIERCE_START_TICKS) / RAY_INTERVAL_TICKS;
        if (rayIndex < RAY_COUNT && rayIndex >= nextHitIndex) {
            // Consume each ray's attempt once, including blocked strikes. Skipped slots
            // are never delivered together as a burst after a pause in entity ticking.
            nextHitIndex = rayIndex + 1;
            if (target.isAlive() && ability.hitLostWorks(caster, target, !hitLanded)) hitLanded = true;
        }
    }

    @Override protected void defineSynchedData() {
        this.entityData.define(TARGET, -1);
        this.entityData.define(START, 0);
        this.entityData.define(HEADING, 0.0F);
    }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
