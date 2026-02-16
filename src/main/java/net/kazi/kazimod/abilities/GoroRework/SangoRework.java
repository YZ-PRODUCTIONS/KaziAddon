//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.GoroRework;

import java.awt.Color;
import javax.annotation.Nullable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.goro.ElThorAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
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

public class SangoRework extends Ability {
    private static final ResourceLocation DEFAULT_ICON = new ResourceLocation("mineminenomi", "textures/abilities/sango.png");
    private static final ResourceLocation ALT_ICON = new ResourceLocation("mineminenomi", "textures/abilities/alts/sango.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "sango", new Pair[]{ImmutablePair.of("Launches powerful charges of electricity from the hands.", (Object)null)});
    private static final int COOLDOWN = 240;
    private static final int CHARGE_TIME = 40;
    public static final AbilityCore<SangoRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::onChargeStart).addTickEvent(this::onChargeTick).addEndEvent(this::onChargeEnd);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final Interval particleInterval = new Interval(10);

    public SangoRework(AbilityCore<SangoRework> core) {
        super(core);
        this.setDisplayIcon(DEFAULT_ICON);
        if (ClientConfig.INSTANCE.isGoroBlue()) {
            this.setDisplayIcon(ALT_ICON);
        }

        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.chargeComponent, this.animationComponent});
        super.addUseEvent(this::onUseEvent);
        this.addEquipEvent(this::equipEvent);
    }

    public void equipEvent(LivingEntity entity, Ability ability) {
        this.setDisplayIcon(DEFAULT_ICON);
        if (ClientConfig.INSTANCE.isGoroBlue()) {
            this.setDisplayIcon(ALT_ICON);
        }

    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.startCharging(entity, 40.0F);
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.particleInterval.restartIntervalToZero();
            this.animationComponent.start(entity, ModAnimations.POINT_RIGHT_ARM);
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.particleInterval.canTick()) {
                EntityRayTraceResult trace = WyHelper.rayTraceEntities(entity, 0.8);
                WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.SANGO.get(), entity, trace.getLocation().x(), entity.getY() + (double)1.0F, trace.getLocation().z());
            }

        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            RayTraceResult mop = WyHelper.rayTraceBlocks(entity, (double)32.0F);
            double beamDistance = Math.sqrt(entity.distanceToSqr(mop.getLocation().x, mop.getLocation().y, mop.getLocation().z));
            float multi = 1.0F;
            if (((MorphInfo)ModMorphs.VOLT_AMARU.get()).isActive(entity)) {
                multi += 0.25F;
            }

            float damage = 20.0F * multi;
            float size = 0.28F * multi;
            float length = 50.0F * multi;
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
            Vector3d pos = VectorHelper.calculateRotationBasedOffsetPosition(entity.position(), (double)entity.yBodyRot, (double)0.5F, 1.15, 0.8);
            LightningEntity boltInner = new LightningEntity(entity, pos.x, pos.y, pos.z, entity.yRot, entity.xRot, length + (float)beamDistance, 20.0F, this.getCore());
            LightningEntity boltOuter = new LightningEntity(entity, pos.x, pos.y, pos.z, entity.yRot, entity.xRot, length + (float)beamDistance, 20.0F, this.getCore());
            this.setBoltPropieties(boltInner, size * 0.9F, beamDistance, 0.0F, 25, false, Color.WHITE, multi);
            this.setBoltPropieties(boltOuter, size, beamDistance, damage, 40, true, ClientConfig.INSTANCE.isGoroBlue() ? ElThorAbility.BLUE_THUNDER : ElThorAbility.YELLOW_THUNDER, multi);
            boltOuter.seed = boltInner.seed;
            entity.level.addFreshEntity(boltInner);
            entity.level.addFreshEntity(boltOuter);
            this.animationComponent.stop(entity);
            super.cooldownComponent.startCooldown(entity, 240.0F);
        }
    }

    public void setBoltPropieties(LightningEntity bolt, float size, double distance, float damage, int timeAlive, boolean explodes, @Nullable Color color, float multiplier) {
        int segments = (int)(distance * (double)0.5F);
        bolt.setBlocksAffectedLimit(1508);
        bolt.setMaxLife(40);
        bolt.setDamage(damage * multiplier);
        if (explodes) {
            bolt.setExplosion((int)(3.0F * multiplier), true, 0.3F);
        } else {
            bolt.setExplosion(0, false);
        }

        bolt.setSize(size * multiplier);
        bolt.setBoxSizeDivision((double)0.225F);
        bolt.setColor(color);
        bolt.setAngle(100);
        bolt.setTargetTimeToReset(60);
        bolt.disableExplosionKnockback();
        bolt.setBranches((int)((double)5.0F + distance / (double)100.0F));
        bolt.setSegments((int)((double)segments + WyHelper.randomWithRange(-segments / 4, segments / 4)));
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Sango", AbilityCategory.DEVIL_FRUITS, SangoRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(240.0F), ChargeComponent.getTooltip(40.0F)}).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.LIGHTNING).setIcon(DEFAULT_ICON).build();
    }
}
