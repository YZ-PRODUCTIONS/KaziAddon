package net.kazi.kazimod.leopard;

import java.util.Map;
import net.kazi.kazimod.models.zoan.CerberusRig;

/** Per-entity animation state, kept independent of the shared morph model. */
final class LeopardAnimation {
    private float age = Float.NaN, phase, motion, run, air, crouch, block, swim, claws, descent, pounce;
    private float yaw, pitch, turn, previousBodyYaw, previousAttack;
    private float landedAt = -10000, airborneAt, attackedAt = -10000;
    private boolean wasGrounded, leftStrike;
    private int strikes;

    void update(float age, float amount, boolean sprinting, boolean grounded, boolean water,
                boolean crouching, boolean blocking, boolean leap, boolean clawsReady,
                float attack, boolean leftHand, float yaw, float pitch, float velocityY, float bodyYaw) {
        float delta = age - this.age;
        boolean fresh = !Float.isFinite(this.age) || delta < 0 || delta > 8;
        float dt = fresh ? 0 : clamp(delta, 0, 2);
        float blend = fresh ? 1 : 1 - (float)Math.exp(-dt / 2.5F);
        this.age = age;
        boolean airborne = !grounded && !water || leap;
        if (fresh) {
            previousBodyYaw = bodyYaw;
            previousAttack = attack;
            landedAt = attackedAt = -10000;
            airborneAt = age;
        } else {
            if (!wasGrounded && grounded && !water) landedAt = age;
            if (wasGrounded && airborne) airborneAt = age;
            if (attack > 0 && (previousAttack <= 0 || attack < previousAttack - .15F)) {
                attackedAt = age;
                leftStrike = leftHand != (++strikes % 2 == 0);
            }
        }
        motion = lerp(motion, clamp(amount * 2.2F, 0, 1), blend);
        run = lerp(run, sprinting || amount > .65F ? 1 : 0, blend);
        air = lerp(air, airborne ? 1 : 0, blend);
        crouch = lerp(crouch, crouching ? 1 : 0, blend);
        block = lerp(block, blocking ? 1 : 0, blend);
        swim = lerp(swim, water ? 1 : 0, blend);
        claws = lerp(claws, clawsReady ? 1 : 0, blend);
        descent = lerp(descent, clamp(-velocityY * 3, 0, 1), blend);
        pounce = lerp(pounce, leap ? 1 : 0, blend);
        this.yaw = lerp(this.yaw, clamp(yaw, -45, 45), blend);
        this.pitch = lerp(this.pitch, clamp(pitch, -35, 35), blend);
        float yawDelta = (bodyYaw - previousBodyYaw) % 360;
        if (yawDelta > 180) yawDelta -= 360;
        if (yawDelta < -180) yawDelta += 360;
        if (fresh || dt > 0) turn = lerp(turn, dt > 0 ? clamp(yawDelta / dt, -12, 12) : 0, blend);
        // Both gaits share a phase, so changing speed never restarts a footfall.
        phase = (phase + dt / 20 * lerp(.85F, 1.65F, run) * lerp(.6F, 1.25F, motion) * motion) % 1;
        previousBodyYaw = bodyYaw;
        previousAttack = attack;
        wasGrounded = grounded;
    }

    Map<String, CerberusRig.Pose> sample(CerberusRig rig, boolean leap, float transformSeconds) {
        Map<String, CerberusRig.Pose> result = rig.pose();
        float seconds = age / 20;
        rig.apply(result, "idle", seconds, 1 - motion);
        rig.apply(result, "walk", phase * rig.clip("walk").length, motion * (1 - run));
        rig.apply(result, "run", phase * rig.clip("run").length, motion * run);
        replace(rig, result, "crouch", seconds, crouch * (1 - air), true);
        if (air > .001F) {
            Map<String, CerberusRig.Pose> flying = rig.pose();
            rig.apply(flying, "jump", seconds, (1 - descent) * (1 - pounce));
            rig.apply(flying, "pounce", (age - airborneAt) / 20, (1 - descent) * pounce);
            rig.apply(flying, "fall", seconds, descent);
            blend(result, flying, air, true);
        }
        replace(rig, result, "swim", seconds, swim, true);
        float landing = (age - landedAt) / 20;
        if (landing >= 0 && landing < .42F)
            replace(rig, result, "land", landing, smooth(landing / .06F) * (1 - smooth((landing - .29F) / .13F)) * (1 - air), false);
        replace(rig, result, "claw_ready", seconds, claws * (1 - air) * .85F, false);
        float attacking = (age - attackedAt) / 20;
        if (attacking >= 0 && attacking < .55F)
            replace(rig, result, leftStrike ? "slash_left" : "slash_right", attacking,
                    smooth(attacking / .065F) * (1 - smooth((attacking - .39F) / .16F)) * (1 - block), false);
        replace(rig, result, "block", seconds, block, false);
        float transforming = transformSeconds >= 0 && transformSeconds < 1.5F
                ? smooth(transformSeconds / .14F) * (1 - smooth((transformSeconds - 1.2F) / .3F)) : 0;
        replace(rig, result, "transform", transformSeconds, transforming, true);
        CerberusRig.Pose head = result.computeIfAbsent("Head", key -> new CerberusRig.Pose());
        head.rotation[1] += yaw * .72F * (1 - transforming);
        head.rotation[0] -= pitch * .65F * (1 - transforming);
        for (int i = 0; i < 6; i++) {
            CerberusRig.Pose tail = result.computeIfAbsent("Tail " + i, key -> new CerberusRig.Pose());
            tail.rotation[1] -= turn * (.35F - i * .035F) * motion;
        }
        return result;
    }

    private static void replace(CerberusRig rig, Map<String, CerberusRig.Pose> result,
                                String clip, float time, float weight, boolean all) {
        if (weight <= .001F) return;
        Map<String, CerberusRig.Pose> overlay = rig.pose();
        rig.apply(overlay, clip, time, 1);
        blend(result, overlay, clamp(weight, 0, 1), all);
    }

    private static void blend(Map<String, CerberusRig.Pose> result,
                              Map<String, CerberusRig.Pose> overlay, float weight, boolean all) {
        if (all) for (Map.Entry<String, CerberusRig.Pose> entry : result.entrySet()) {
            if (!overlay.containsKey(entry.getKey())) for (int axis = 0; axis < 3; axis++) {
                entry.getValue().rotation[axis] *= 1 - weight;
                entry.getValue().position[axis] *= 1 - weight;
            }
        }
        for (Map.Entry<String, CerberusRig.Pose> entry : overlay.entrySet()) {
            CerberusRig.Pose target = result.computeIfAbsent(entry.getKey(), key -> new CerberusRig.Pose());
            CerberusRig.Pose value = entry.getValue();
            for (int axis = 0; axis < 3; axis++) {
                target.rotation[axis] = lerp(target.rotation[axis], value.rotation[axis], weight);
                target.position[axis] = lerp(target.position[axis], value.position[axis], weight);
            }
        }
    }

    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private static float lerp(float from, float to, float t) { return from + (to - from) * t; }
    private static float smooth(float t) { t = clamp(t, 0, 1); return t * t * (3 - 2 * t); }
}
