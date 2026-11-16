package net.kazi.kazimod.entities;

import java.util.UUID;
import net.kazi.kazimod.abilities.NagiAwakening.DragonKind;
import net.kazi.kazimod.abilities.NagiAwakening.DragonStrikeAbility;
import net.kazi.kazimod.abilities.NagiAwakening.DragonSpecial;
import net.kazi.kazimod.abilities.NagiAwakening.DragonSpecialAttacks;
import net.kazi.kazimod.abilities.NagiAwakening.DragonSpecialCycle;
import net.kazi.kazimod.abilities.NagiAwakening.DragonVfxPhase;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** A real, independently targetable flying summon. All decisions and damage run on the server. */
public final class DragonStrikeEntity extends FlyingEntity {
    // Let dragons orbit/retreat around prey at the edge of the caster's hunting radius.
    private static final double RECALL_RANGE = DragonStrikeAbility.RANGE + 16;
    private static final DataParameter<Integer> OWNER = EntityDataManager.defineId(DragonStrikeEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> KIND = EntityDataManager.defineId(DragonStrikeEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> START = EntityDataManager.defineId(DragonStrikeEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> ATTACK = EntityDataManager.defineId(DragonStrikeEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> SPECIAL_START = EntityDataManager.defineId(DragonStrikeEntity.class, DataSerializers.INT);
    private DragonStrikeAbility ability;
    private UUID ownerId;
    private int attackCooldown, retreatTicks, pursuitTicks, blockedTarget = -1, blockedUntil;
    private double pursuitDistance = Double.POSITIVE_INFINITY;
    private Vector3d destination = Vector3d.ZERO;
    private DragonSpecialCycle specialCycle = new DragonSpecialCycle(20);
    private LivingEntity specialTarget;
    private LivingEntity grabbedTarget;
    private DragonAttackVfxEntity chargeVisual, bindVisual;
    private Vector3d grabAnchor;
    private int grabTicks;
    private static final int LIFT_TICKS = 14, MAX_GRAB_TICKS = 54;

    public DragonStrikeEntity(EntityType<? extends DragonStrikeEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
        setInvulnerable(true);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        xpReward = 0;
        moveControl = new DragonFlightController();
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MobEntity.createMobAttributes().add(Attributes.MAX_HEALTH, 32)
                .add(Attributes.MOVEMENT_SPEED, .48).add(Attributes.FLYING_SPEED, .48)
                .add(Attributes.FOLLOW_RANGE, DragonStrikeAbility.RANGE).add(Attributes.ARMOR, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, .5);
    }

    @Override protected PathNavigator createNavigation(World world) {
        FlyingPathNavigator navigator = new FlyingPathNavigator(this, world);
        navigator.setCanOpenDoors(false);
        navigator.setCanPassDoors(true);
        navigator.setCanFloat(true);
        return navigator;
    }
    @Override protected void registerGoals() {
        // customServerAiStep handles each summon's attack/follow/retreat states independently.
    }

    public void begin(LivingEntity owner, DragonStrikeAbility ability, DragonKind kind) {
        this.ability = ability;
        ownerId = owner.getUUID();
        entityData.set(OWNER, owner.getId());
        entityData.set(KIND, kind.ordinal());
        entityData.set(START, (int) level.getGameTime());
        entityData.set(ATTACK, (int) level.getGameTime() - 100);
        entityData.set(SPECIAL_START, (int) level.getGameTime() - 100);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind.health);
        getAttribute(Attributes.ARMOR).setBaseValue(kind.armor);
        getAttribute(Attributes.FLYING_SPEED).setBaseValue(kind.speed);
        setHealth((float) kind.health);
        setCustomName(new TranslationTextComponent("entity.kazimod.dragon_strike." + kind.id));
        attackCooldown = 10 + kind.ordinal() * 5;
        specialCycle = new DragonSpecialCycle(20 + kind.ordinal() * 5);
        setPos(owner.getX(), owner.getY() + 1, owner.getZ());
    }

    public LivingEntity getCaster() {
        Entity owner = level.getEntity(entityData.get(OWNER));
        return owner instanceof LivingEntity && (ownerId == null || ownerId.equals(owner.getUUID()))
                ? (LivingEntity) owner : null;
    }
    public DragonKind getKind() { return DragonKind.byId(entityData.get(KIND)); }
    public float getAge(float partial) {
        return Math.max(0, ((int) level.getGameTime() - entityData.get(START)) + partial);
    }
    public float getAttack(float partial) {
        float bite = MathHelper.clamp(1 - (((int) level.getGameTime() - entityData.get(ATTACK)) + partial) / 12F, 0, 1);
        float age = ((int) level.getGameTime() - entityData.get(SPECIAL_START)) + partial;
        float charge = age >= 0 && age < getKind().special.windupTicks
                ? .35F + .55F * age / getKind().special.windupTicks : 0;
        return Math.max(bite, charge);
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        // The small collision box must not hide the full dragon before it reaches prey.
        double range = (DragonStrikeAbility.RANGE + 28) * getViewScale();
        return distance < range * range;
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER, -1);
        entityData.define(KIND, 0);
        entityData.define(START, 0);
        entityData.define(ATTACK, -100);
        entityData.define(SPECIAL_START, -100);
    }

    @Override public void tick() {
        if (!level.isClientSide && !isAlive()) releaseGrab();
        // Check before AI runs, including while dying/disabled or after a logout/dimension transfer.
        if (!level.isClientSide && (ability == null || !ability.ownsDragons(this))) {
            remove();
            return;
        }
        super.tick();
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity owner = getCaster();
        if (owner == null || ability == null || !isAlive()) return;
        if (attackCooldown > 0) attackCooldown--;
        if (retreatTicks > 0) retreatTicks--;
        specialCycle.tick();
        if (getKind() == DragonKind.FIRST && tickCount % 4 == 0) ability.dispelProjectiles(this);
        if (grabbedTarget != null) { tickGrabSlam(); return; }
        if (specialCycle.windingUp()) { tickSpecial(); return; }

        LivingEntity target = getTarget();
        if (target != null && !DragonStrikeAbility.shouldHunt(owner, target)) {
            setTarget(null);
            target = null;
            pursuitTicks = 0;
            pursuitDistance = Double.POSITIVE_INFINITY;
        }
        if (distanceToSqr(owner) > RECALL_RANGE * RECALL_RANGE) {
            setTarget(null);
            pursuitTicks = 0;
            pursuitDistance = Double.POSITIVE_INFINITY;
            navigation.stop();
            if (!moveNearOwner(owner)) remove();
            return;
        }
        // Stagger expensive searches across all eight entities; keep a valid target between searches.
        if (target == null && (tickCount + getKind().ordinal() * 3) % 20 == 0) {
            target = findTarget(owner);
            setTarget(target);
            pursuitTicks = 0;
            pursuitDistance = Double.POSITIVE_INFINITY;
        }
        if (target != null) {
            Vector3d center = target.getBoundingBox().getCenter();
            double distance = getEyePosition(1).distanceTo(center);
            boolean sight = DragonStrikeAbility.clearPath(this, getEyePosition(1), center);
            // A long flight is not a stuck flight. Even the slowest dragon can close
            // the full hunting radius without exhausting a fixed nine-second chase.
            if (distance < pursuitDistance - 1) {
                pursuitDistance = distance;
                pursuitTicks = 0;
            }
            if (specialCycle.ready() && distance <= getKind().special.range && sight
                    && ability.canSpecialHit(this, target) && specialCycle.start(getKind().special)) {
                specialTarget = target;
                entityData.set(SPECIAL_START, (int) level.getGameTime());
                attackCooldown = Math.max(attackCooldown, getKind().special.windupTicks + 12);
                navigation.stop();
                setDeltaMovement(Vector3d.ZERO);
                chargeVisual = DragonAttackVfxEntity.spawn(this, DragonVfxPhase.CHARGE,
                        getEyePosition(1), center, getKind().special.windupTicks, null);
                level.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                        SoundCategory.PLAYERS, .45F, .7F + getKind().ordinal() * .07F);
                return;
            } else if (attackCooldown == 0 && distance <= getKind().reach && sight) {
                entityData.set(ATTACK, (int) level.getGameTime());
                ability.attack(this, target);
                attackCooldown = getKind().attackTicks + random.nextInt(9);
                retreatTicks = getKind().ranged() ? 8 : 20;
                pursuitTicks = 0;
                pursuitDistance = Double.POSITIVE_INFINITY;
            } else if (++pursuitTicks > 180) {
                // Drop prey after nine seconds without closing distance, not nine
                // seconds of otherwise successful pursuit.
                blockedTarget = target.getId();
                blockedUntil = tickCount + 100;
                setTarget(null);
                pursuitTicks = 0;
                pursuitDistance = Double.POSITIVE_INFINITY;
            }
            double phase = getKind().ordinal() * Math.PI / 4 + tickCount * .012;
            double spacing = getKind().ranged() ? getKind().reach * .6 : retreatTicks > 0 ? 4 : 1.2;
            // Angel must close to grab range when its special is ready, despite its ranged basic attack.
            if (specialCycle.ready()) spacing = Math.min(spacing, getKind().special.range * .6);
            destination = center.add(Math.cos(phase) * spacing,
                    getKind().ranged() || retreatTicks > 0 ? 1.3 : -.2, Math.sin(phase) * spacing);
        } else {
            double phase = getKind().ordinal() * Math.PI / 4 + owner.tickCount * .012;
            destination = owner.position().add(Math.cos(phase) * 4,
                    2.5 + .6 * Math.sin(phase * 2), Math.sin(phase) * 4);
        }
        if ((tickCount + getKind().ordinal()) % 8 == 0) steerTo(destination);
        yBodyRot = yRot;
        yHeadRot = yRot;
    }

    private void tickSpecial() {
        LivingEntity target = specialTarget;
        if (target == null || !ability.canSpecialHit(this, target)
                || getEyePosition(1).distanceToSqr(target.getBoundingBox().getCenter())
                > getKind().special.range * getKind().special.range) {
            specialCycle.abandonTarget();
            specialTarget = null;
            entityData.set(SPECIAL_START, (int) level.getGameTime() - 100);
            clearChargeVisual();
            return;
        }
        navigation.stop();
        setDeltaMovement(Vector3d.ZERO);
        Vector3d direction = target.getBoundingBox().getCenter().subtract(getEyePosition(1));
        yRot = (float) (-MathHelper.atan2(direction.x, direction.z) * 180 / Math.PI);
        xRot = (float) (-MathHelper.atan2(direction.y,
                Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * 180 / Math.PI);
        yBodyRot = yHeadRot = yRot;
        if (specialCycle.consumeRelease()) {
            clearChargeVisual();
            specialTarget = null;
            entityData.set(ATTACK, (int) level.getGameTime());
            DragonSpecialAttacks.execute(this, target, ability);
            attackCooldown = Math.max(attackCooldown, 12);
            retreatTicks = 16;
            pursuitTicks = 0;
            pursuitDistance = Double.POSITIVE_INFINITY;
        }
    }

    /** Angel holds one enemy in the air briefly, then drives them down for one attributed hit. */
    public boolean beginGrabSlam(LivingEntity target) {
        if (level.isClientSide || ability == null || getKind().special != DragonSpecial.HEAVENLY_SLAM || grabbedTarget != null
                || !ability.canSpecialHit(this, target) || target.isPassenger() || target.isVehicle()
                || target.hasEffect(ModEffects.GRABBED.get())
                || !level.getEntitiesOfClass(DragonStrikeEntity.class, target.getBoundingBox().inflate(10),
                    dragon -> dragon.grabbedTarget == target).isEmpty()
                || getEyePosition(1).distanceToSqr(target.getBoundingBox().getCenter()) > 4 * 4) return false;
        grabbedTarget = target;
        grabAnchor = target.position();
        grabTicks = 0;
        navigation.stop();
        target.fallDistance = 0;
        target.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 3, 0, false, false));
        Vector3d center = target.getBoundingBox().getCenter();
        bindVisual = DragonAttackVfxEntity.spawn(this, DragonVfxPhase.BIND, center, center, MAX_GRAB_TICKS, target);
        level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundCategory.PLAYERS, .8F, 1.25F);
        return true;
    }

    private void tickGrabSlam() {
        LivingEntity victim = grabbedTarget;
        if (victim == null) return;
        if (ability == null || !isAlive() || !ability.canSpecialHit(this, victim)
                || victim.removed || victim.isPassenger() || victim.isVehicle() || victim.hasEffect(ModEffects.GRABBED.get())
                || distanceToSqr(victim) > 10 * 10 || ++grabTicks > MAX_GRAB_TICKS
                || victim.position().distanceToSqr(grabAnchor) > 8 * 8) {
            releaseGrab();
            return;
        }
        victim.fallDistance = 0;
        if (grabTicks > LIFT_TICKS && victim.isOnGround()) {
            Vector3d impact = victim.position();
            releaseGrab(); // Clear ownership before any damage callbacks can remove this dragon.
            if (ability.specialHit(this, victim, 18)) {
                ((ServerWorld) level).sendParticles(ParticleTypes.CLOUD, impact.x, impact.y + .15, impact.z,
                        24, 1.3, .15, 1.3, .08);
                DragonAttackVfxEntity.spawn(this, DragonVfxPhase.SLAM, impact.add(0, .05, 0), impact.add(0, .05, 0));
                level.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                        SoundCategory.PLAYERS, .75F, 1.3F);
            }
            return;
        }
        victim.setJumping(false);
        victim.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 3, 0, false, false));
        double dx = MathHelper.clamp(grabAnchor.x - victim.getX(), -.2, .2);
        double dz = MathHelper.clamp(grabAnchor.z - victim.getZ(), -.2, .2);
        Vector3d motion = new Vector3d(dx, grabTicks <= LIFT_TICKS ? .35 : -1.15, dz);
        if (!loadedMotion(victim, motion)) { releaseGrab(); return; }
        if (!level.noCollision(victim, victim.getBoundingBox().expandTowards(motion))) {
            // Let normal entity physics clip the downward slam to the floor. Never teleport through terrain.
            grabTicks = Math.max(grabTicks, LIFT_TICKS);
            motion = new Vector3d(0, -1.15, 0);
            if (!loadedMotion(victim, motion)) { releaseGrab(); return; }
        }
        AbilityHelper.setDeltaMovement(victim, motion.x, motion.y, motion.z);
        Vector3d follow = victim.position().add(2, 1.5, 0).subtract(position());
        Vector3d dragonMotion = follow.lengthSqr() < .01 ? Vector3d.ZERO
                : follow.normalize().scale(Math.min(.45, follow.length() * .2));
        setDeltaMovement(loadedMotion(this, dragonMotion)
                && level.noCollision(this, getBoundingBox().expandTowards(dragonMotion)) ? dragonMotion : Vector3d.ZERO);
        if (tickCount % 4 == 0) {
            entityData.set(ATTACK, (int) level.getGameTime());
        }
    }

    private boolean loadedMotion(Entity entity, Vector3d motion) {
        AxisAlignedBB swept = entity.getBoundingBox().expandTowards(motion);
        if (!level.getWorldBorder().isWithinBounds(new BlockPos(swept.minX, swept.minY, swept.minZ))
                || !level.getWorldBorder().isWithinBounds(new BlockPos(swept.maxX, swept.maxY, swept.maxZ))) return false;
        for (int x = MathHelper.floor(swept.minX) >> 4; x <= MathHelper.floor(swept.maxX) >> 4; x++) {
            for (int z = MathHelper.floor(swept.minZ) >> 4; z <= MathHelper.floor(swept.maxZ) >> 4; z++) {
                if (!level.hasChunkAt(new BlockPos(x << 4, 0, z << 4))) return false;
            }
        }
        return true;
    }

    private void releaseGrab() {
        if (bindVisual != null) { bindVisual.remove(); bindVisual = null; }
        LivingEntity victim = grabbedTarget;
        grabbedTarget = null;
        grabAnchor = null;
        grabTicks = 0;
        if (victim != null && !victim.removed && victim.level == level) {
            victim.fallDistance = 0;
            Vector3d motion = victim.getDeltaMovement();
            AbilityHelper.setDeltaMovement(victim, motion.x, Math.min(0, motion.y), motion.z);
        }
        // Three-tick refreshes expire naturally without removing another ability's restraint.
    }

    @Override public void remove() {
        if (!level.isClientSide) releaseGrab();
        clearChargeVisual();
        specialTarget = null;
        specialCycle.abandonTarget();
        super.remove();
    }

    private void clearChargeVisual() {
        if (chargeVisual != null) { chargeVisual.remove(); chargeVisual = null; }
    }

    private LivingEntity findTarget(LivingEntity owner) {
        LivingEntity best = null;
        double score = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                owner.getBoundingBox().inflate(DragonStrikeAbility.RANGE))) {
            if (candidate.getId() == blockedTarget && tickCount < blockedUntil
                    || !DragonStrikeAbility.shouldHunt(owner, candidate)
                    || !DragonStrikeAbility.clearPath(this, getEyePosition(1), candidate.getBoundingBox().getCenter())) continue;
            double value = distanceToSqr(candidate) + ability.attackersOn(candidate) * 80;
            if (candidate == owner.getLastHurtByMob() || candidate == getLastHurtByMob()) value *= .4;
            if (candidate == owner.getLastHurtMob()) value *= .65;
            if (value < score) { score = value; best = candidate; }
        }
        return best;
    }

    private void steerTo(Vector3d point) {
        if (!level.hasChunkAt(new BlockPos(point))) { navigation.stop(); return; }
        if (!horizontalCollision && !verticalCollision && DragonStrikeAbility.clearPath(this, getEyePosition(1), point)
                && level.noCollision(this, getBoundingBox().move(point.subtract(position())))) {
            navigation.stop();
            moveControl.setWantedPosition(point.x, point.y, point.z, 1);
        } else {
            navigation.moveTo(point.x, point.y, point.z, 1);
        }
    }

    /** Safe initial placement/long-distance recall. Never loads chunks or places inside solid blocks. */
    public boolean moveNearOwner(LivingEntity owner) {
        for (int attempt = 0; attempt < 64; attempt++) {
            double angle = getKind().ordinal() * Math.PI / 4 + attempt * 2.4;
            double radius = 2.8 + attempt / 16;
            Vector3d point = owner.position().add(Math.cos(angle) * radius,
                    .5 + (attempt % 4) * .8, Math.sin(angle) * radius);
            if (!level.hasChunkAt(new BlockPos(point)) || !level.getWorldBorder().isWithinBounds(new BlockPos(point))
                    || !level.noCollision(this, getBoundingBox().move(point.subtract(position())))) continue;
            setPos(point.x, point.y, point.z);
            setDeltaMovement(Vector3d.ZERO);
            navigation.stop();
            return true;
        }
        return false;
    }

    /** Collision-aware inertial flight, driven by either direct steering or the flying navigator. */
    private final class DragonFlightController extends MovementController {
        private DragonFlightController() { super(DragonStrikeEntity.this); }
        @Override public void tick() {
            if (grabbedTarget != null) return;
            if (specialCycle.windingUp()) { setDeltaMovement(Vector3d.ZERO); return; }
            if (operation != Action.MOVE_TO || !isAlive()) {
                setDeltaMovement(getDeltaMovement().scale(.8));
                return;
            }
            Vector3d offset = new Vector3d(wantedX - getX(), wantedY - getY(), wantedZ - getZ());
            double distance = offset.length();
            if (distance < .35) { operation = Action.WAIT; return; }
            Vector3d desired = offset.scale(Math.min(getKind().speed * speedModifier, distance * .2) / distance);
            setDeltaMovement(getDeltaMovement().scale(.75).add(desired.scale(.25)));
            Vector3d motion = getDeltaMovement();
            if (motion.lengthSqr() > .001) {
                yRot = rotlerp(yRot, (float) (-MathHelper.atan2(motion.x, motion.z) * 180 / Math.PI), 12);
                xRot = rotlerp(xRot, (float) (-MathHelper.atan2(motion.y, Math.sqrt(motion.x * motion.x
                        + motion.z * motion.z)) * 180 / Math.PI), 10);
            }
        }
    }

    @Override public void travel(Vector3d input) {
        // No gravity or forced player motion. Entity collisions still prevent crossing blocks.
        // A long pursuit must not carry a summon into an unloaded, unticking chunk.
        AxisAlignedBB swept = getBoundingBox().expandTowards(getDeltaMovement());
        for (int x = MathHelper.floor(swept.minX) >> 4; x <= MathHelper.floor(swept.maxX) >> 4; x++) {
            for (int z = MathHelper.floor(swept.minZ) >> 4; z <= MathHelper.floor(swept.maxZ) >> 4; z++) {
                if (!level.hasChunkAt(new BlockPos(x << 4, 0, z << 4))) {
                    navigation.stop();
                    setDeltaMovement(Vector3d.ZERO);
                    return;
                }
            }
        }
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(.96));
    }
    // Summons cannot take damage, including sources that bypass the vanilla invulnerable flag.
    // Dismissal and session cleanup still use remove() and release any grabbed target.
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override protected void dropAllDeathLoot(DamageSource source) { /* Temporary summons have no loot. */ }
    @Override public boolean canBeLeashed(PlayerEntity player) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public void readAdditionalSaveData(CompoundNBT nbt) { super.readAdditionalSaveData(nbt); remove(); }
    @Override public void addAdditionalSaveData(CompoundNBT nbt) { super.addAdditionalSaveData(nbt); }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
