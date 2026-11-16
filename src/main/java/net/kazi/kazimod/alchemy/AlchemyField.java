package net.kazi.kazimod.alchemy;

import java.util.*;
import net.kazi.kazimod.petal.PetalFeatures;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

/** Bounded temporary geometry: constructs never replace blocks or leave saved entities behind. */
public final class AlchemyField extends Entity {
    private static final DataParameter<Integer> KIND = EntityDataManager.defineId(AlchemyField.class, DataSerializers.INT);
    private static final DataParameter<Integer> OWNER = EntityDataManager.defineId(AlchemyField.class, DataSerializers.INT);
    private static final DataParameter<Integer> TARGET = EntityDataManager.defineId(AlchemyField.class, DataSerializers.INT);
    private static final DataParameter<Integer> START = EntityDataManager.defineId(AlchemyField.class, DataSerializers.INT);
    private static final DataParameter<Integer> LIFE = EntityDataManager.defineId(AlchemyField.class, DataSerializers.INT);
    private static final DataParameter<Integer> PHASE = EntityDataManager.defineId(AlchemyField.class, DataSerializers.INT);
    private static final DataParameter<Float> SIZE = EntityDataManager.defineId(AlchemyField.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> HEADING = EntityDataManager.defineId(AlchemyField.class, DataSerializers.FLOAT);
    private static final DataParameter<CompoundNBT> DATA = EntityDataManager.defineId(AlchemyField.class, DataSerializers.COMPOUND_TAG);
    private final Set<UUID> hit = new HashSet<>();
    private UUID ownerId, targetId;
    private float health = 60;
    private int launchTime;
    private AlchemyAbility damage;

    public AlchemyField(EntityType<?> type, World world) { super(type, world); noPhysics = true; setNoGravity(true); }
    @Override protected void defineSynchedData() {
        entityData.define(KIND, 0); entityData.define(OWNER, -1); entityData.define(TARGET, -1);
        entityData.define(START, 0); entityData.define(LIFE, 20); entityData.define(PHASE, 0);
        entityData.define(SIZE, 1F); entityData.define(HEADING, 0F); entityData.define(DATA, new CompoundNBT());
    }
    private static AlchemyField create(LivingEntity p, int kind, Vector3d pos, int life, float size) {
        AlchemyField f = new AlchemyField(AlchemyFeatures.FIELD.get(), p.level);
        f.entityData.set(KIND, kind); f.entityData.set(OWNER, p.getId()); f.ownerId = p.getUUID();
        f.entityData.set(START, (int)p.level.getGameTime()); f.entityData.set(LIFE, life); f.entityData.set(SIZE, size);
        f.entityData.set(HEADING, p.yRot); f.setPos(pos.x, pos.y, pos.z);
        BlockPos ground = surface(p.level, new BlockPos(pos).below());
        CompoundNBT data = new CompoundNBT();
        BlockState material = ground == null ? Blocks.STONE.defaultBlockState() : p.level.getBlockState(ground);
        // Earth is reconstructed into solid stone rather than growing transparent or fluid blocks.
        if (!material.getMaterial().isSolid() || material.hasTileEntity()) material = Blocks.STONE.defaultBlockState();
        data.putInt("Material", Block.getId(material)); f.entityData.set(DATA, data);
        return f;
    }
    public static AlchemyField spawn(LivingEntity p, int kind, Vector3d pos, int life, float size) {
        AlchemyField f = create(p, kind, pos, life, size); p.level.addFreshEntity(f); return f;
    }
    public static AlchemyField fangs(LivingEntity p) {
        AlchemyField f = create(p, 1, p.position(), 66, 18);
        Vector3d forward = horizontal(p);
        List<BlockPos> nodes = new ArrayList<>();
        BlockPos previous = surface(p.level, p.blockPosition().below());
        if (previous == null) return null;
        for (int i = 1; i <= 14; i++) {
            Vector3d at = p.position().add(forward.scale(i * 1.25));
            BlockPos next = surface(p.level, new BlockPos(at.x, previous.getY(), at.z));
            if (next == null || Math.abs(next.getY() - previous.getY()) > 1) break;
            nodes.add(next); previous = next;
        }
        if (nodes.isEmpty()) return null;
        f.nodes(nodes); p.level.addFreshEntity(f); return f;
    }
    public static AlchemyField wall(LivingEntity p) {
        float yaw = Math.round(p.yRot / 90F) * 90F;
        Vector3d direction = new Vector3d(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
        Vector3d at = p.position().add(direction.scale(3));
        BlockPos ground = surface(p.level, new BlockPos(at).below());
        if (ground == null) return null;
        AlchemyField f = create(p, 2, new Vector3d(at.x, ground.getY() + 1, at.z), 160, 2.5F);
        f.entityData.set(HEADING, yaw); f.updateBounds();
        if (!p.level.noCollision(f.getBoundingBox())) return null;
        p.level.addFreshEntity(f); return f;
    }
    public static AlchemyField prison(LivingEntity p, LivingEntity target) {
        BlockPos ground = surface(p.level, target.blockPosition().below());
        if (ground == null) return null;
        AlchemyField f = create(p, 3, new Vector3d(target.getX(), ground.getY() + 1, target.getZ()), AlchemyAbility.PRISON_HOLD + 14, 1.5F);
        f.target(target); f.health = 35; p.level.addFreshEntity(f); f.tick(); return f;
    }
    public static AlchemyField grand(LivingEntity p, Vector3d at) {
        AlchemyField f = create(p, 5, at, AlchemyAbility.GRAND_WINDUP + 50, 12);
        List<BlockPos> nodes = new ArrayList<>();
        for (int ring = 0; ring < 3; ring++) {
            double radius = 11 - ring * 3.7;
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                BlockPos surface = surface(p.level, new BlockPos(at.add(Math.cos(a) * radius, -1, Math.sin(a) * radius)));
                if (surface != null) nodes.add(surface);
            }
        }
        if (nodes.isEmpty()) return null;
        f.nodes(nodes); p.level.addFreshEntity(f); return f;
    }
    public static Vector3d horizontal(LivingEntity p) {
        Vector3d v = new Vector3d(p.getLookAngle().x, 0, p.getLookAngle().z);
        return v.lengthSqr() < .01 ? new Vector3d(-Math.sin(Math.toRadians(p.yRot)), 0, Math.cos(Math.toRadians(p.yRot))) : v.normalize();
    }
    public static BlockPos surface(World world, BlockPos at) {
        for (int y = 1; y >= -1; y--) {
            BlockPos p = at.offset(0, y, 0);
            if (!world.hasChunkAt(p) || !world.getWorldBorder().isWithinBounds(p)) continue;
            BlockState state = world.getBlockState(p);
            if (state.getMaterial().isSolid() && !state.getCollisionShape(world, p).isEmpty()
                    && world.getBlockState(p.above()).getCollisionShape(world, p.above()).isEmpty()) return p;
        }
        return null;
    }
    private void nodes(List<BlockPos> positions) {
        int[] xyz = new int[positions.size() * 3]; int i = 0;
        for (BlockPos p : positions) { xyz[i++] = p.getX(); xyz[i++] = p.getY(); xyz[i++] = p.getZ(); }
        CompoundNBT n = entityData.get(DATA).copy(); n.putIntArray("Nodes", xyz); entityData.set(DATA, n);
    }
    public int[] nodes() { return entityData.get(DATA).getIntArray("Nodes"); }
    public BlockState material() { return Block.stateById(entityData.get(DATA).getInt("Material")); }
    public int kind() { return entityData.get(KIND); }
    public int life() { return entityData.get(LIFE); }
    public int phase() { return entityData.get(PHASE); }
    public float size() { return entityData.get(SIZE); }
    public float heading() { return entityData.get(HEADING); }
    public float age(float partial) { return Math.max(0, (int)level.getGameTime() - entityData.get(START) + partial); }
    public float visualAge(float partial) {
        float age = age(partial);
        // Preserve the existing release animations while compressing only their entrance.
        if (kind() == 3) return Math.max(18, age + 14);
        if (kind() == 5) return age < AlchemyAbility.GRAND_WINDUP
                ? age * 60 / AlchemyAbility.GRAND_WINDUP : age + 60 - AlchemyAbility.GRAND_WINDUP;
        return age;
    }
    public float phaseAge(float partial) { return phase() == 1 ? Math.max(0, age(partial) - entityData.get(DATA).getInt("LaunchAge")) : age(partial); }
    public LivingEntity owner() { Entity e = level.getEntity(entityData.get(OWNER)); return e instanceof LivingEntity && (ownerId == null || ownerId.equals(e.getUUID())) ? (LivingEntity)e : null; }
    public LivingEntity target() { Entity e = level.getEntity(entityData.get(TARGET)); return e instanceof LivingEntity && (targetId == null || targetId.equals(e.getUUID())) ? (LivingEntity)e : null; }
    public void target(LivingEntity target) { entityData.set(TARGET, target.getId()); targetId = target.getUUID(); }
    public float durability() { return health; }
    public void launch() {
        if (kind() != 2 || phase() != 0 || removed) return;
        entityData.set(PHASE, 1); launchTime = (int)age(0);
        entityData.set(LIFE, launchTime + 26);
        CompoundNBT state = entityData.get(DATA).copy();
        state.putInt("LaunchAge", launchTime); entityData.set(DATA, state);
        setDeltaMovement(-Math.sin(Math.toRadians(heading())) * 1.1, 0, Math.cos(Math.toRadians(heading())) * 1.1);
        AlchemyAbility.sound(owner(), SoundEvents.STONE_BREAK, .9F, .75F);
    }
    private void updateBounds() {
        if (kind() == 2) {
            boolean alongX = Math.abs(Math.cos(Math.toRadians(heading()))) > .5;
            double x = alongX ? size() : .4, z = alongX ? .4 : size();
            setBoundingBox(new AxisAlignedBB(getX() - x, getY(), getZ() - z, getX() + x, getY() + 3.5, getZ() + z));
        } else if (kind() == 3) setBoundingBox(new AxisAlignedBB(getX() - size(), getY(), getZ() - size(), getX() + size(), getY() + 3, getZ() + size()));
    }
    @Override public void tick() {
        super.tick(); updateBounds();
        if (kind() == 2 || kind() == 3) level.increaseMaxEntityRadius(size() + 1);
        if (level.isClientSide) return;
        LivingEntity p = owner(); int age = (int)age(0);
        if (p == null || !AlchemyFeatures.usable(p) || age >= life() || p.distanceToSqr(this) > 96 * 96) { remove(); return; }
        if (damage == null) damage = AlchemyAbility.CORES[Math.min(5, kind())].createAbility();
        if (kind() == 1) {
            int[] nodes = nodes();
            for (int i = 0; i < nodes.length / 3; i++) if (age == 12 + i * 2) {
                Vector3d center = new Vector3d(nodes[i * 3] + .5, nodes[i * 3 + 1] + 1, nodes[i * 3 + 2] + .5);
                impact(p, new AxisAlignedBB(center.add(-1.15, -.1, -1.15), center.add(1.15, 2.6, 1.15)), 18, .55);
                if (i % 3 == 0) AlchemyAbility.sound(p, SoundEvents.STONE_BREAK, .55F, .85F);
            }
        } else if (kind() == 2) {
            if (phase() == 1) {
                Vector3d step = getDeltaMovement();
                AxisAlignedBB swept = getBoundingBox().expandTowards(step);
                if (age - launchTime > 24 || level.getBlockCollisions(this, swept).anyMatch(shape -> !shape.isEmpty())) { remove(); return; }
                impact(p, swept, 20, .25);
                setPos(getX() + step.x, getY(), getZ() + step.z); updateBounds();
            } else if (age >= 12) absorbProjectiles(p);
        } else if (kind() == 3) {
            LivingEntity t = target();
            if (t == null || !PetalFeatures.canHit(p, t) || !t.isAlive()) { remove(); return; }
            if (hit.add(t.getUUID())) {
                if (!getBoundingBox().inflate(.1).intersects(t.getBoundingBox()) || !p.canSee(t)) { remove(); return; }
                damage.strike(p, t, 6);
                AlchemyAbility.sound(p, SoundEvents.STONE_PLACE, .9F, .7F);
            }
            if (age < AlchemyAbility.PRISON_HOLD) {
                if (t.distanceToSqr(this) > 16 || Math.abs(t.getY() - getY()) > 3) { remove(); return; }
                t.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 3, 3, false, false));
                double dx = getX() - t.getX(), dz = getZ() - t.getZ();
                Vector3d motion = t.getDeltaMovement();
                AbilityHelper.setDeltaMovement(t, Math.abs(dx) > size() - .7 ? dx * .18 : motion.x * .3,
                        Math.min(0, motion.y), Math.abs(dz) > size() - .7 ? dz * .18 : motion.z * .3);
            }
        } else if (kind() == 4 && size() <= 1) {
            LivingEntity t = target();
            Vector3d pos = t == null ? p.position() : t.position();
            setPos(pos.x, pos.y, pos.z);
        } else if (kind() == 5 && age >= AlchemyAbility.GRAND_WINDUP && age <= AlchemyAbility.GRAND_WINDUP + 28 && age % 2 == 0) {
            float radius = Math.max(1.5F, 12 - (age - AlchemyAbility.GRAND_WINDUP) * .42F);
            for (LivingEntity part : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(13, 7, 13), t -> PetalFeatures.canHit(p, t))) {
                double distance = Math.sqrt((part.getX() - getX()) * (part.getX() - getX()) + (part.getZ() - getZ()) * (part.getZ() - getZ()));
                if (Math.abs(distance - radius) > 2 || part.getY() < getY() - 1 || part.getY() > getY() + 3.8 || !p.canSee(part)) continue;
                BlockPos ground = surface(level, part.blockPosition().below());
                if (ground == null) continue;
                LivingEntity actual = PetalFeatures.bodyOwner(part);
                if (actual != null && hit.add(actual.getUUID()) && damage.strike(p, actual, 46)) {
                    Vector3d in = position().subtract(actual.position()).normalize();
                    AbilityHelper.setDeltaMovement(actual, in.x * .7, .55, in.z * .7);
                }
            }
        }
    }
    private void impact(LivingEntity p, AxisAlignedBB area, float amount, double launch) {
        for (LivingEntity part : level.getEntitiesOfClass(LivingEntity.class, area, t -> PetalFeatures.canHit(p, t))) {
            LivingEntity target = PetalFeatures.bodyOwner(part);
            if (target != null && p.canSee(part) && hit.add(target.getUUID()) && damage.strike(p, target, amount)) {
                Vector3d direction = horizontal(p);
                AbilityHelper.setDeltaMovement(target, direction.x * .45, launch, direction.z * .45);
            }
        }
    }
    private void absorbProjectiles(LivingEntity p) {
        // Sweep the next step as well as the last one; fast projectiles cannot skip a thin wall.
        for (Entity shot : level.getEntities(this, getBoundingBox().inflate(32), e -> e instanceof ProjectileEntity || e instanceof AbilityProjectileEntity)) {
            if (!shot.isAlive()) continue;
            Entity shooter = shot instanceof ProjectileEntity ? ((ProjectileEntity)shot).getOwner() : shot instanceof AbilityProjectileEntity ? ((AbilityProjectileEntity)shot).getThrower() : null;
            if (shooter == p || shooter instanceof LivingEntity && !PetalFeatures.canHit(p, (LivingEntity)shooter)) continue;
            Vector3d before = shot.tickCount <= 1 ? shot.position() : new Vector3d(shot.xo, shot.yo, shot.zo);
            Vector3d after = shot.position().add(shot.getDeltaMovement());
            if (!getBoundingBox().inflate(.15).clip(before, after).isPresent() && !getBoundingBox().contains(shot.position())) continue;
            float amount = projectileDamage(shot);
            float previous = health;
            hurt(new DamageSource("alchemy_projectile"), amount);
            if (amount <= previous) shot.remove();
            if (removed) return;
        }
    }
    public static float projectileDamage(Entity shot) {
        if (shot instanceof AbilityProjectileEntity) return Math.max(1, ((AbilityProjectileEntity)shot).getDamage());
        if (shot instanceof AbstractArrowEntity) return (float)Math.max(1, ((AbstractArrowEntity)shot).getBaseDamage() * shot.getDeltaMovement().length());
        return 12;
    }
    @Override public boolean isPickable() { return !removed && (kind() == 2 && phase() == 0 && age(0) >= 12 || kind() == 3 && age(0) < AlchemyAbility.PRISON_HOLD); }
    @Override public boolean canBeCollidedWith() { return kind() == 2 && isPickable(); }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (level.isClientSide || !isPickable() || !Float.isFinite(amount) || amount <= 0 || source.getEntity() == owner()) return false;
        health -= amount; markHurt();
        if (health <= 0) { level.playSound(null, blockPosition(), SoundEvents.STONE_BREAK, SoundCategory.PLAYERS, 1, .65F); remove(); }
        return true;
    }
    @Override public AxisAlignedBB getBoundingBoxForCulling() { return getBoundingBox().inflate(kind() == 1 ? 22 : kind() == 5 ? 16 : 5); }
    @Override protected void readAdditionalSaveData(CompoundNBT nbt) { remove(); }
    @Override protected void addAdditionalSaveData(CompoundNBT nbt) {}
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
