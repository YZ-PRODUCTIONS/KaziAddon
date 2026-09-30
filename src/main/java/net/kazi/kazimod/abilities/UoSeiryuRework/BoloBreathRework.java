package net.kazi.kazimod.abilities.UoSeiryuRework;

import java.awt.Color;
import java.util.List;
import net.MrMagicalCart.cartaddon.entities.projectiles.uoseiryu.BoloBreathProjectile;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.MorphHelper;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class BoloBreathRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"cartaddon", (String)"bolo_breath", new Pair[]{ImmutablePair.of((Object)"The user charges and unleashes a devastating draconic laser.", null)});
    private static final int COOLDOWN = 500;
    private static final float CHARGE_TIME = 70.0f;
    public static final AbilityCore<BoloBreathRework> INSTANCE = new AbilityCore.Builder<BoloBreathRework>("Bolo Breath", AbilityCategory.DEVIL_FRUITS, BoloBreathRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip((float)20.0f, (float)35.0f), ChargeComponent.getTooltip((float)60.0f), ContinuousComponent.getTooltip((float)80.0f), CooldownComponent.getTooltip((float)400.0f), RangeComponent.getTooltip((float)90.0f, (RangeComponent.RangeType)RangeComponent.RangeType.LINE)}).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.FIRE).build();
    private final ChargeComponent chargeComponent = new ChargeComponent((IAbility)this).addTickEvent(this::tickChargingEvent).addEndEvent(this::endChargingEvent);
    private final ContinuousComponent continuousComponent = new ContinuousComponent((IAbility)this, true).addTickEvent(this::tickBeamEvent).addEndEvent(this::endBeamEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent((IAbility)this);
    private final RangeComponent rangeComponent = new RangeComponent((IAbility)this);
    private final RequireMorphComponent requireMorphComponent = new RequireMorphComponent((IAbility)this, (MorphInfo)CartMorphs.SEIRYU_FLY.get(), new MorphInfo[]{(MorphInfo)CartMorphs.SEIRYU_HEAVY.get(), (MorphInfo)CartMorphs.SEIRYU_HEAVY_PARTIAL.get(), (MorphInfo)CartMorphs.SEIRYU_HEAVY_PARTIAL2.get(), (MorphInfo)CartMorphs.KAEN_DAIKO.get()});
    private final PoolComponent poolComponent = new PoolComponent((IAbility)this, CartAbilityPools.SEIRYU_ABILITY, new AbilityPool2[0]);
    private final ProjectileComponent projectileComponent = new ProjectileComponent((IAbility)this, this::createProjectile);
    private LightningEntity boltInner;
    private LightningEntity boltOuter;
    private float range = 90.0f;
    private float damage = 10.0f;
    private float size = 0.40f;
    private final Interval damageInterval = new Interval(10);
    private final Interval particleInterval = new Interval(4);

    public BoloBreathRework(AbilityCore<BoloBreathRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.chargeComponent, this.continuousComponent, this.dealDamageComponent, this.rangeComponent, this.requireMorphComponent, this.poolComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, 60.0f);
        }
    }

    private void tickChargingEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.getChargeTime() % 2.0f == 0.0f) {
            WyHelper.spawnParticleEffect((ParticleEffect)((ParticleEffect)CartParticleEffects.BOLO_BREATH.get()), (Entity)entity, (double)entity.getX(), (double)entity.getY(), (double)entity.getZ());
        }
    }

    private void endChargingEvent(LivingEntity entity, IAbility ability) {
        this.applyMorphScaling(entity);
        this.continuousComponent.startContinuity(entity, 80.0f);
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend((Entity)entity, (IPacket)new SAnimateHandPacket((Entity)entity, 0));
        }
    }

    private void applyMorphScaling(LivingEntity entity) {
        if (MorphHelper.getZoanInfo((LivingEntity)entity) == CartMorphs.KAEN_DAIKO.get()) {
            this.size = 0.60f;
            this.damage = 9.0f;
        } else if (MorphHelper.getZoanInfo((LivingEntity)entity) == CartMorphs.SEIRYU_FLY.get()) {
            this.size = 0.40f;
            this.damage = 8.0f;
        } else {
            this.size = 0.20f;
            this.damage = 7.0f;
        }
    }

    private void tickBeamEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.projectileComponent.shoot(entity, 4.0f, 3.0f);
            BlockRayTraceResult trace = WyHelper.rayTraceBlocks((Entity)entity, (double)0.25);
            Direction dir = Direction.fromYRot((double)entity.yRot);
            Vector3d hitVec = trace.getLocation().add((double)dir.getStepX(), (double)dir.getStepY(), (double)dir.getStepZ());
            Vector3d origin = VectorHelper.calculateRotationBasedOffsetPosition((Vector3d)entity.position(), (double)entity.yBodyRot, (double)0.5, (double)1.2, (double)0.8);
            if (this.boltOuter == null) {
                this.spawnBeam(entity, origin, hitVec);
            } else {
                this.boltInner.moveTo(origin.x, origin.y, origin.z, entity.yRot, entity.xRot);
                this.boltOuter.moveTo(origin.x, origin.y, origin.z, entity.yRot, entity.xRot);
            }
            if (this.particleInterval.canTick()) {
                WyHelper.spawnParticleEffect((ParticleEffect)((ParticleEffect)CartParticleEffects.OMORI_CHARGING.get()), (Entity)entity, (double)hitVec.x, (double)hitVec.y, (double)hitVec.z);
            }
            if (this.damageInterval.canTick()) {
                List<LivingEntity> targets = this.rangeComponent.getTargetsInLine(entity, this.range, 3.5f);
                for (LivingEntity target : targets) {
                    boolean flag = this.dealDamageComponent.hurtTarget(entity, target, this.damage);
                    if (flag) {
                        target.setSecondsOnFire(5);
                    }
                }
            }
        }
    }

    private void spawnBeam(LivingEntity entity, Vector3d origin, Vector3d hitVec) {
        int segments = (int)(this.range * 0.6f);
        this.boltInner = new LightningEntity((Entity)entity, origin.x, origin.y, origin.z, entity.yRot, entity.xRot, this.range, 20.0f, this.getCore());
        this.boltOuter = new LightningEntity((Entity)entity, origin.x, origin.y, origin.z, entity.yRot, entity.xRot, this.range, 20.0f, this.getCore());
        this.boltInner.setSize(this.size * 0.75f);
        this.boltInner.setColor(new Color(255, 220, 160));
        this.boltInner.setDamage(0.0f);
        this.boltInner.setSegments(segments);
        this.boltInner.setBranches(2);
        this.boltInner.setAngle(85);
        this.boltInner.setBoxSizeDivision((double)0.22f);
        this.boltInner.setCollideWithEntities(false);
        this.boltInner.setLightningMimic(false);
        this.boltInner.setMaxLife(120);
        this.boltOuter.setSize(this.size);
        this.boltOuter.setColor(new Color(255, 90, 20));
        this.boltOuter.setDamage(this.damage);
        this.boltOuter.setSegments(segments + 4);
        this.boltOuter.setBranches(4);
        this.boltOuter.setAngle(100);
        this.boltOuter.setBoxSizeDivision((double)0.22f);
        this.boltOuter.setExplosion(3, true, 0.3f);
        this.boltOuter.disableExplosionKnockback();
        this.boltOuter.setCollideWithEntities(false);
        this.boltOuter.setLightningMimic(false);
        this.boltOuter.setMaxLife(120);
        this.boltOuter.seed = this.boltInner.seed;
        entity.level.addFreshEntity((Entity)this.boltInner);
        entity.level.addFreshEntity((Entity)this.boltOuter);
    }

    private void endBeamEvent(LivingEntity entity, IAbility ability) {
        if (this.boltInner != null) {
            this.boltInner.remove();
            this.boltInner = null;
        }
        if (this.boltOuter != null) {
            this.boltOuter.remove();
            this.boltOuter = null;
        }
        this.cooldownComponent.startCooldown(entity, 500.0f);
    }

    private BoloBreathProjectile createProjectile(LivingEntity entity) {
        BoloBreathProjectile proj = new BoloBreathProjectile(entity.level, entity);
        return proj;
    }
}

