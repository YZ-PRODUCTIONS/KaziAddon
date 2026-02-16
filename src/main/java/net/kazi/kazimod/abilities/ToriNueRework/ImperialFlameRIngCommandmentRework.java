//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.ToriNueRework;

import java.util.ArrayList;
import java.util.List;
import net.MrMagicalCart.cartaddon.entities.projectiles.lunarian.OmoriVisualProjectile;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ImperialFlameRIngCommandmentRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "imperial_flame_ring_commandment", new Pair[]{ImmutablePair.of("Transforms the user into fire and launches them forward, setting on fire all enemies around the user.", (Object)null)});
    private static final int COOLDOWN = 340;
    private static final int ON_HOLD = 40;
    private static final float RANGE = 3.5F;
    private static final int DAMAGE = 30;
    public static final AbilityCore<ImperialFlameRIngCommandmentRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::onContinuityStart).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final Interval particleInterval = new Interval(2);
    private final Interval ringInterval = new Interval(5);
    private final List<OmoriVisualProjectile> spawnedRings = new ArrayList();
    private final RequireMorphComponent requireMorphComponent;

    public ImperialFlameRIngCommandmentRework(AbilityCore<ImperialFlameRIngCommandmentRework> core) {
        super(core);
        this.requireMorphComponent = new RequireMorphComponent(this, (MorphInfo)CartMorphs.NUE_FLY.get(), new MorphInfo[]{(MorphInfo)CartMorphs.NUE_ASSAULT.get()});
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.requireMorphComponent, this.dealDamageComponent, this.continuousComponent, this.hitTrackerComponent, this.rangeComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.particleInterval.restartIntervalToZero();
        this.ringInterval.restartIntervalToZero();
        this.spawnedRings.clear();
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 2.0F, 0.5F + this.random.nextFloat() / 3.0F);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.continuousComponent.getContinueTime() > 40.0F) {
                if (entity.isOnGround()) {
                    this.continuousComponent.stopContinuity(entity);
                }
            } else {
                if (super.canUse(entity).isFail()) {
                    this.continuousComponent.stopContinuity(entity);
                }

                Vector3d dir = entity.getLookAngle().normalize().scale((double)3.0F);
                AbilityHelper.setDeltaMovement(entity, dir);
                if (this.particleInterval.canTick()) {
                    WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.NUE_FLAMES_RING.get(), entity, entity.getX(), entity.getY(), entity.getZ());
                }

                if (this.ringInterval.canTick()) {
                    OmoriVisualProjectile inner = new OmoriVisualProjectile(entity.level, entity, this, 10.0F);
                    inner.setPos(entity.getX(), entity.getY() - (double)0.75F, entity.getZ());
                    inner.setOwner(entity);
                    entity.level.addFreshEntity(inner);
                    this.spawnedRings.add(inner);
                    OmoriVisualProjectile outer = new OmoriVisualProjectile(entity.level, entity, this, 20.0F);
                    outer.setPos(entity.getX(), entity.getY() - (double)2.0F, entity.getZ());
                    outer.setOwner(entity);
                    entity.level.addFreshEntity(outer);
                    this.spawnedRings.add(outer);
                }

                for(LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 3.5F)) {
                    if (this.hitTrackerComponent.canHit(target) && this.dealDamageComponent.hurtTarget(entity, target, 30.0F)) {
                        Vector3d speed = WyHelper.propulsion(entity, (double)3.0F, (double)3.0F);
                        AbilityHelper.setDeltaMovement(entity, speed.x, (double)0.5F, speed.z);
                    }
                }
            }
        }

    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        for(OmoriVisualProjectile ring : this.spawnedRings) {
            if (ring != null && ring.isAlive()) {
                ring.remove();
            }
        }

        this.spawnedRings.clear();
        this.cooldownComponent.startCooldown(entity, 600.0F);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Imperial Flame Ring Commandment", AbilityCategory.DEVIL_FRUITS, ImperialFlameRIngCommandmentRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(30.0F), CooldownComponent.getTooltip(340.0F), ContinuousComponent.getTooltip(40.0F), RangeComponent.getTooltip(3.5F, RangeType.AOE)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, RequireMorphComponent.getTooltip()}).setSourceElement(SourceElement.FIRE).setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    }
}
