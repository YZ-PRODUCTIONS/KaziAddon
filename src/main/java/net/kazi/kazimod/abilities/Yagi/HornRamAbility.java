package net.kazi.kazimod.abilities.Yagi;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;

/** A short physical charge. It never teleports, removes blocks, or repeats a hit. */
public class HornRamAbility extends Ability {
    public static final float COOLDOWN = 320.0F;
    public static final float DAMAGE = 10.0F;
    public static final float DURATION = 10.0F;
    private static final double SPEED = 0.95D;

    public static final AbilityCore<HornRamAbility> INSTANCE = new AbilityCore.Builder<>(
            "Horn Ram", AbilityCategory.DEVIL_FRUITS, HornRamAbility::new)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/horn_dash.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.horn_ram.description.0"))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                    DealDamageComponent.getTooltip(DAMAGE), CooldownComponent.getTooltip(COOLDOWN))
            .setSourceType(SourceType.PHYSICAL)
            .setSourceHakiNature(SourceHakiNature.HARDENING)
            .build();

    private final ContinuousComponent charge = new ContinuousComponent(this, true)
            .addStartEvent(this::startCharge).addTickEvent(this::tickCharge).addEndEvent(this::endCharge);
    private final DealDamageComponent damage = new DealDamageComponent(this);
    private final Set<UUID> hitTargets = new HashSet<>();
    private Vector3d direction = Vector3d.ZERO;

    public HornRamAbility(AbilityCore<HornRamAbility> core) {
        super(core);
        this.isNew = true;
        addComponents(charge, damage);
        addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        addUseEvent((user, ability) -> {
            if (!user.level.isClientSide) charge.startContinuity(user, DURATION);
        });
    }

    private void startCharge(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        hitTargets.clear();
        // Pitch does not turn a ram into flight; yaw still works when looking straight up.
        float yaw = user.yRot * ((float) Math.PI / 180.0F);
        direction = new Vector3d(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        user.level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.65F, 0.8F);
    }

    private void tickCharge(LivingEntity user, IAbility ability) {
        if (!(user.level instanceof ServerWorld)) return;
        if (!user.isAlive() || user.isPassenger() || direction.lengthSqr() < 0.5D
                || !AbilityHelper.canUseMomentumAbilities(user)) {
            charge.stopContinuity(user);
            return;
        }

        Vector3d travel = direction.scale(SPEED);
        AxisAlignedBB swept = user.getBoundingBox().expandTowards(travel).inflate(0.35D, 0.15D, 0.35D);
        for (LivingEntity target : user.level.getEntitiesOfClass(LivingEntity.class, swept)) {
            if (target == user || !target.isAlive() || target.isSpectator()
                    || user.isAlliedTo(target) || target.isAlliedTo(user)
                    || !ModEntityPredicates.getEnemyFactions(user).test(target)
                    || target instanceof PlayerEntity && ((PlayerEntity) target).isCreative()
                    || target.position().subtract(user.position()).dot(direction) < -0.15D
                    || !user.canSee(target) || !hitTargets.add(target.getUUID())) continue;

            if (damage.hurtTarget(user, target, DAMAGE)) {
                AbilityHelper.setDeltaMovement(target, direction.x * 0.95D, 0.3D, direction.z * 0.95D);
                ((ServerWorld) user.level).sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        8, 0.25D, 0.3D, 0.25D, 0.08D);
                user.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 0.65F, 0.9F);
            }
        }

        // Sweep the user's whole body so a thin wall cannot be crossed between ticks.
        if (!user.level.noCollision(user, user.getBoundingBox().expandTowards(travel).deflate(0.001D))) {
            charge.stopContinuity(user);
            return;
        }
        AbilityHelper.setDeltaMovement(user, travel.x, user.getDeltaMovement().y, travel.z);
        if (user.tickCount % 2 == 0) {
            ((ServerWorld) user.level).sendParticles(ParticleTypes.CLOUD,
                    user.getX(), user.getY() + 0.15D, user.getZ(), 2, 0.2D, 0.1D, 0.2D, 0.01D);
        }
    }

    private void endCharge(LivingEntity user, IAbility ability) {
        hitTargets.clear();
        direction = Vector3d.ZERO;
        if (!user.level.isClientSide) {
            Vector3d motion = user.getDeltaMovement();
            AbilityHelper.setDeltaMovement(user, motion.x * 0.2D, motion.y, motion.z * 0.2D);
            cooldownComponent.startCooldown(user, COOLDOWN);
        }
    }
}
