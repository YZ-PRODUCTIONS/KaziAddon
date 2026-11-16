package net.kazi.kazimod.alchemy;

import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;

/** Model-space poses; the hips remain the common anchor for every body part. */
final class AlchemyPose {
    // time, torso pitch/yaw/roll, right arm pitch/yaw/roll, left arm pitch/yaw/roll, stance
    private static final float[][] COMBO = {
        {-4, .24F, -.50F, -.05F, -1.12F, -.80F, -.28F, -.72F, .32F, .28F, .30F},
        { 0, .42F,  .55F,  .06F, -1.48F,  .85F,  .24F, -.90F, .22F, .20F, .30F},
        { 3, .30F,  .38F,  .03F, -1.10F,  .65F,  .15F, -1.00F, .52F, .35F, .30F},
        { 7, .25F,  .40F,  .03F,  -.88F,  .42F, -.18F,  -.70F, .64F, .30F, .30F},
        {10, .49F, -.42F, -.05F,  -.70F, -.30F, -.22F, -1.65F, .03F, .04F, .30F},
        {13, .28F, -.23F, -.02F, -1.12F, -.35F, -.40F, -1.20F, .20F, .20F, .30F},
        {17, .24F,  .48F,  .07F, -1.80F,  .88F,  .35F,  -.90F, .20F, .22F, .30F},
        {20, .43F, -.56F, -.10F, -1.30F, -.95F, -.25F,  -.65F, .45F, .30F, .30F},
        {23, .29F, -.30F, -.04F, -1.82F, -.30F, -.20F, -1.10F, .15F, .15F, .30F},
        {26, .10F, -.16F,  .00F, -2.85F, -.12F, -.12F, -2.30F, .15F, .12F, .30F},
        {30, .92F,  .12F,  .03F,  -.72F,  .05F, -.12F,  -.82F, .05F, .12F, .30F},
        {32, .72F,  .08F,  .02F,  -.48F,  .05F, -.16F,  -.62F, .10F, .16F, .30F},
        {36, .08F,  .00F,  .00F,  -.28F, -.06F, -.08F,  -.28F, .06F, .08F, .30F}
    };

    static boolean apply(PlayerModel<?> m, LivingEntity player, float animationTime) {
        int action = AlchemyFeatures.action(player);
        if (action < 0 || action > 5 || !player.isAlive() || player.isPassenger()
                || player.isFallFlying() || player.isVisuallySwimming() || AlchemyClient.renderingHand()) return false;
        CompoundNBT state = AlchemyFeatures.state(player);
        float partial = animationTime - (float)Math.floor(animationTime);
        float age = player.level.getGameTime() - state.getLong("Start") + partial;
        float remaining = state.getLong("End") - player.level.getGameTime() - partial;
        if (age < 0 || remaining <= 0) return false;
        float weight = (action == 3 ? 1 : smooth(age / 2)) * smooth(remaining / (action == 5 ? 4 : 5));
        if (action == 3) age += 14;
        else if (action == 5) age = age < AlchemyAbility.GRAND_WINDUP
                ? age * 12 / AlchemyAbility.GRAND_WINDUP : age + 12 - AlchemyAbility.GRAND_WINDUP;
        Frame frame = new Frame();

        if (action == 4) {
            long contact = state.getLong("ComboContact");
            if (contact > 0) {
                float hitAge = player.level.getGameTime() - contact + partial;
                if (hitAge >= 36) return false;
                sample(frame, hitAge);
                weight *= 1 - smooth((hitAge - 31) / 5);
            } else {
                frame.bodyX = .25F; frame.bodyY = -.42F;
                frame.rightX = -1.15F; frame.rightY = -.65F; frame.rightZ = -.22F;
                frame.leftX = -.92F; frame.leftY = .32F; frame.leftZ = .23F;
                frame.stance = .30F;
            }
        } else if (action == 2 && state.contains("WallLaunch", 4)
                && state.getLong("WallLaunch") == state.getLong("Start")) {
            if (age >= 12) return false;
            frame.bodyX = .16F; frame.bodyY = -.46F;
            frame.rightX = -.62F; frame.rightY = -.66F; frame.rightZ = -.18F;
            frame.leftX = -1.02F; frame.leftY = .35F; frame.leftZ = .23F;
            frame.stance = .25F;
            Frame punch = new Frame();
            punch.bodyX = .38F; punch.bodyY = .27F;
            punch.rightX = -1.58F; punch.rightY = -.02F; punch.rightZ = -.06F;
            punch.leftX = -.70F; punch.leftY = .34F; punch.leftZ = .20F;
            punch.stance = .25F;
            frame.mix(punch, smooth((age - 1) / 3));
            Frame recover = new Frame();
            recover.rightX = -.30F; recover.rightY = -.10F; recover.rightZ = -.06F;
            recover.leftX = -.35F; recover.leftY = .12F; recover.leftZ = .06F;
            recover.stance = .25F;
            frame.mix(recover, smooth((age - 5) / 7));
        } else {
            clap(frame, age);
            float open = smooth((age - 8) / 4);
            if (action == 1 || action == 5) {
                Frame ground = new Frame();
                ground.bodyX = 1.62F;
                ground.rightX = -.04F; ground.leftX = -.04F;
                ground.rightY = -.08F; ground.leftY = .08F;
                ground.rightZ = -.12F; ground.leftZ = .12F;
                ground.stance = .48F;
                frame.mix(ground, open);
                if (action == 1) weight *= 1 - smooth((age - 27) / 5);
            } else if (action == 2) {
                Frame wall = new Frame();
                wall.bodyX = .32F; wall.rightX = -1.35F; wall.leftX = -1.35F;
                wall.rightY = -.14F; wall.leftY = .14F;
                wall.rightZ = -.12F; wall.leftZ = .12F; wall.stance = .22F;
                frame.mix(wall, open);
                weight *= 1 - smooth((age - 15) / 5);
                if (age >= 20) return false;
            } else if (action == 3) {
                Frame reach = new Frame();
                reach.bodyX = .22F; reach.bodyY = -.17F;
                reach.rightX = -1.48F; reach.rightY = -.05F; reach.rightZ = -.09F;
                reach.leftX = -.92F; reach.leftY = .48F; reach.leftZ = .28F;
                reach.stance = .20F;
                frame.mix(reach, smooth((age - 8) / 6));
                weight *= 1 - smooth((age - 25) / 11);
            } else {
                Frame forge = new Frame();
                forge.bodyY = -.15F; forge.rightX = -1.12F; forge.rightY = -.28F;
                forge.rightZ = -.12F; forge.leftX = -1.14F; forge.leftY = .72F;
                forge.leftZ = .35F; forge.stance = .12F;
                frame.mix(forge, open);
            }
        }
        if (weight <= .001F) return false;

        m.body.xRot = lerp(weight, m.body.xRot, frame.bodyX);
        m.body.yRot = lerp(weight, m.body.yRot, frame.bodyY);
        m.body.zRot = lerp(weight, m.body.zRot, frame.bodyZ);
        arm(m.rightArm, weight, frame.rightX, frame.rightY, frame.rightZ);
        arm(m.leftArm, weight, frame.leftX, frame.leftY, frame.leftZ);
        arm(m.rightLeg, weight, -frame.stance, 0, .035F);
        arm(m.leftLeg, weight, frame.stance, 0, -.035F);

        // Equal/opposite leg angles keep both soles at y=24 while lowering the hips.
        float hipY = 24 - 12 * MathHelper.cos(frame.stance) * MathHelper.cos(.035F);
        m.rightLeg.y = lerp(weight, m.rightLeg.y, hipY);
        m.leftLeg.y = lerp(weight, m.leftLeg.y, hipY);
        float hipX = (m.rightLeg.x + m.leftLeg.x) * .5F;
        float hipZ = (m.rightLeg.z + m.leftLeg.z) * .5F;
        float[] spine = rotate(m.body, 0, 12, 0);
        m.body.x = hipX - spine[0];
        m.body.y = (m.rightLeg.y + m.leftLeg.y) * .5F - spine[1];
        m.body.z = hipZ - spine[2];
        m.head.x = m.body.x; m.head.y = m.body.y; m.head.z = m.body.z;
        m.head.xRot -= frame.bodyX * .38F * weight;
        m.head.zRot += frame.bodyZ * .5F * weight;
        shoulder(m, m.rightArm, -5, AlchemyClient.slim(player) ? 2.5F : 2);
        shoulder(m, m.leftArm, 5, AlchemyClient.slim(player) ? 2.5F : 2);
        copyWear(m);
        return true;
    }

    private static void clap(Frame f, float age) {
        float close = smooth(age / 5);
        f.bodyX = .08F; f.stance = .12F;
        f.rightX = lerp(close, -.72F, -1.35F); f.leftX = f.rightX;
        f.rightY = lerp(close, .40F, -.44F); f.leftY = -f.rightY;
        f.rightZ = lerp(close, -.42F, -.04F); f.leftZ = -f.rightZ;
    }

    private static void sample(Frame f, float time) {
        int i = 0;
        while (i < COMBO.length - 2 && time > COMBO[i + 1][0]) i++;
        float[] a = COMBO[i], b = COMBO[i + 1];
        float t = smooth((time - a[0]) / (b[0] - a[0]));
        f.bodyX = lerp(t, a[1], b[1]); f.bodyY = lerp(t, a[2], b[2]); f.bodyZ = lerp(t, a[3], b[3]);
        f.rightX = lerp(t, a[4], b[4]); f.rightY = lerp(t, a[5], b[5]); f.rightZ = lerp(t, a[6], b[6]);
        f.leftX = lerp(t, a[7], b[7]); f.leftY = lerp(t, a[8], b[8]); f.leftZ = lerp(t, a[9], b[9]);
        f.stance = lerp(t, a[10], b[10]);
    }

    private static void shoulder(PlayerModel<?> m, ModelRenderer arm, float x, float y) {
        float[] offset = rotate(m.body, x, y, 0);
        arm.x = m.body.x + offset[0]; arm.y = m.body.y + offset[1]; arm.z = m.body.z + offset[2];
    }

    // ModelRenderer applies Z/Y/X rotation; use the identical order for attachment pivots.
    private static float[] rotate(ModelRenderer m, float x, float y, float z) {
        float cy = MathHelper.cos(m.yRot), sy = MathHelper.sin(m.yRot);
        float cx = MathHelper.cos(m.xRot), sx = MathHelper.sin(m.xRot);
        float cz = MathHelper.cos(m.zRot), sz = MathHelper.sin(m.zRot);
        float y1 = y * cx - z * sx, z1 = y * sx + z * cx;
        float x2 = x * cy + z1 * sy, z2 = -x * sy + z1 * cy;
        return new float[]{x2 * cz - y1 * sz, x2 * sz + y1 * cz, z2};
    }

    private static void arm(ModelRenderer arm, float weight, float x, float y, float z) {
        arm.xRot = lerp(weight, arm.xRot, x); arm.yRot = lerp(weight, arm.yRot, y);
        arm.zRot = lerp(weight, arm.zRot, z);
    }

    static void copyWear(PlayerModel<?> m) {
        m.hat.copyFrom(m.head); m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm); m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg); m.leftPants.copyFrom(m.leftLeg);
    }

    static float smooth(float t) { t = MathHelper.clamp(t, 0, 1); return t * t * (3 - 2 * t); }
    private static float lerp(float t, float a, float b) { return MathHelper.lerp(t, a, b); }

    private static final class Frame {
        float bodyX, bodyY, bodyZ, rightX, rightY, rightZ, leftX, leftY, leftZ, stance;
        void mix(Frame b, float t) {
            bodyX = lerp(t, bodyX, b.bodyX); bodyY = lerp(t, bodyY, b.bodyY); bodyZ = lerp(t, bodyZ, b.bodyZ);
            rightX = lerp(t, rightX, b.rightX); rightY = lerp(t, rightY, b.rightY); rightZ = lerp(t, rightZ, b.rightZ);
            leftX = lerp(t, leftX, b.leftX); leftY = lerp(t, leftY, b.leftY); leftZ = lerp(t, leftZ, b.leftZ);
            stance = lerp(t, stance, b.stance);
        }
    }

    private AlchemyPose() {}
}
