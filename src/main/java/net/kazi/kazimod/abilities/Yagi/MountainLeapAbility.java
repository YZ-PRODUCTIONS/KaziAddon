package net.kazi.kazimod.abilities.Yagi;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

/** A grounded leap with one-jump fall protection, not a levitation effect. */
public class MountainLeapAbility extends Ability {
    public static final float COOLDOWN = 240.0F;
    private static final float MAX_AIR_TIME = 100.0F;
    private static final double FORWARD_SPEED = 0.9D;
    private static final double UPWARD_SPEED = 0.9D;

    public static final AbilityCore<MountainLeapAbility> INSTANCE = new AbilityCore.Builder<>(
            "Mountain Leap", AbilityCategory.DEVIL_FRUITS, MountainLeapAbility::new)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/leap.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.mountain_leap.description.0"))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(COOLDOWN))
            .build();

    private final ContinuousComponent airborne = new ContinuousComponent(this, true)
            .addTickEvent(this::tickLeap).addEndEvent(this::endLeap);
    private final DamageTakenComponent fallProtection = new DamageTakenComponent(this)
            .addOnAttackEvent(this::protectLanding);
    private boolean leftGround;

    public MountainLeapAbility(AbilityCore<MountainLeapAbility> core) {
        super(core);
        this.isNew = true;
        addComponents(airborne, fallProtection);
        addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        addCanUseCheck((user, ability) -> user.isOnGround() && !user.isPassenger()
                ? AbilityUseResult.success()
                : AbilityUseResult.fail(new TranslationTextComponent("ability.kazimod.mountain_leap.requires_ground")));
        addUseEvent(this::leap);
    }

    private void leap(LivingEntity user, IAbility ability) {
        if (!(user.level instanceof ServerWorld)) return;
        leftGround = false;
        user.fallDistance = 0.0F;
        airborne.startContinuity(user, MAX_AIR_TIME);
        cooldownComponent.startCooldown(user, COOLDOWN);
        float yaw = user.yRot * ((float) Math.PI / 180.0F);
        AbilityHelper.setDeltaMovement(user, -Math.sin(yaw) * FORWARD_SPEED,
                UPWARD_SPEED, Math.cos(yaw) * FORWARD_SPEED);
        ((ServerWorld) user.level).sendParticles(ParticleTypes.CLOUD,
                user.getX(), user.getY() + 0.1D, user.getZ(), 10, 0.35D, 0.08D, 0.35D, 0.04D);
        user.level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.55F, 1.35F);
    }

    private void tickLeap(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        if (!user.isAlive() || user.isPassenger() || user.isInWater() || user.isInLava()) {
            airborne.stopContinuity(user);
            return;
        }
        user.fallDistance = 0.0F;
        if (!user.isOnGround()) leftGround = true;
        if (leftGround && user.isOnGround()
                || !leftGround && airborne.getContinueTime() > 5.0F) {
            airborne.stopContinuity(user);
        }
    }

    private float protectLanding(LivingEntity user, IAbility ability, DamageSource source, float damage) {
        return airborne.isContinuous() && source == DamageSource.FALL ? 0.0F : damage;
    }

    private void endLeap(LivingEntity user, IAbility ability) {
        leftGround = false;
        // No potion, gravity, or movement flag is changed, so ending needs no effect removal.
    }
}
