package net.kazi.kazimod.animations.gojo;

import net.kazi.kazimod.abilities.Koku.HollowPurpleAbility;
import net.kazi.kazimod.abilities.boss.gojo.BossHollowPurpleAbility;
import net.kazi.kazimod.animations.gojo.GojoCastPose;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

public class GojoHollowPurpleAnimation
extends Animation<LivingEntity, EntityModel> {
    // Compatibility for existing boss visual callbacks; progress is now per ability.
    public static GojoHollowPurpleAnimation INSTANCE;
    public void reset() { }
    public void triggerFire() { }

    public GojoHollowPurpleAnimation(AnimationId<GojoHollowPurpleAnimation> id) {
        super(id);
        INSTANCE = this;
        this.setAnimationAngles(this::angles);
    }

    private void angles(LivingEntity entity, EntityModel model, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float progress = (float)this.getTime() / 140.0f;
        IAbilityData data = AbilityDataCapability.get((LivingEntity)entity);
        HollowPurpleAbility playerAbility = data == null ? null : data.getEquippedAbility(HollowPurpleAbility.INSTANCE);
        BossHollowPurpleAbility bossAbility = data == null ? null : data.getEquippedAbility(BossHollowPurpleAbility.INSTANCE);
        if (playerAbility != null && playerAbility.isCharging()) {
            progress = playerAbility.getComponent(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.CHARGE).map(charge -> charge.getChargePercentage()).orElse(progress);
        } else if (bossAbility != null && bossAbility.isCharging()) {
            progress = bossAbility.getComponent(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.CHARGE).map(charge -> charge.getChargePercentage()).orElse(progress);
        }
        float raise = GojoCastPose.smooth(progress / 0.1f);
        float gesture = GojoCastPose.smooth((progress - 0.3f) / 0.22f);
        float release = GojoCastPose.smooth((progress - 0.8f) / 0.17f);
        float yaw = (float)Math.toRadians(netHeadYaw);
        float pitch = (float)Math.toRadians(headPitch);
        GojoCastPose.arm(model, true, -1.35f - gesture * 0.5f + release * (0.28f + pitch), 0.72f * (1.0f - release) + yaw * release, 0.12f * (1.0f - release), raise);
        GojoCastPose.arm(model, false, -1.35f - gesture * 0.3f - release * 0.3f, -0.72f + release * 0.25f, -0.12f - release * 0.12f, raise);
    }
}
