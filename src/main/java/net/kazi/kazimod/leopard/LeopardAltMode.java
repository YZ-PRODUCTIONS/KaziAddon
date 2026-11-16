package net.kazi.kazimod.leopard;

import java.util.Comparator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

/** Keeps both modes on the native ability's components, cooldown and network state. */
public final class LeopardAltMode extends AltModeComponent<LeopardAltMode.Mode> {
    public enum Mode { NORMAL, AWAKENED }
    private final Ability host;
    private final ContinuousComponent continuous;
    private final CooldownComponent cooldown;
    private final DealDamageComponent damage;
    private final RangeComponent range;
    private final GrabEntityComponent grab;
    public final boolean barrage;
    private Vector3d direction = Vector3d.ZERO;
    private int capturedAt = -1;

    public LeopardAltMode(Ability host, boolean barrage, boolean legacy) {
        super(host, Mode.class, legacy ? Mode.AWAKENED : Mode.NORMAL, true);
        this.host = host;
        this.barrage = barrage;
        continuous = component(host, ContinuousComponent.class, new ContinuousComponent(host));
        cooldown = component(host, CooldownComponent.class, new CooldownComponent(host));
        damage = component(host, DealDamageComponent.class, new DealDamageComponent(host));
        range = component(host, RangeComponent.class, new RangeComponent(host));
        grab = barrage ? null : new GrabEntityComponent(host, true, true, false, 1.6F);
        host.addComponents(continuous, cooldown, damage, range, this);
        if (grab != null) host.addComponents(grab, new PoolComponent(host, ModAbilityPools.GRAB_ABILITY));
        addChangeModeEvent((u, a, m) -> updateDisplay(m));
        host.addCanUseCheck((u, a) -> {
            syncForm(u);
            return !alternate() || LeopardFeatures.empowered(u) && !u.isPassenger()
                ? AbilityUseResult.success() : AbilityUseResult.fail(new TranslationTextComponent("message.kazimod.leopard.form_required"));
        });
        host.addEquipEvent((u, a) -> syncForm(u));
        host.addTickEvent((u, a) -> syncForm(u));
        host.addRemoveEvent((u, a) -> { if (active()) continuous.stopContinuity(u); if (grab != null) grab.release(u); });
        if (legacy) {
            host.addUseEvent((u, a) -> use(u));
            continuous.addStartEvent((u, a) -> start(u));
            continuous.addTickEvent((u, a) -> tickAttack(u));
            continuous.addEndEvent((u, a) -> end(u));
        }
        updateDisplay(getCurrentMode());
    }

    private static <T extends AbilityComponent<?>> T component(Ability host, Class<T> type, T fallback) {
        return host.getComponents().values().stream().filter(type::isInstance).map(type::cast).findFirst().orElse(fallback);
    }
    public static LeopardAltMode get(Object ability) {
        if (!(ability instanceof IAbility)) return null;
        for (AbilityComponent<?> c : ((IAbility)ability).getComponents().values()) if (c instanceof LeopardAltMode) return (LeopardAltMode)c;
        return null;
    }
    public static boolean alternate(Object ability) { LeopardAltMode m = get(ability); return m != null && m.alternate(); }
    public boolean alternate() { return isMode(Mode.AWAKENED); }
    public boolean active() { return alternate() && continuous.isContinuous(); }
    public boolean pinning() { return active() && capturedAt >= 0; }
    public float strikeTime() { return Math.max(0, continuous.getContinueTime() - capturedAt) / 20F; }

    @Override public void setNextInCycle(LivingEntity user) {
        syncForm(user);
    }
    @Override public void setMode(LivingEntity user, Enum mode) {
        if (user.level.isClientSide) super.setMode(user, mode);
        else syncForm(user);
    }
    public void syncForm(LivingEntity user) {
        if (user.level.isClientSide) return;
        Mode desired = LeopardFeatures.awakened(user) && LeopardFeatures.active(user) ? Mode.AWAKENED : Mode.NORMAL;
        if (getCurrentMode() == desired) return;
        // End the outgoing move using its own callbacks before changing modes.
        if (continuous.isContinuous()) continuous.stopContinuity(user);
        super.setMode(user, desired);
    }
    private void updateDisplay(Mode mode) {
        boolean alt = mode == Mode.AWAKENED;
        String key = alt ? (barrage ? "shugan_barrage" : "predator_hunt") : (barrage ? "claw_strike" : "ferocious_leap");
        host.setDisplayName(new TranslationTextComponent("ability." + (alt ? "kazimod." : "mineminenomi.") + key));
        host.setDisplayIcon(new ResourceLocation(alt && barrage ? "kazimod" : "mineminenomi",
                "textures/abilities/" + (alt && !barrage ? "ferocious_leap" : key) + ".png"));
    }
    public void use(LivingEntity user) { continuous.triggerContinuity(user, barrage ? 40 : 12); }
    public void start(LivingEntity user) {
        capturedAt = -1;
        direction = user.getLookAngle();
        if (user.isOnGround() && direction.y < .1) direction = new Vector3d(direction.x, .16, direction.z).normalize();
    }
    public void end(LivingEntity user) {
        if (grab != null) {
            grab.release(user); capturedAt = -1;
            AbilityHelper.setDeltaMovement(user, Vector3d.ZERO);
            if (!user.level.isClientSide) user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 6, 0, false, false));
        }
        if (!user.level.isClientSide) cooldown.startCooldown(user, barrage ? 240 : 280);
    }
    public void tickAttack(LivingEntity user) {
        if (!LeopardFeatures.empowered(user) || user.isPassenger()) { continuous.stopContinuity(user); return; }
        int t = (int)continuous.getContinueTime();
        if (barrage) {
            if (user.level.isClientSide || t % 4 != 0) return;
            user.level.playSound(null, user.blockPosition(), ModSounds.SHIGAN_SFX.get(), SoundCategory.PLAYERS, .7F, 1.05F);
            for (LivingEntity target : range.getTargetsInLine(user, 1.25F, 4F)) if (user.canSee(target)) hit(user, target, 6);
            return;
        }
        if (capturedAt >= 0) {
            AbilityHelper.setDeltaMovement(user, Vector3d.ZERO); user.fallDistance = 0;
            if (user.level.isClientSide) return;
            LivingEntity target = grab.getGrabbedEntity();
            if (target == null || !grab.canContinueGrab(user) || target.level != user.level || !user.canSee(target)) { continuous.stopContinuity(user); return; }
            user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
            int strike = t - capturedAt;
            if (strike == 5 || strike == 10) {
                hit(user, target, strike == 10 ? 18 : 12);
                user.level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1, .8F);
            }
            if (strike >= 11) {
                Vector3d push = direction.scale(1.1).add(0, .25, 0);
                grab.release(user); AbilityHelper.setDeltaMovement(target, push); continuous.stopContinuity(user);
            }
            return;
        }
        Vector3d before = user.position();
        boolean moved = LeopardMobility.move(user, direction.scale(1.25));
        if (user.level.isClientSide) return;
        AxisAlignedBB sweep = user.getBoundingBox().expandTowards(before.subtract(user.position())).inflate(.3);
        LivingEntity closest = TargetHelper.getEntitiesInArea(user, 6, null, LivingEntity.class).stream()
                .filter(e -> e != user && e.isAlive() && !e.isPassenger() && !e.hasEffect(ModEffects.GRABBED.get()) && sweep.intersects(e.getBoundingBox()) && user.canSee(e))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(before))).orElse(null);
        if (closest != null) {
            if (damage.hurtTarget(user, closest, 6) && grab.grabManually(user, closest)) {
                capturedAt = Math.max(1, t); grab.setState(GrabEntityComponent.GrabState.GRABBED);
                // Changing the threshold resets the native clock; restore it before syncing the pin.
                continuous.setThresholdTime(user, capturedAt + 13);
                continuous.increaseContinuityTime(capturedAt);
                WyNetwork.sendToAllTrackingAndSelf(new SSyncAbilityDataPacket(user.getId(), AbilityDataCapability.get(user)), user);
            } else continuous.stopContinuity(user);
        } else if (!moved || user.horizontalCollision) continuous.stopContinuity(user);
    }
    private void hit(LivingEntity user, LivingEntity target, float amount) {
        int old = target.invulnerableTime;
        try { target.invulnerableTime = 0; damage.hurtTarget(user, target, amount); }
        finally { target.invulnerableTime = Math.max(old, target.invulnerableTime); }
    }
    @Override public CompoundNBT save() {
        CompoundNBT tag = super.save();
        tag.putInt("leopardCapturedAt", capturedAt);
        tag.putDouble("leopardHuntX", direction.x); tag.putDouble("leopardHuntY", direction.y); tag.putDouble("leopardHuntZ", direction.z);
        return tag;
    }
    @Override public void load(CompoundNBT tag) {
        super.load(tag);
        capturedAt = tag.contains("leopardCapturedAt") ? tag.getInt("leopardCapturedAt") : -1;
        Vector3d d = new Vector3d(tag.getDouble("leopardHuntX"), tag.getDouble("leopardHuntY"), tag.getDouble("leopardHuntZ"));
        direction = Double.isFinite(d.lengthSqr()) ? d.normalize() : Vector3d.ZERO;
        updateDisplay(getCurrentMode());
    }
}
