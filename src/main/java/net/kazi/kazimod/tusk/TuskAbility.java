package net.kazi.kazimod.tusk;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public final class TuskAbility extends Ability {
    public enum Move {
        MANIFEST("Tusk", "tusk", 1, 0, 0, 10, "Manifest or dismiss your Stand. Alternate mode cycles mastered Acts. Using this also displays your next mastery challenge."),
        NAIL("Nail Shot", "nail_shot", 1, 1, 0, 8, "Fire a spinning nail. Act 2 and above upgrade it into a Golden Nail; missed terrain shots leave a pursuing hole. Nails regenerate individually."),
        SHOTGUN("Nail Shotgun", "nail_shotgun", 1, 5, 0, 80, "Fire five nails in a spread. Each nail can hit once; concentrated shots can hit the same target."),
        GLIDE("Spin Glide", "spin_glide", 1, 0, 8, 140, "Slide quickly over the ground for 1.5 seconds. Steer with your aim; use again to stop."),
        HOLE("Pursuing Hole", "pursuing_hole", 2, 2, 0, 70, "Fire a golden drilling nail that leaves a surface-following hole. Aim at an enemy to mark them. Up to six holes persist for six seconds."),
        BURST("Spiral Burst", "spiral_burst", 2, 0, 15, 200, "Detonate your active holes in a 3.5-block radius. Overlapping bursts hit each enemy only once and cannot damage through walls."),
        SLIP("Wormhole Slip", "wormhole_slip", 3, 0, 20, 200, "Sink into a portal, then emerge up to 20 blocks along your aim. The destination must be clear and within the world border. Use again to cancel."),
        WORMHOLE("Wormhole Shot", "wormhole_shot", 3, 2, 10, 120, "Open a portal beside a visible enemy within 32 blocks and fire a nail through it from a different angle."),
        DODGE("Dimensional Dodge", "dimensional_dodge", 3, 0, 15, 240, "Slip partially into a wormhole for 0.6 seconds, avoiding ordinary attacks. Unavoidable damage and environmental hazards still apply."),
        BARRAGE("Stand Barrage", "stand_barrage", 4, 0, 25, 240, "Tusk unleashes a two-second punching barrage, ending with a powerful knockback strike. Use again to cancel."),
        BEATDOWN("Golden Beatdown", "golden_beatdown", 4, 0, 40, 400, "Grab a nearby enemy, then deliver coordinated Stand strikes and a heavy finisher. Missing leaves you briefly exposed. Use again to release."),
        INFINITE("Infinite Rotation", "infinite_rotation", 4, 1, 100, 2400, "Charge for 2.5 seconds and fire a golden nail that tracks the enemy you aim at when firing. A hit applies 10 seconds of rotation damage and slowing. Effects do not stack or refresh; this does not permanently stun. Use again to cancel.");
        public final String title, id, description;
        public final int act, nails, cooldown;
        public final float spin;
        Move(String title, String id, int act, int nails, float spin, int cooldown, String description) {
            this.title = title; this.id = id; this.act = act; this.nails = nails; this.spin = spin; this.cooldown = cooldown; this.description = description;
        }
    }
    public enum Act { ACT_1, ACT_2, ACT_3, ACT_4 }
    public static final AbilityCore<TuskAbility>[] CORES = createCores();
    @SuppressWarnings("unchecked") private static AbilityCore<TuskAbility>[] createCores() {
        AbilityCore<TuskAbility>[] result = new AbilityCore[Move.values().length];
        for (Move move : Move.values()) result[move.ordinal()] = new AbilityCore.Builder<TuskAbility>(move.title,
                AbilityCategory.DEVIL_FRUITS, core -> new TuskAbility(core, move))
                .setUnlockCheck(user -> TuskState.awakened(user) && TuskState.tier(user) >= move.act)
                .setSourceHakiNature(move == Move.BARRAGE || move == Move.BEATDOWN ? SourceHakiNature.HARDENING : SourceHakiNature.IMBUING)
                .setSourceType(move == Move.BARRAGE || move == Move.BEATDOWN ? SourceType.FIST : SourceType.PROJECTILE)
                .setSourceElement(SourceElement.NONE).setIcon(icon(move.id))
                .addDescriptionLine(new StringTextComponent(move.description))
                .addDescriptionLine(new StringTextComponent("Act " + move.act + "+ | Nails: " + move.nails + " | Spin: " + (int) move.spin
                        + " | Cooldown: " + move.cooldown / 20F + "s")).build();
        return result;
    }
    public static AbilityCore<TuskAbility> core(Move move) { return CORES[move.ordinal()]; }
    public static boolean isTusk(AbilityCore<?> core) { return CORES != null && Arrays.asList(CORES).contains(core); }
    public static ResourceLocation icon(String name) { return new ResourceLocation("kazimod", "textures/abilities/tusk/" + name + ".png"); }
    public final Move move;
    private final ContinuousComponent continuous = new ContinuousComponent(this, true);
    private final DealDamageComponent damage = new DealDamageComponent(this);
    private final GrabEntityComponent grab = new GrabEntityComponent(this, false, false, false, 1);
    private final AltModeComponent<Act> actMode = new AltModeComponent<Act>(this, Act.class, Act.ACT_1) {
        @Override public void setMode(LivingEntity user, Enum value) {
            if (TuskState.action(user) >= 3) return;
            super.setMode(user, value.ordinal() < TuskState.tier(user) ? value : Act.ACT_1);
        }
    };
    private int ticks;
    private Vector3d destination;
    private LivingEntity captive;

    public TuskAbility(AbilityCore<TuskAbility> core, Move move) {
        super(core); this.move = move; isNew = true;
        addComponents(continuous, damage);
        if (move == Move.BEATDOWN) addComponents(grab);
        if (move == Move.MANIFEST) {
            addComponents(actMode);
            actMode.addChangeModeEvent((user, ability, selected) -> {
                if (user.level.isClientSide || !TuskState.awakened(user)) return;
                int next = Math.min(TuskState.tier(user), selected.ordinal() + 1);
                if (next != TuskState.act(user)) { TuskState.get(user).putInt("Act", next); TuskState.pose(user, 1, 32); }
                TuskState.get(user).putBoolean("Manifest", true); TuskFeatures.sync((PlayerEntity) user);
            });
            addTickEvent((user, ability) -> {
                int act = TuskState.act(user);
                setDisplayName(new StringTextComponent("Tusk: Act " + act)); setDisplayIcon(icon("act_" + act));
                if (!user.level.isClientSide && actMode.getCurrentMode().ordinal() + 1 != act && TuskState.action(user) < 3)
                    actMode.setMode(user, Act.values()[act - 1]);
            });
        }
        if (move == Move.NAIL) addTickEvent((user, ability) -> setDisplayName(new StringTextComponent(TuskState.act(user) >= 2 ? "Golden Nail" : "Nail Shot")));
        addCanUseCheck((user, ability) -> {
            if (!(user instanceof PlayerEntity) || !TuskState.awakened(user) || TuskState.act(user) < move.act)
                return fail("Select a mastered Tusk Act that supports this technique.");
            if (continuous.isContinuous()) return AbilityUseResult.success();
            if (AbilityHelper.isWeakenedByKairosekiOrWater(user)) return fail("Your Devil Fruit is weakened.");
            if (TuskState.action(user) >= 3) return fail("Finish your current Tusk technique first.");
            if (TuskState.nails(user) + .0001F < move.nails) return fail("Your nails are still regrowing.");
            if (TuskState.spin(user) + .0001F < move.spin) return fail("Build more Golden Spin by landing attacks and moving in combat.");
            if (move == Move.GLIDE && !user.isOnGround()) return fail("Spin Glide needs solid ground.");
            if (move == Move.BURST && TuskProjectile.holes(user).isEmpty()) return fail("There are no active holes to detonate.");
            if (move == Move.WORMHOLE && TuskFeatures.aim(user, 32) == null) return fail("Aim at a visible enemy within 32 blocks.");
            return AbilityUseResult.success();
        });
        addUseEvent((user, ability) -> useMove(user));
        continuous.addTickEvent((user, ability) -> tickMove(user));
        continuous.addEndEvent((user, ability) -> end(user));
        addRemoveEvent((user, ability) -> { if (continuous.isContinuous()) continuous.stopContinuity(user); });
    }
    private AbilityUseResult fail(String message) { return AbilityUseResult.fail(new StringTextComponent(message)); }

    private void useMove(LivingEntity user) {
        if (user.level.isClientSide) return;
        if (continuous.isContinuous()) { continuous.stopContinuity(user); return; }
        if (!TuskState.awakened(user) || TuskState.act(user) < move.act) return;
        if (move == Move.MANIFEST) {
            CompoundNBT n = TuskState.get(user); n.putBoolean("Manifest", !n.getBoolean("Manifest"));
            TuskState.pose(user, n.getBoolean("Manifest") ? 1 : 12, 24);
            ((PlayerEntity) user).displayClientMessage(new StringTextComponent(TuskState.mastery(user)), false);
            cooldownComponent.startCooldown(user, move.cooldown); sync(user); return;
        }
        if (move == Move.SLIP) {
            destination = slipDestination(user);
            if (destination == null) { ((PlayerEntity) user).displayClientMessage(new StringTextComponent("No safe portal exit in that direction."), true); return; }
        }
        LivingEntity aimed = move == Move.WORMHOLE ? TuskFeatures.aim(user, 32) : null;
        Vector3d exit = aimed == null ? null : portalExit(user, aimed);
        if (move == Move.WORMHOLE && exit == null) return;
        if (!TuskState.spend(user, move.nails, move.spin)) return;
        TuskState.get(user).putBoolean("Manifest", true);
        switch (move) {
            case NAIL: shoot(user, TuskProjectile.NAIL, hand(user), user.getLookAngle(), 10 + (TuskState.act(user) - 1) * 3, 0); pose(user, 2, 10); break;
            case SHOTGUN:
                for (int i = -2; i <= 2; i++) shoot(user, TuskProjectile.SHOTGUN, hand(user), user.getLookAngle().yRot(i * .065F), 6 + TuskState.act(user), i);
                pose(user, 4, 14); break;
            case HOLE: shoot(user, TuskProjectile.SEEKER, hand(user), user.getLookAngle(), 16, 0); pose(user, 5, 14); break;
            case BURST: burst(user); pose(user, 6, 20); break;
            case WORMHOLE:
                TuskEffect.spawn(user, TuskEffect.PORTAL, exit, 22, .7F);
                shoot(user, TuskProjectile.WORMHOLE, exit, aimed.getBoundingBox().getCenter().subtract(exit).normalize(), 22, 0);
                pose(user, 8, 18); break;
            case DODGE:
                TuskState.get(user).putLong("DodgeUntil", user.level.getGameTime() + 12);
                TuskState.get(user).putBoolean("DodgeCredited", false);
                TuskEffect.spawn(user, TuskEffect.PORTAL, user.position(), 16, 1); pose(user, 13, 12); break;
            default:
                ticks = 0; captive = null; continuous.startContinuity(user);
                pose(user, action(), move == Move.INFINITE ? 52 : move == Move.BEATDOWN ? 52 : move == Move.BARRAGE ? 42 : move == Move.SLIP ? 20 : 30);
                if (move == Move.SLIP) TuskEffect.spawn(user, TuskEffect.PORTAL, user.position(), 22, 1.2F);
                if (move == Move.INFINITE) TuskFeatures.play(user, SoundEvents.BEACON_POWER_SELECT, .7F, .7F);
                sync(user); return;
        }
        cooldownComponent.startCooldown(user, move.cooldown); sync(user);
    }
    private void pose(LivingEntity user, int action, int duration) { TuskState.pose(user, action, duration); }
    private int action() {
        switch (move) { case GLIDE: return 3; case SLIP: return 7; case BARRAGE: return 9; case BEATDOWN: return 10; case INFINITE: return 11; default: return 0; }
    }
    private void tickMove(LivingEntity user) {
        if (user.level.isClientSide) return;
        if (!user.isAlive() || !TuskState.awakened(user) || TuskState.act(user) < move.act || AbilityHelper.isWeakenedByKairosekiOrWater(user)) {
            continuous.stopContinuity(user); return;
        }
        ticks++;
        switch (move) {
            case GLIDE:
                Vector3d forward = user.getLookAngle().multiply(1, 0, 1).normalize();
                AbilityHelper.setDeltaMovement(user, forward.scale(.8).add(0, Math.min(0, user.getDeltaMovement().y), 0));
                if (ticks >= 30 || user.horizontalCollision || !user.isOnGround()) continuous.stopContinuity(user);
                break;
            case SLIP:
                user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
                AbilityHelper.setDeltaMovement(user, Vector3d.ZERO);
                if (ticks == 12 && destination != null && TuskFeatures.safe(user, destination)) {
                    TuskFeatures.teleport(user, destination); TuskEffect.spawn(user, TuskEffect.PORTAL, destination, 18, 1.2F);
                    TuskFeatures.play(user, SoundEvents.ENDERMAN_TELEPORT, .45F, 1.2F);
                }
                if (ticks >= 20) continuous.stopContinuity(user);
                break;
            case BARRAGE:
                user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
                if (ticks % 5 == 0) {
                    LivingEntity target = TuskFeatures.aim(user, 4);
                    if (target != null) {
                        target.invulnerableTime = 0;
                        if (damage.hurtTarget(user, target, ticks >= 40 ? 15 : 3.5F)) {
                            TuskFeatures.landed(user, target, TuskProjectile.MELEE, false);
                            TuskEffect.spawn(user, TuskEffect.IMPACT, target.getBoundingBox().getCenter(), 10, ticks >= 40 ? 1.5F : .6F);
                            if (ticks >= 40) AbilityHelper.setDeltaMovement(target, user.getLookAngle().scale(1.7).add(0, .35, 0));
                        }
                    }
                    TuskFeatures.play(user, SoundEvents.PLAYER_ATTACK_STRONG, .4F, 1.25F);
                }
                if (ticks >= 40) continuous.stopContinuity(user);
                break;
            case BEATDOWN: beatdown(user); break;
            case INFINITE:
                user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
                AbilityHelper.setDeltaMovement(user, Vector3d.ZERO);
                if (ticks >= 50) {
                    shoot(user, TuskProjectile.INFINITE, hand(user), user.getLookAngle(), 32, 0);
                    TuskFeatures.play(user, SoundEvents.GENERIC_EXPLODE, .55F, 1.35F);
                    continuous.stopContinuity(user);
                }
                break;
            default: continuous.stopContinuity(user);
        }
    }
    private void beatdown(LivingEntity user) {
        user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
        if (ticks == 6) {
            LivingEntity target = TuskFeatures.aim(user, 3.5);
            if (target != null && !target.isPassenger() && grab.grabManually(user, target)) captive = target;
        }
        if (captive == null) { if (ticks >= 18) continuous.stopContinuity(user); return; }
        if (!captive.isAlive() || captive.level != user.level || user.distanceToSqr(captive) > 25
                || !TuskFeatures.canHit(user, captive) || !user.canSee(captive) || !grab.canContinueGrab(user)) {
            continuous.stopContinuity(user); return;
        }
        captive.getPersistentData().putLong("KaziTuskGrabUntil", user.level.getGameTime() + 3);
        captive.addEffect(new EffectInstance(ModEffects.GRABBED.get(), 3, 3, false, false));
        captive.stopUsingItem(); AbilityHelper.setDeltaMovement(captive, Vector3d.ZERO);
        if (ticks == 12 || ticks == 20 || ticks == 28 || ticks == 36 || ticks == 44) {
            captive.invulnerableTime = 0;
            if (damage.hurtTarget(user, captive, ticks == 44 ? 24 : 6)) {
                TuskEffect.spawn(user, TuskEffect.IMPACT, captive.getBoundingBox().getCenter(), 12, ticks == 44 ? 2 : .9F);
                TuskFeatures.landed(user, captive, TuskProjectile.MELEE, false);
                TuskFeatures.play(user, SoundEvents.PLAYER_ATTACK_STRONG, .65F, ticks == 44 ? .65F : 1);
            }
            if (ticks == 44) {
                LivingEntity released = captive; release(user);
                AbilityHelper.setDeltaMovement(released, user.getLookAngle().scale(2).add(0, .5, 0));
                continuous.stopContinuity(user);
            }
        }
        if (ticks >= 50) continuous.stopContinuity(user);
    }
    private void end(LivingEntity user) {
        if (user.level.isClientSide) return;
        release(user); destination = null;
        if (TuskState.action(user) == action()) TuskState.pose(user, 0, 0);
        if (move == Move.GLIDE) AbilityHelper.setDeltaMovement(user, user.getDeltaMovement().multiply(.25, 1, .25));
        cooldownComponent.startCooldown(user, move.cooldown); sync(user);
    }
    private void release(LivingEntity user) {
        if (captive != null) captive.getPersistentData().remove("KaziTuskGrabUntil");
        if (grab.hasGrabbedEntity()) grab.release(user);
        captive = null;
    }
    private void sync(LivingEntity user) { if (user instanceof PlayerEntity) TuskFeatures.sync((PlayerEntity) user); }
    private static Vector3d hand(LivingEntity user) { return user.getEyePosition(1).add(0, -.18, 0).add(user.getLookAngle().scale(.65)); }
    private void shoot(LivingEntity user, int kind, Vector3d origin, Vector3d direction, float power, int spread) {
        TuskProjectile projectile = new TuskProjectile(user, getCore(), kind, power);
        LivingEntity target = TuskFeatures.aim(user, 48); if (target != null) projectile.target(target);
        projectile.setPos(origin.x, origin.y, origin.z);
        projectile.shoot(direction.x, direction.y + Math.abs(spread) * .007, direction.z, kind == TuskProjectile.INFINITE ? 1.8F : 2.6F, 0);
        user.level.addFreshEntity(projectile);
        if (spread == 0) { TuskEffect.spawn(user, TuskEffect.MUZZLE, origin, 8, .5F); TuskFeatures.play(user, SoundEvents.TRIDENT_THROW, .45F, 1.8F); }
    }
    private void burst(LivingEntity user) {
        Set<UUID> hit = new HashSet<>(); boolean landed = false;
        for (TuskProjectile hole : TuskProjectile.holes(user)) {
            Vector3d center = hole.position().add(0, .3, 0);
            TuskEffect.spawn(user, TuskEffect.BURST, center, 22, 3.5F);
            for (LivingEntity target : user.level.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(center, center).inflate(3.5))) {
                if (!TuskFeatures.canHit(user, target) || target.distanceToSqr(center) > 12.25 || hit.contains(target.getUUID())) continue;
                if (user.level.clip(new RayTraceContext(center, target.getBoundingBox().getCenter(), RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user)).getType() == RayTraceResult.Type.BLOCK) continue;
                hit.add(target.getUUID());
                if (damage.hurtTarget(user, target, 20)) { landed = true; target.knockback(.6F, center.x - target.getX(), center.z - target.getZ()); TuskFeatures.landed(user, target, TuskProjectile.MELEE, false); }
            }
            hole.remove();
        }
        if (landed) TuskState.progress(user, "Bursts");
        TuskFeatures.play(user, SoundEvents.GENERIC_EXPLODE, .55F, 1.6F);
    }
    private Vector3d slipDestination(LivingEntity user) {
        Vector3d from = user.getEyePosition(1), end = from.add(user.getLookAngle().scale(20));
        Vector3d impact = user.level.clip(new RayTraceContext(from, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user)).getLocation();
        for (double distance = Math.min(20, from.distanceTo(impact) - .8); distance >= 2; distance -= .5) {
            Vector3d at = from.add(user.getLookAngle().scale(distance)).add(0, -user.getEyeHeight(), 0);
            if (TuskFeatures.safe(user, at)) return at;
        }
        return null;
    }
    private Vector3d portalExit(LivingEntity user, LivingEntity target) {
        Vector3d center = target.getBoundingBox().getCenter();
        for (int i = 0; i < 8; i++) {
            double angle = user.yRot * Math.PI / 180 + (i * Math.PI / 4);
            Vector3d at = center.add(Math.cos(angle) * 2.2, .25, Math.sin(angle) * 2.2);
            if (!user.level.hasChunkAt(new BlockPos(at)) || !user.level.getWorldBorder().isWithinBounds(new BlockPos(at))) continue;
            if (!user.level.noCollision(new AxisAlignedBB(at, at).inflate(.25))) continue;
            if (user.level.clip(new RayTraceContext(at, center, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user)).getType() != RayTraceResult.Type.BLOCK) return at;
        }
        return null;
    }
}
