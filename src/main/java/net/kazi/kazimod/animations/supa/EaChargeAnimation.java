package net.kazi.kazimod.animations.supa;

import net.kazi.kazimod.abilities.GoruRework.EaAbility;
import net.kazi.kazimod.entities.EaVfxEntity;
import net.kazi.kazimod.entities.WindsRaptureShape;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

/** Raise Ea over the caster, then bring the sword down into the aimed release. */
public final class EaChargeAnimation extends Animation<LivingEntity, EntityModel> {
    public static final float RAISED_RIGHT_X = -2.65F;
    public static final float RAISED_RIGHT_Y = 0.10F;
    public static final float RAISED_RIGHT_Z = 0.35F;
    public static final float RAISE_TICKS = 8.0F;
    public static final float AIM_TICKS = 20.0F;

    public EaChargeAnimation(AnimationId<EaChargeAnimation> id) {
        super(id);
        setAnimationAngles(this::angles);
    }

    private void angles(LivingEntity entity, EntityModel entityModel, float limbSwing,
                        float limbAmount, float age, float headYaw, float headPitch) {
        // Animation callbacks also run for non-humanoid morph models. Those have
        // no matching arms, so leave their pose alone without a per-frame cast error.
        if (!(entityModel instanceof BipedModel)) return;
        BipedModel model = (BipedModel) entityModel;
        float progress = (float) getTime() / EaVfxEntity.CHARGE_TICKS;
        IAbilityData data = AbilityDataCapability.get(entity);
        EaAbility ability = data == null ? null : data.getEquippedAbility(EaAbility.INSTANCE);
        if (ability != null && ability.isCharging()) {
            progress = ability.getComponent(ModAbilityKeys.CHARGE)
                    .map(charge -> charge.getChargePercentage()).orElse(progress);
        }

        if (ability != null && ability.isWindsMode()) {
            float raise = smooth(progress * WindsRaptureShape.CHARGE_TICKS / RAISE_TICKS);
            boolean leftHanded = entity.getMainArm() == HandSide.LEFT;
            float side = leftHanded ? -1 : 1;
            // Hold the casting arm low and outward to match the off-center sword.
            arm(leftHanded ? model.leftArm : model.rightArm,
                    -.65F + (float)Math.toRadians(headPitch) * .65F,
                    (float)Math.toRadians(headYaw), side * .90F, raise);
            arm(leftHanded ? model.rightArm : model.leftArm, .12F, side * -.08F, side * -.22F, raise);
            return;
        }

        float raise = raiseBlend(progress);
        float aim = aimBlend(progress);
        float yaw = (float) Math.toRadians(headYaw);
        float pitch = (float) Math.toRadians(headPitch);
        arm(model.rightArm,
                MathHelper.lerp(aim, RAISED_RIGHT_X, -(float) Math.PI / 2 + pitch),
                MathHelper.lerp(aim, RAISED_RIGHT_Y, yaw),
                RAISED_RIGHT_Z * (1 - aim), raise);
        // The free arm stays low and open instead of duplicating a two-hand cast.
        arm(model.leftArm, 0.10F - aim * 0.18F, -aim * 0.08F,
                -0.20F - aim * 0.06F, raise);
    }

    public static float raiseBlend(float progress) {
        return smooth(progress * EaVfxEntity.CHARGE_TICKS / RAISE_TICKS);
    }

    public static float aimBlend(float progress) {
        return smooth((progress * EaVfxEntity.CHARGE_TICKS
                - (EaVfxEntity.CHARGE_TICKS - AIM_TICKS)) / AIM_TICKS);
    }

    private static void arm(ModelRenderer arm, float x, float y, float z, float blend) {
        arm.xRot = MathHelper.lerp(blend, arm.xRot, x);
        arm.yRot = MathHelper.lerp(blend, arm.yRot, y);
        arm.zRot = MathHelper.lerp(blend, arm.zRot, z);
    }

    private static float smooth(float value) {
        float t = MathHelper.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
