package net.kazi.kazimod.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.kazi.kazimod.abilities.GoruRework.EnkiduAbility;
import net.kazi.kazimod.abilities.GoruRework.EnkiduAbility.Mode;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

/** A single synchronized cast, regardless of the number of portals, links or victims. */
public final class EnkiduVfxEntity extends Entity {
    public static final int CHARGING = 0, LAUNCHING = 1, BOUND = 2, FADING = 3;
    public static final int TRAVEL_TICKS = 2, FADE_TICKS = 12;
    private static final DataParameter<CompoundNBT> STATE = EntityDataManager.defineId(EnkiduVfxEntity.class, DataSerializers.COMPOUND_TAG);
    private static final DataParameter<Float> PROGRESS = EntityDataManager.defineId(EnkiduVfxEntity.class, DataSerializers.FLOAT);
    private final List<Binding> bindings = new ArrayList<>();
    private LivingEntity owner, selected;
    private EnkiduAbility ability;
    private CompoundNBT cachedState;
    private VisualTarget[] cachedTargets = new VisualTarget[0];

    public EnkiduVfxEntity(EntityType<? extends EnkiduVfxEntity> type, World world) {
        super(type, world);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
    }

    public void begin(LivingEntity owner, EnkiduAbility ability, Mode mode, Vector3d center, LivingEntity selected) {
        this.owner = owner; this.ability = ability; this.selected = selected;
        setPos(center.x, center.y, center.z);
        CompoundNBT state = new CompoundNBT();
        state.putInt("owner", owner.getId());
        state.putBoolean("area", mode == Mode.AREA);
        state.putBoolean("absolute", mode == Mode.ABSOLUTE);
        state.putLong("start", level.getGameTime());
        if (selected != null) bindings.add(new Binding(selected));
        writeTargets(state);
        entityData.set(STATE, state);
        sound(SoundEvents.BEACON_ACTIVATE, 0.7F, 1.5F);
        owner.level.playSound(null, owner.blockPosition(), SoundEvents.CHAIN_PLACE, SoundCategory.PLAYERS, 0.8F, 0.7F);
        refreshBounds();
    }

    public boolean isCharging() { return getPhase() == CHARGING; }
    public boolean isFading() { return getPhase() == FADING; }
    public int getPhase() { return entityData.get(STATE).getInt("phase"); }
    public Mode getMode() { return entityData.get(STATE).getBoolean("absolute") ? Mode.ABSOLUTE
            : entityData.get(STATE).getBoolean("area") ? Mode.AREA : Mode.SINGLE; }
    public float getPhaseAge(float partial) {
        return Math.max(0, level.getGameTime() - entityData.get(STATE).getLong("start") + partial);
    }
    public float getVisualAge(float partial) {
        return isFading() ? entityData.get(STATE).getFloat("fadeAge")
                : isCharging() ? Math.min(getMode().chargeTicks, entityData.get(PROGRESS) * getMode().chargeTicks + partial)
                : getPhaseAge(partial);
    }
    public void setChargeProgress(float progress) { entityData.set(PROGRESS, MathHelper.clamp(progress, 0, 1)); }
    public int getVisualPhase() { return isFading() ? entityData.get(STATE).getInt("fadePhase") : getPhase(); }
    public Vector3d getCenter(float partial) {
        if (isCharging() && getMode() == Mode.SINGLE) {
            VisualTarget[] targets = getVisualTargets();
            if (targets.length > 0) return targets[0].position(level, partial);
        }
        return position();
    }

    public VisualTarget[] getVisualTargets() {
        CompoundNBT state = entityData.get(STATE);
        if (state != cachedState) {
            cachedState = state;
            ListNBT targets = state.getList("targets", 10);
            cachedTargets = new VisualTarget[getMode() == Mode.ABSOLUTE ? targets.size()
                    : Math.min(EnkiduAbility.MAX_AREA_TARGETS, targets.size())];
            for (int i = 0; i < cachedTargets.length; i++) cachedTargets[i] = new VisualTarget(targets.getCompound(i));
        }
        return cachedTargets;
    }

    public void launch() {
        if (level.isClientSide || !isCharging()) return;
        List<LivingEntity> candidates;
        if (getMode() == Mode.ABSOLUTE) {
            setPos(owner.getX(), owner.getY(), owner.getZ());
            candidates = EnkiduAbility.absoluteTargets(owner);
        } else if (getMode() == Mode.AREA) candidates = EnkiduAbility.areaTargets(owner, position());
        else candidates = validCandidate(selected) ? Collections.singletonList(selected) : Collections.emptyList();
        bindings.clear();
        for (LivingEntity target : candidates) bindings.add(new Binding(target));
        if (bindings.isEmpty()) { fade(); return; }
        if (getMode() == Mode.SINGLE) setPos(selected.getX(), selected.getY(), selected.getZ());
        phase(LAUNCHING);
        sound(SoundEvents.TRIDENT_RETURN, 1.2F, 1.35F);
        sound(SoundEvents.CHAIN_PLACE, 1.4F, 0.65F);
    }

    public void fade() {
        if (level.isClientSide || isFading()) return;
        CompoundNBT state = entityData.get(STATE).copy();
        state.putInt("fadePhase", getPhase());
        state.putFloat("fadeAge", getVisualAge(0));
        state.putInt("phase", FADING);
        state.putLong("start", level.getGameTime());
        // Freeze the last visible endpoints instead of chasing escaped/dead targets during retraction.
        if (!bindings.isEmpty()) writeTargets(state);
        ListNBT targets = state.getList("targets", 10);
        for (int i = 0; i < targets.size(); i++) targets.getCompound(i).putInt("id", -1);
        entityData.set(STATE, state);
        bindings.clear(); // Short refreshed effects expire naturally without removing another cast's hold.
        sound(SoundEvents.CHAIN_BREAK, 0.9F, 0.7F);
        sound(SoundEvents.BEACON_DEACTIVATE, 0.5F, 1.6F);
    }

    @Override public void tick() {
        super.tick();
        if (level.isClientSide) return;
        float age = getPhaseAge(0);
        if (isFading()) { if (age >= FADE_TICKS) remove(); return; }
        if (owner == null || !owner.isAlive() || owner.removed || owner.level != level
                || ability == null || !ability.ownsCast(this)
                || AbilityDataCapability.get(owner).getEquippedAbility(EnkiduAbility.INSTANCE) != ability) {
            fade(); return;
        }
        if (isCharging()) {
            if (owner.position().distanceToSqr(position()) > 48 * 48) { fade(); return; }
            if (getMode() == Mode.SINGLE) {
                if (!validCandidate(selected)) { fade(); return; }
                setPos(selected.getX(), selected.getY(), selected.getZ());
                refreshBounds();
            }
            if (tickCount % 10 == 0) sound(SoundEvents.CHAIN_STEP, 0.6F, 0.8F + age * 0.01F);
        } else if (getPhase() == LAUNCHING && age >= TRAVEL_TICKS) {
            // Catch eligible targets without damage; the strike happens after their bind expires.
            bindings.removeIf(binding -> !validCandidate(binding.target)
                    || getMode() == Mode.AREA && (!EnkiduAbility.insideArea(position(), binding.target)
                    || !EnkiduAbility.clearPath(owner, position().add(0, .25, 0), binding.target.getEyePosition(1)))
                    || binding.target.position().distanceToSqr(binding.anchor) > 3 * 3
                    || AbilityHelper.isDodging(binding.target)
                    || !binding.target.isAlive());
            if (bindings.isEmpty()) { fade(); return; }
            for (Binding binding : bindings) binding.anchor = binding.target.position();
            phase(BOUND);
            restrain();
            sound(SoundEvents.CHAIN_HIT, 1.5F, 0.65F);
            sound(SoundEvents.ANVIL_LAND, 0.25F, 1.8F);
        } else if (getPhase() == BOUND) {
            if (age >= getMode().bindTicks) { finishBindings(); return; }
            restrain();
            if (tickCount % 16 == 0) sound(SoundEvents.CHAIN_STEP, 0.45F, 0.75F);
        }
    }

    private boolean validCandidate(LivingEntity target) {
        if (getMode() == Mode.ABSOLUTE) return EnkiduAbility.isAbsoluteTarget(owner, target);
        double reach = EnkiduAbility.RANGE + 4 + (getMode() == Mode.AREA ? EnkiduAbility.AREA_RADIUS : 0);
        return EnkiduAbility.isEnemy(owner, target) && !target.removed
                && owner.distanceToSqr(target) <= reach * reach
                && EnkiduAbility.clearPath(owner, owner.getEyePosition(1), target.getEyePosition(1));
    }

    private void restrain() {
        boolean changed = false;
        Iterator<Binding> iterator = bindings.iterator();
        while (iterator.hasNext()) {
            Binding binding = iterator.next();
            LivingEntity target = binding.target;
            double dx = target.getX() - binding.anchor.x, dz = target.getZ() - binding.anchor.z;
            if (!EnkiduAbility.isEnemy(owner, target) || target.removed || AbilityHelper.isDodging(target)
                    || dx * dx + dz * dz > 9 || Math.abs(target.getY() - binding.anchor.y) > 6
                    || owner.distanceToSqr(target) > 48 * 48) {
                iterator.remove(); changed = true; continue;
            }
            if (target.isPassenger()) target.stopRiding();
            target.addEffect(new EffectInstance(KaziEffects.HEAVEN_BOUND.get(), 3, 0, false, false, false));
            if (!target.hasEffect(KaziEffects.HEAVEN_BOUND.get())) {
                iterator.remove(); changed = true; continue;
            }
            AbilityHelper.slowEntityFall(target, 3);
            target.setJumping(false);
            AbilityHelper.setDeltaMovement(target, 0, Math.min(0, target.getDeltaMovement().y), 0);
        }
        if (bindings.isEmpty()) fade();
        else if (changed) {
            CompoundNBT state = entityData.get(STATE).copy();
            writeTargets(state);
            entityData.set(STATE, state);
        }
    }

    private void finishBindings() {
        Iterator<Binding> iterator = bindings.iterator();
        while (iterator.hasNext()) {
            Binding binding = iterator.next();
            LivingEntity target = binding.target;
            double dx = target.getX() - binding.anchor.x, dz = target.getZ() - binding.anchor.z;
            if (!EnkiduAbility.isEnemy(owner, target) || target.removed || AbilityHelper.isDodging(target)
                    || dx * dx + dz * dz > 9 || Math.abs(target.getY() - binding.anchor.y) > 6
                    || owner.distanceToSqr(target) > 48 * 48) {
                iterator.remove();
                continue;
            }
            // Stop refreshing the effect, then wait for its last three ticks to expire.
            // Never remove another cast's bind or damage a target while it is still bound.
            if (target.hasEffect(KaziEffects.HEAVEN_BOUND.get())) continue;
            iterator.remove(); // Consume this strike before damage callbacks can run.
            ability.strike(owner, target, getMode());
        }
        if (bindings.isEmpty()) fade();
    }

    private void phase(int phase) {
        CompoundNBT state = entityData.get(STATE).copy();
        state.putInt("phase", phase);
        state.putLong("start", level.getGameTime());
        writeTargets(state);
        entityData.set(STATE, state);
        refreshBounds();
    }
    private void writeTargets(CompoundNBT state) {
        ListNBT targets = new ListNBT();
        for (Binding binding : bindings) {
            LivingEntity target = binding.target;
            CompoundNBT tag = new CompoundNBT();
            tag.putInt("id", target.getId());
            tag.putInt("seed", target.getId());
            tag.putDouble("x", target.getX()); tag.putDouble("y", target.getY()); tag.putDouble("z", target.getZ());
            tag.putFloat("width", MathHelper.clamp(target.getBbWidth(), 0.5F, 4));
            tag.putFloat("height", MathHelper.clamp(target.getBbHeight(), 0.8F, 8));
            targets.add(tag);
        }
        state.put("targets", targets);
    }
    private void sound(SoundEvent sound, float volume, float pitch) {
        level.playSound(null, getX(), getY(), getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
    }
    private void refreshBounds() { setBoundingBox(new AxisAlignedBB(position(), position()).inflate(
            getMode() == Mode.ABSOLUTE ? EnkiduAbility.ABSOLUTE_RADIUS + 8 : 16,
            getMode() == Mode.ABSOLUTE ? EnkiduAbility.ABSOLUTE_RADIUS + 8 : 12,
            getMode() == Mode.ABSOLUTE ? EnkiduAbility.ABSOLUTE_RADIUS + 8 : 16)); }
    private static final class Binding {
        final LivingEntity target;
        Vector3d anchor;
        Binding(LivingEntity target) { this.target = target; anchor = target.position(); }
    }
    public static final class VisualTarget {
        public final int id, seed;
        public final Vector3d anchor;
        public final float width, height;
        VisualTarget(CompoundNBT tag) {
            id = tag.getInt("id");
            seed = tag.getInt("seed");
            anchor = new Vector3d(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
            width = tag.getFloat("width"); height = tag.getFloat("height");
        }
        public Vector3d position(World world, float partial) {
            Entity target = id < 0 ? null : world.getEntity(id);
            return target == null || !target.isAlive() ? anchor : new Vector3d(
                    MathHelper.lerp(partial, target.xOld, target.getX()),
                    MathHelper.lerp(partial, target.yOld, target.getY()),
                    MathHelper.lerp(partial, target.zOld, target.getZ()));
        }
    }
    @Override protected void defineSynchedData() {
        entityData.define(STATE, new CompoundNBT());
        entityData.define(PROGRESS, 0.0F);
    }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { remove(); }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 160 * 160; }
}
