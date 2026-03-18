package net.kazi.kazimod.animations.gojo;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class GojoHollowPurpleAnimation extends Animation<LivingEntity, BipedModel> {

    public static GojoHollowPurpleAnimation INSTANCE;

    // Phase 1 — right arm raised straight up during chargeup
    private static final float CHARGE_RIGHT_ARM_X = (float) Math.toRadians(-160.0F);
    private static final float CHARGE_RIGHT_ARM_Y = (float) Math.toRadians(-10.0F);

    // Phase 2 — right arm thrust forward for firing
    private static final float FIRE_RIGHT_ARM_X = (float) Math.toRadians(-70.0F);
    private static final float FIRE_RIGHT_ARM_Y = (float) Math.toRadians(-10.0F);

    private static final float RAISE_END = 10.0F;
    private static final float FIRE_LERP = 5.0F;

    private boolean firing = false;
    private float fireTime = 0.0F;

    public GojoHollowPurpleAnimation(AnimationId<GojoHollowPurpleAnimation> animId) {
        super(animId);
        INSTANCE = this;
        this.setAnimationAngles(this::angles);
    }

    public void triggerFire() {
        this.firing = true;
        this.fireTime = 0.0F;
    }

    public void reset() {
        this.firing = false;
        this.fireTime = 0.0F;
    }

    public void angles(LivingEntity entity, BipedModel model, float limbSwing,
                       float limbSwingAmount, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        float time = (float) this.getTime();

        if (!firing) {
            // Phase 1 — lerp into raised pose over RAISE_END ticks then hold
            float t = MathHelper.clamp(time / RAISE_END, 0.0F, 1.0F);
            model.rightArm.xRot = MathHelper.lerp(t, 0.0F, CHARGE_RIGHT_ARM_X);
            model.rightArm.yRot = MathHelper.lerp(t, 0.0F, CHARGE_RIGHT_ARM_Y);
            model.rightArm.zRot = 0.0F;
        } else {
            // Phase 2 — lerp from raised into fire pose
            fireTime++;
            float t = MathHelper.clamp(fireTime / FIRE_LERP, 0.0F, 1.0F);
            model.rightArm.xRot = MathHelper.lerp(t, CHARGE_RIGHT_ARM_X, FIRE_RIGHT_ARM_X);
            model.rightArm.yRot = MathHelper.lerp(t, CHARGE_RIGHT_ARM_Y, FIRE_RIGHT_ARM_Y);
            model.rightArm.zRot = 0.0F;
        }
    }
}