package net.kazi.kazimod.blood;

import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;

public final class BloodPose {
    static float age(LivingEntity p, float partial) {
        return p.level.getGameTime() - BloodFeatures.state(p).getLong("Start") + partial;
    }
    static float releaseAge(LivingEntity p, float partial) {
        CompoundNBT n = BloodFeatures.state(p);
        return n.contains("Released") && n.getLong("Released") >= n.getLong("Start")
                ? p.level.getGameTime() - n.getLong("Released") + partial : -1;
    }
    static float gestureAge(LivingEntity p, float partial) {
        CompoundNBT n = BloodFeatures.state(p);
        return n.contains("NovaAt") ? p.level.getGameTime() - n.getLong("NovaAt") + partial : 100;
    }
    static boolean holdingNova(LivingEntity p) { return BloodFeatures.state(p).getLong("NovaHoldUntil") > p.level.getGameTime(); }
    static boolean apply(PlayerModel<?> m, LivingEntity p, float time) {
        if (!p.isAlive()) return false;
        CompoundNBT n = BloodFeatures.state(p);
        int action = BloodFeatures.action(p);
        float partial = MathHelper.clamp(time - p.tickCount, 0, 1), age = age(p, partial);
        float gesture = gestureAge(p, partial);
        float blend = BloodMesh.smooth(age / 5);
        if ((action < 0 || action == 1) && gesture < 12) {
            boolean burst = n.getInt("NovaGesture") == 2;
            float close = BloodMesh.smooth(gesture / (burst ? 2 : 4));
            float fade = 1 - BloodMesh.smooth((gesture - 5) / 7);
            together(m, MathHelper.lerp(close, 1.8F, .15F), burst ? 2.7F : 5.2F,
                    burst ? 8.8F : 10.4F, fade);
            stance(m, fade, burst ? .08F : -.06F);
        } else if (action < 0 && holdingNova(p)) {
            together(m, .2F, 5.8F, 8.4F, 1);
            stance(m, .8F, .04F);
        } else if (action < 0 && n.getLong("TetherUntil") > p.level.getGameTime()) {
            arm(m.rightArm, -1.55F + m.head.xRot * .65F, m.head.yRot, .02F, 1);
            arm(m.leftArm, -.8F, .45F, -.2F, 1);
            stance(m, .7F, -.08F);
        } else if (action == 0) {
            float duration = BloodFeatures.scale(p) ? 22 : 30;
            float compression = BloodMesh.smooth((age - 4) / (duration - 10));
            float release = releaseAge(p, partial);
            float thrust = release < 0 ? BloodMesh.smooth((age - duration + 6) / 5)
                    : 1 - .16F * (float)Math.sin(Math.min(1, release / 6) * Math.PI);
            float fade = release < 0 ? 1 : 1 - BloodMesh.smooth((release - n.getInt("FiringTicks")) / 6);
            together(m, MathHelper.lerp(compression, 2.1F, .16F),
                    6.8F - compression * 1.2F - thrust * 1.4F, 7.4F + thrust * 3, blend * fade);
            stance(m, blend * fade, -.05F);
        } else if (action == 1) {
            together(m, MathHelper.lerp(BloodMesh.smooth(age / 8), 2.2F, .2F),
                    5.8F, 8.4F, blend);
            stance(m, blend * .8F, .04F);
        } else if (action == 2) {
            float cut = BloodMesh.smooth((age - 4) / 4), fade = 1 - BloodMesh.smooth((age - 9) / 5);
            arm(m.rightArm, -1.05F - cut * .3F, -1.2F + cut * 2.3F, .18F - cut * .4F, blend * fade);
            arm(m.leftArm, -.65F, .35F, -.16F, blend * fade);
            stance(m, blend * fade, -.18F + cut * .32F);
        } else if (action == 4) {
            float punch = BloodMesh.smooth((age - 4) / 4);
            arm(m.rightArm, -.45F - punch * 1.1F + m.head.xRot * .65F,
                    -.4F + punch * .4F + m.head.yRot, .12F * (1 - punch), blend);
            arm(m.leftArm, -.9F, .4F, -.22F, blend);
            stance(m, blend, -.1F + punch * .12F);
        } else if (action == 5) {
            float fade = 1 - BloodMesh.smooth((age - 10) / 8);
            arm(m.rightArm, -2.1F, -.4F, .1F, blend * fade);
            arm(m.leftArm, -.6F, .2F, -.12F, blend * fade);
        } else return false;
        m.rightSleeve.copyFrom(m.rightArm); m.leftSleeve.copyFrom(m.leftArm);
        m.jacket.copyFrom(m.body); m.hat.copyFrom(m.head);
        m.rightPants.copyFrom(m.rightLeg); m.leftPants.copyFrom(m.leftLeg);
        return true;
    }
    private static void together(PlayerModel<?> m, float gap, float height, float reach, float weight) {
        float pitch = MathHelper.clamp(m.head.xRot, -1.1F, 1.1F);
        float yaw = MathHelper.clamp(m.head.yRot, -.9F, .9F);
        float x = -MathHelper.sin(yaw) * reach, z = -MathHelper.cos(yaw) * reach * MathHelper.cos(pitch);
        float y = height + MathHelper.sin(pitch) * reach;
        point(m.rightArm, x - gap, y, z, weight, -1);
        point(m.leftArm, x + gap, y, z, weight, 1);
    }
    private static void point(ModelRenderer a, float x, float y, float z, float weight, int side) {
        float dx = x - a.x - side, dy = y - a.y, dz = z - a.z;
        arm(a, -(float)Math.atan2(Math.sqrt(dx * dx + dz * dz), dy),
                (float)Math.atan2(-dx, -dz), 0, weight);
    }
    private static void stance(PlayerModel<?> m, float weight, float twist) {
        // Rotate the torso and its shoulder anchors together, without separating body parts.
        float yaw = twist * weight;
        m.body.yRot += yaw;
        for (ModelRenderer arm : new ModelRenderer[]{m.rightArm, m.leftArm}) {
            float x = arm.x, z = arm.z;
            arm.x = x * MathHelper.cos(yaw) + z * MathHelper.sin(yaw);
            arm.z = z * MathHelper.cos(yaw) - x * MathHelper.sin(yaw);
        }
        m.rightLeg.xRot = MathHelper.lerp(weight * .6F, m.rightLeg.xRot, -.17F);
        m.leftLeg.xRot = MathHelper.lerp(weight * .6F, m.leftLeg.xRot, .21F);
        m.rightLeg.zRot = MathHelper.lerp(weight, m.rightLeg.zRot, .07F);
        m.leftLeg.zRot = MathHelper.lerp(weight, m.leftLeg.zRot, -.07F);
    }
    private static void arm(ModelRenderer arm, float x, float y, float z, float mix) {
        arm.xRot = MathHelper.lerp(mix, arm.xRot, x);
        arm.yRot = MathHelper.lerp(mix, arm.yRot, y);
        arm.zRot = MathHelper.lerp(mix, arm.zRot, z);
    }
    private BloodPose() { }
}
