package net.kazi.kazimod.abilities.NetsuRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModParticleTypes;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.List;

public class PhoenixDiveAbility extends Ability {

    private static final ResourceLocation ICON =
            new ResourceLocation("kazimod", "textures/abilities/netsu_phoenix_dive_red.png");

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "phoenix_dive",
            new Pair[]{ImmutablePair.of("Launches the user into the air before driving them back down in a blazing area slam.", null)}
    );
    private static final float COOLDOWN = 340.0F;
    private static final float DAMAGE = 35.0F;
    private static final float HOLD_TIME = 60.0F;
    private static final int ASCENT_TICKS = 12;
    private static final double IMPACT_RADIUS = 6.0D;

    public static final AbilityCore<PhoenixDiveAbility> INSTANCE =
            new AbilityCore.Builder<>("Phoenix Dive", AbilityCategory.DEVIL_FRUITS, PhoenixDiveAbility::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            CooldownComponent.getTooltip(COOLDOWN),
                            DealDamageComponent.getTooltip(DAMAGE)
                    )
                    .setIcon(ICON)
                    .setSourceElement(SourceElement.FIRE)
                    .setSourceHakiNature(SourceHakiNature.IMBUING)
                    .setUnlockCheck(PhoenixDiveAbility::canUnlock)
                    .build();

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this)
            .addStartEvent(this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final Interval particleInterval = new Interval(2);

    private boolean landed;

    public PhoenixDiveAbility(AbilityCore<PhoenixDiveAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.dealDamageComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) {
            this.continuousComponent.startContinuity(entity, HOLD_TIME);
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.landed = false;
        this.particleInterval.restartIntervalToZero();
        entity.level.playSound(null, entity.blockPosition(), ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.05F);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }

        entity.fallDistance = 0.0F;
        float time = this.continuousComponent.getContinueTime();
        if (time <= ASCENT_TICKS) {
            Vector3d look = entity.getLookAngle().scale(0.22D);
            AbilityHelper.setDeltaMovement(entity, look.x, 1.9D, look.z);
        } else {
            Vector3d look = entity.getLookAngle().scale(0.7D);
            AbilityHelper.setDeltaMovement(entity, look.x, -3.4D, look.z);
            if (entity.isOnGround()) {
                this.resolveImpact(entity);
                this.continuousComponent.stopContinuity(entity);
                return;
            }
        }

        if (this.particleInterval.canTick()) {
            this.spawnTrailParticles(entity);
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        entity.fallDistance = 0.0F;
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private void resolveImpact(LivingEntity entity) {
        if (this.landed) {
            return;
        }

        this.landed = true;
        Vector3d center = entity.position();
        AxisAlignedBB box = new AxisAlignedBB(
                center.x - IMPACT_RADIUS, center.y - 2.0D, center.z - IMPACT_RADIUS,
                center.x + IMPACT_RADIUS, center.y + 4.0D, center.z + IMPACT_RADIUS
        );
        List<LivingEntity> targets = entity.level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                target -> target != entity && target.isAlive() && xyz.pixelatedw.mineminenomi.init.ModEntityPredicates.getEnemyFactions(entity).test(target)
        );
        for (LivingEntity target : targets) {
            if (this.dealDamageComponent.hurtTarget(entity, target, DAMAGE)) {
                target.setSecondsOnFire(8);
                Vector3d knock = target.position().subtract(center);
                if (knock.lengthSqr() < 0.001D) {
                    knock = new Vector3d(0.0D, 0.0D, 1.0D);
                }
                Vector3d push = knock.normalize().scale(0.65D);
                AbilityHelper.setDeltaMovement(target, push.x, 0.35D, push.z);
            }
        }

        entity.level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 0.82F);
        entity.level.playSound(null, entity.blockPosition(), ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 2.8F, 0.7F);
        this.spawnImpactParticles(center, entity.level);
    }

    private void spawnTrailParticles(LivingEntity entity) {
        for (int i = 0; i < 7; i++) {
            SimpleParticleData mera = new SimpleParticleData((ParticleType<?>) ModParticleTypes.MERA.get());
            mera.setLife(16);
            mera.setSize(1.7F);
            WyHelper.spawnParticles(mera, (net.minecraft.world.server.ServerWorld) entity.level,
                    entity.getX() + (WyHelper.randomDouble() - 0.5D) * 0.8D,
                    entity.getY() + WyHelper.randomDouble() * entity.getBbHeight(),
                    entity.getZ() + (WyHelper.randomDouble() - 0.5D) * 0.8D);
        }
    }

    private void spawnImpactParticles(Vector3d center, net.minecraft.world.World world) {
        for (int i = 0; i < 60; i++) {
            SimpleParticleData mera = new SimpleParticleData((ParticleType<?>) ModParticleTypes.MERA.get());
            mera.setLife(18);
            mera.setSize(2.2F);
            WyHelper.spawnParticles(mera, (net.minecraft.world.server.ServerWorld) world,
                    center.x + (WyHelper.randomDouble() - 0.5D) * 4.5D,
                    center.y + WyHelper.randomDouble() * 2.2D,
                    center.z + (WyHelper.randomDouble() - 0.5D) * 4.5D);
        }
    }

    private static boolean canUnlock(LivingEntity user) {
        IDevilFruit devilFruit = DevilFruitCapability.get(user);
        return devilFruit != null
                && devilFruit.hasDevilFruit(ModAbilities.NETSU_NETSU_NO_MI)
                && devilFruit.hasAwakenedFruit();
    }
}
