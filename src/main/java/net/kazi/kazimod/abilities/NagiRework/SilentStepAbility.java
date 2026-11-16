package net.kazi.kazimod.abilities.NagiRework;

import java.util.EnumSet;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SilentStepAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "silent_step",
                    new Pair[]{ImmutablePair.of(
                            "The user silently teleports behind a target within 20 blocks, slashing their back and inflicting bleeding and slowness.", null)});

    private static final ResourceLocation ICON = new ResourceLocation("kazimod", "textures/abilities/silent_step.png");
    private static final float DAMAGE = 15.0F;
    private static final float COOLDOWN = 160.0F;
    private static final float RANGE = 20.0F;

    public static final AbilityCore<SilentStepAbility> INSTANCE;

    public SilentStepAbility(AbilityCore<SilentStepAbility> core) {
        super(core);
        this.isNew = true;
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        RayTraceResult result = WyHelper.rayTraceBlocksAndEntities(entity, (double) RANGE);
        if (!(result instanceof EntityRayTraceResult)) return;

        Entity hitEntity = ((EntityRayTraceResult) result).getEntity();
        if (!(hitEntity instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) hitEntity;

        // Calculate position behind the target
        Vector3d targetLook = target.getLookAngle().normalize();
        double behindX = target.getX() - targetLook.x * 1.5;
        double behindY = target.getY();
        double behindZ = target.getZ() - targetLook.z * 1.5;

        // Calculate yaw to face the target's back
        double dx = target.getX() - behindX;
        double dz = target.getZ() - behindZ;
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;

        // Smoke particles at origin
        ((ServerWorld) entity.level).sendParticles(ParticleTypes.SMOKE,
                entity.getX(), entity.getY() + 1.0, entity.getZ(),
                15, 0.3, 0.5, 0.3, 0.02);

        // Teleport the player behind the target
        entity.moveTo(behindX, behindY, behindZ, yaw, entity.xRot);
        entity.moveTo(behindX, behindY, behindZ);
        if (entity instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) entity).connection.teleport(
                    behindX, behindY, behindZ,
                    yaw, entity.xRot,
                    EnumSet.noneOf(SPlayerPositionLookPacket.Flags.class)
            );
        }

        // Smoke particles at arrival
        ((ServerWorld) entity.level).sendParticles(ParticleTypes.SMOKE,
                behindX, behindY + 1.0, behindZ,
                15, 0.3, 0.5, 0.3, 0.02);

        // Deal damage
        target.hurt(ModDamageSource.causeAbilityDamage(entity, this, "slash"), DAMAGE);

        // Apply bleeding 2 + slowness 2 for 5 seconds
        target.addEffect(new EffectInstance(
                (Effect) ModEffects.BLEEDING.get(), 100, 1, false, false));
        target.addEffect(new EffectInstance(
                Effects.MOVEMENT_SLOWDOWN, 100, 1, false, false));

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.TELEPORT_SFX.get(),
                SoundCategory.PLAYERS, 1.0F, 1.2F);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Silent Step", AbilityCategory.DEVIL_FRUITS, SilentStepAbility::new))
                .setIcon(ICON)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE)
                })
                .setUnlockCheck(SilentStepAbility::canUnlock)
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .build();
    }
}
