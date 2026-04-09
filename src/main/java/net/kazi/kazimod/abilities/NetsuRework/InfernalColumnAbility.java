package net.kazi.kazimod.abilities.NetsuRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleTypes;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.mera.HibashiraParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.awt.Color;
import java.util.List;

public class InfernalColumnAbility extends Ability {

    private static final ResourceLocation ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/hibashira.png");

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "infernal_column",
            new Pair[]{ImmutablePair.of("Calls down a blazing infernal pillar from the sky that scorches everything caught inside.", null)}
    );
    private static final float CHARGE_TIME = 70.0F;
    private static final float COOLDOWN = 400.0F;
    private static final float DAMAGE = 40.0F;
    private static final double HIT_RADIUS = 4.0D;
    private static final Color OUTER_FIRE = new Color(255, 55, 25, 90);
    private static final Color INNER_FIRE = new Color(255, 170, 95, 65);

    public static final AbilityCore<InfernalColumnAbility> INSTANCE =
            new AbilityCore.Builder<>("Infernal Column", AbilityCategory.DEVIL_FRUITS, InfernalColumnAbility::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            ChargeComponent.getTooltip(CHARGE_TIME),
                            CooldownComponent.getTooltip(COOLDOWN),
                            DealDamageComponent.getTooltip(DAMAGE)
                    )
                    .setIcon(ICON)
                    .setSourceElement(SourceElement.FIRE)
                    .setSourceHakiNature(SourceHakiNature.IMBUING)
                    .setUnlockCheck(InfernalColumnAbility::canUnlock)
                    .build();

    private final ChargeComponent chargeComponent = new ChargeComponent(this, component -> component.getChargeTime() >= 10.0F)
            .addStartEvent(this::onChargeStart)
            .addTickEvent(this::onChargeTick)
            .addEndEvent(this::onChargeEnd);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final Interval particleInterval = new Interval(2);

    public InfernalColumnAbility(AbilityCore<InfernalColumnAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.dealDamageComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.particleInterval.restartIntervalToZero();
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide || !this.particleInterval.canTick()) {
            return;
        }

        AbilityHelper.slowEntityFall(entity);
        RayTraceResult trace = WyHelper.rayTraceBlocksAndEntities(entity, 256.0D, 0.4F);
        this.spawnAimFlames(entity, trace.getLocation(), (int) Math.max(8.0F, this.chargeComponent.getChargeTime() * 0.6F));
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }

        RayTraceResult trace = WyHelper.rayTraceBlocksAndEntities(entity, 256.0D, 0.4F);
        Vector3d tracePos = trace.getLocation();
        BlockRayTraceResult ceiling = entity.level.clip(new RayTraceContext(
                tracePos,
                tracePos.add(0.0D, 128.0D, 0.0D),
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.ANY,
                entity
        ));
        float topY = ceiling.getType() == RayTraceResult.Type.BLOCK ? (float) ceiling.getLocation().y : 128.0F;
        Vector3d center = new Vector3d(tracePos.x, topY, tracePos.z);
        float travelLength = topY + 16.0F;

        this.spawnPillar(entity, center, travelLength);
        this.spawnPillarFlames(entity, center);
        this.applyImpactDamage(entity, center);
        entity.level.playSound(null, new BlockPos(center), ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 3.2F, 0.7F);
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private void spawnPillar(LivingEntity entity, Vector3d center, float travelLength) {
        LightningEntity inner = new LightningEntity(entity, center.x, center.y, center.z, 0.0F, 90.0F, travelLength, 24.0F, this.getCore());
        LightningEntity outer = new LightningEntity(entity, center.x, center.y, center.z, 0.0F, 90.0F, travelLength, 24.0F, this.getCore());
        this.configureBolt(inner, 2.9F, 60, 24, INNER_FIRE);
        this.configureBolt(outer, 3.6F, 70, 28, OUTER_FIRE);
        outer.seed = inner.seed;
        entity.level.addFreshEntity(inner);
        entity.level.addFreshEntity(outer);
    }

    private void configureBolt(LightningEntity bolt, float size, int maxLife, int resetTime, Color color) {
        bolt.setBlocksAffectedLimit(0);
        bolt.setAngle(160);
        bolt.setBranches(1);
        bolt.setSegments(1);
        bolt.setSize(size);
        bolt.setBoxSizeDivision(0.225D);
        bolt.setLightningMovement(false);
        bolt.setExplosion(0, false, 0.0F);
        bolt.setColor(color);
        bolt.setMaxLife(maxLife);
        bolt.setDamage(0.0F);
        bolt.setTargetTimeToReset(resetTime);
    }

    private void applyImpactDamage(LivingEntity entity, Vector3d center) {
        AxisAlignedBB box = new AxisAlignedBB(
                center.x - HIT_RADIUS, center.y - 4.0D, center.z - HIT_RADIUS,
                center.x + HIT_RADIUS, center.y + 20.0D, center.z + HIT_RADIUS
        );
        List<LivingEntity> targets = entity.level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                target -> target != entity && target.isAlive() && xyz.pixelatedw.mineminenomi.init.ModEntityPredicates.getEnemyFactions(entity).test(target)
        );
        for (LivingEntity target : targets) {
            if (this.dealDamageComponent.hurtTarget(entity, target, DAMAGE)) {
                target.setSecondsOnFire(8);
            }
        }
    }

    private void spawnAimFlames(LivingEntity entity, Vector3d target, int count) {
        for (int i = 0; i < count; i++) {
            double x = target.x + (WyHelper.randomDouble() - 0.5D) * 2.6D;
            double z = target.z + (WyHelper.randomDouble() - 0.5D) * 2.6D;
            double y = target.y + WyHelper.randomDouble() * 1.8D;
            SimpleParticleData mera = new SimpleParticleData((ParticleType<?>) ModParticleTypes.MERA.get());
            mera.setLife(16);
            mera.setSize(1.65F);
            WyHelper.spawnParticles(mera, (net.minecraft.world.server.ServerWorld) entity.level, x, y, z);
        }
    }

    private void spawnPillarFlames(LivingEntity entity, Vector3d center) {
        for (int i = 0; i < 6; i++) {
            WyHelper.spawnParticleEffect((ParticleEffect) ModParticleEffects.HIBASHIRA.get(),
                    entity, center.x, center.y - 1.0D + i * 1.75D, center.z, HibashiraParticleEffect.NO_DETAILS);
        }
        for (int ring = 0; ring < 10; ring++) {
            double y = center.y - 1.0D + ring * 1.75D;
            for (int i = 0; i < 26; i++) {
                double angle = Math.PI * 2.0D * i / 26.0D;
                double radius = 2.0D + (ring % 2 == 0 ? 0.4D : 0.9D);
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                SimpleParticleData mera = new SimpleParticleData((ParticleType<?>) ModParticleTypes.MERA.get());
                mera.setLife(22);
                mera.setSize(2.35F);
                WyHelper.spawnParticles(mera, (net.minecraft.world.server.ServerWorld) entity.level, x, y, z);
            }
        }
    }

    private static boolean canUnlock(LivingEntity user) {
        IDevilFruit devilFruit = DevilFruitCapability.get(user);
        return devilFruit != null
                && devilFruit.hasDevilFruit(ModAbilities.NETSU_NETSU_NO_MI)
                && devilFruit.hasAwakenedFruit();
    }
}
