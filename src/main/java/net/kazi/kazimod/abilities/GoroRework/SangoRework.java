package net.kazi.kazimod.abilities.GoroRework;

import java.awt.Color;
import javax.annotation.Nullable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Kazi-owned copy of Mine Mine no Mi 0.10.11's Sango. */
public class SangoRework extends Ability {
    private static final ResourceLocation DEFAULT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/sango.png");
    private static final ResourceLocation ALT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/alts/sango.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "sango",
            new Pair[]{ImmutablePair.of(
                    "Launches powerful charges of electricity from the hands.", null)});

    public static final AbilityCore<SangoRework> INSTANCE =
            new AbilityCore.Builder<SangoRework>(
                    "Sango", AbilityCategory.DEVIL_FRUITS, SangoRework::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            CooldownComponent.getTooltip(240.0F),
                            ChargeComponent.getTooltip(40.0F))
                    .setSourceHakiNature(SourceHakiNature.SPECIAL)
                    .setSourceElement(SourceElement.LIGHTNING)
                    .setIcon(DEFAULT_ICON)
                    .build();

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final Interval particleInterval = new Interval(10);

    public SangoRework(AbilityCore<SangoRework> core) {
        super(core);
        updateDisplayIcon();
        this.isNew = true;
        this.addComponents(chargeComponent, animationComponent);
        this.addUseEvent(this::onUseEvent);
        this.addEquipEvent((entity, ability) -> updateDisplayIcon());
    }

    private void updateDisplayIcon() {
        this.setDisplayIcon(ClientConfig.INSTANCE.isGoroBlue() ? ALT_ICON : DEFAULT_ICON);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        chargeComponent.startCharging(entity, 40.0F);
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        particleInterval.restartIntervalToZero();
        animationComponent.start(entity, ModAnimations.POINT_RIGHT_ARM);
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide || !particleInterval.canTick()) return;
        EntityRayTraceResult trace = WyHelper.rayTraceEntities(entity, 0.8D);
        WyHelper.spawnParticleEffect(
                (ParticleEffect) ModParticleEffects.SANGO.get(), entity,
                trace.getLocation().x(), entity.getY() + 1.0D, trace.getLocation().z());
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        BlockRayTraceResult mop = WyHelper.rayTraceBlocks(entity, 32.0D);
        double beamDistance = Math.sqrt(entity.distanceToSqr(
                mop.getLocation().x(), mop.getLocation().y(), mop.getLocation().z()));
        float multiplier = ((MorphInfo) ModMorphs.VOLT_AMARU.get()).isActive(entity)
                ? 1.25F : 1.0F;
        float damage = 20.0F * multiplier;
        float size = 0.28F * multiplier;
        float length = 50.0F * multiplier;

        ((ServerWorld) entity.level).getChunkSource()
                .broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        Vector3d pos = VectorHelper.calculateRotationBasedOffsetPosition(
                entity.position(), entity.yBodyRot, 0.5D, 1.15D, 0.8D);
        LightningEntity boltInner = new LightningEntity(
                entity, pos.x, pos.y, pos.z, entity.yRot, entity.xRot,
                length + (float) beamDistance, 20.0F, getCore());
        LightningEntity boltOuter = new LightningEntity(
                entity, pos.x, pos.y, pos.z, entity.yRot, entity.xRot,
                length + (float) beamDistance, 20.0F, getCore());
        setBoltProperties(
                boltInner, size * 0.9F, beamDistance, 0.0F,
                25, false, Color.WHITE, multiplier);
        setBoltProperties(
                boltOuter, size, beamDistance, damage,
                40, true,
                ClientConfig.INSTANCE.isGoroBlue()
                        ? ElThorRework.BLUE_THUNDER : ElThorRework.YELLOW_THUNDER,
                multiplier);
        boltOuter.seed = boltInner.seed;
        entity.level.addFreshEntity(boltInner);
        entity.level.addFreshEntity(boltOuter);
        animationComponent.stop(entity);
        cooldownComponent.startCooldown(entity, 240.0F);
    }

    private void setBoltProperties(
            LightningEntity bolt, float size, double distance, float damage,
            int timeAlive, boolean explodes, @Nullable Color color, float multiplier) {
        int segments = (int) (distance * 0.5D);
        bolt.setBlocksAffectedLimit(1508);
        bolt.setMaxLife(40);
        bolt.setDamage(damage * multiplier);
        if (explodes) {
            bolt.setExplosion((int) (3.0F * multiplier), true, 0.3F);
        } else {
            bolt.setExplosion(0, false);
        }
        bolt.setSize(size * multiplier);
        bolt.setBoxSizeDivision(0.225D);
        bolt.setColor(color);
        bolt.setAngle(100);
        bolt.setTargetTimeToReset(60);
        bolt.disableExplosionKnockback();
        bolt.setBranches((int) (5.0D + distance / 100.0D));
        bolt.setSegments((int) (segments
                + WyHelper.randomWithRange(-segments / 4, segments / 4)));
    }
}
