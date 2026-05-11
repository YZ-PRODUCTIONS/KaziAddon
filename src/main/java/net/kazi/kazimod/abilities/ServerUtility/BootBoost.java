package net.kazi.kazimod.abilities.ServerUtility;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

public class BootBoost extends Ability {

    private static final float COOLDOWN = 300.0F;
    private static final float CONTINUITY_TIME = 25.0F;
    private static final double DASH_SPEED = 1.0D;
    public static final String NO_FALL_TAG = "kazimod_boot_boost_no_fall";
    private static final int ROCKET_SOUND_INTERVAL = 4;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "boot_boost",
            new Pair[]{ImmutablePair.of("launches the user into a fast forward dash.", null)}
    );

    public static final AbilityCore<BootBoost> INSTANCE = new AbilityCore.Builder(
            "Boot Boost",
            AbilityCategory.STYLE,
            BootBoost::new
    )
            .addDescriptionLine(DESCRIPTION)
            .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                    AbilityDescriptionLine.NEW_LINE,
                    ContinuousComponent.getTooltip(CONTINUITY_TIME),
                    CooldownComponent.getTooltip(COOLDOWN)
            })
            .build();

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this)
            .addStartEvent(this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);

    public BootBoost(AbilityCore<BootBoost> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.cooldownComponent, this.continuousComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        entity.getPersistentData().putBoolean(NO_FALL_TAG, true);
        entity.fallDistance = 0.0F;
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && entity.isAlive()) {
            if (this.continuousComponent.getContinueTime() > CONTINUITY_TIME || super.canUse(entity).isFail()) {
                this.continuousComponent.stopContinuity(entity);
                return;
            }

            Vector3d dir = entity.getLookAngle().normalize().scale(DASH_SPEED);
            AbilityHelper.setDeltaMovement(entity, dir);
            entity.hurtMarked = true;
            entity.fallDistance = 0.0F;
            entity.getPersistentData().putBoolean(NO_FALL_TAG, true);
            if (entity.tickCount % ROCKET_SOUND_INTERVAL == 0) {
                entity.level.playSound(
                        null,
                        entity.blockPosition(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH,
                        SoundCategory.PLAYERS,
                        0.9F,
                        1.15F
                );
            }
            spawnBootSmoke(entity, dir.normalize());
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        entity.fallDistance = 0.0F;
        entity.getPersistentData().putBoolean(NO_FALL_TAG, true);
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private void spawnBootSmoke(LivingEntity entity, Vector3d forwardDir) {
        if (!(entity.level instanceof ServerWorld)) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) entity.level;
        Vector3d backwardDir = forwardDir.scale(-0.45D);
        Vector3d right = new Vector3d(-forwardDir.z, 0.0D, forwardDir.x).normalize().scale(0.24D);

        double footY = entity.getY() + 0.12D;
        double leftX = entity.getX() - right.x;
        double leftZ = entity.getZ() - right.z;
        double rightX = entity.getX() + right.x;
        double rightZ = entity.getZ() + right.z;

        serverWorld.sendParticles(ParticleTypes.POOF,
                leftX, footY, leftZ,
                3, 0.03D, 0.03D, 0.03D, 0.0D);
        serverWorld.sendParticles(ParticleTypes.POOF,
                rightX, footY, rightZ,
                3, 0.03D, 0.03D, 0.03D, 0.0D);

        serverWorld.sendParticles(ParticleTypes.POOF,
                leftX, footY, leftZ,
                0, backwardDir.x, 0.06D, backwardDir.z, 1.0D);
        serverWorld.sendParticles(ParticleTypes.POOF,
                rightX, footY, rightZ,
                0, backwardDir.x, 0.06D, backwardDir.z, 1.0D);
    }
}
