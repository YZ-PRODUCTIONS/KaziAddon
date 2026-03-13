//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.JikiRework;

import java.awt.Color;
import net.MrMagicalCart.cartaddon.entities.projectiles.jikiextra.ExplosiveDamnedPunkProjectile;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.jiki.JikiHelper;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.MorphComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.ItemsHelper;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class DamnedPunkRework extends Ability {
    private static final int REQUIRED_IRON = 100;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "damned_punk", new Pair[]{ImmutablePair.of("Transforms the users arm into a rail-gun that shoots a projectile dealing massive damage on impact.", new Object[]{AbilityHelper.mentionText(15)})});
    private static final int COOLDOWN = 500;
    public static final AbilityCore<DamnedPunkRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::tickContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final MorphComponent morphComponent;
    private float range;
    private LightningEntity boltInner;
    private LightningEntity boltOuter;
    private Interval damageInterval;
    private final ProjectileComponent projectileComponent;
    private static float DAMAGE = 12.0F;
    private List<ItemStack> magneticItems = new ArrayList<>();

    public DamnedPunkRework(AbilityCore<DamnedPunkRework> core) {
        super(core);
        this.range = 20.0F;
        this.damageInterval = new Interval(10);
        this.projectileComponent = new ProjectileComponent(this, this::createProjectile);
        this.morphComponent = new MorphComponent(this);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.continuousComponent, this.dealDamageComponent, this.rangeComponent, this.chargeComponent, this.morphComponent});
        this.addCanUseCheck(JikiHelper.getMetalicItemsCheck(REQUIRED_IRON));
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, 60.0F);
        } else if (this.chargeComponent.getChargePercentage() > 0.5F) {
            this.chargeComponent.stopCharging(entity);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.range = 20.0F;
        this.morphComponent.startMorph(entity, (MorphInfo)CartMorphs.DAMNED_PUNK.get());
        if (!this.magneticItems.isEmpty()) {
            this.magneticItems.clear();
        }
        List<ItemStack> inventory = ItemsHelper.getAllInventoryItems(entity);
        this.magneticItems = JikiHelper.getMagneticItemsNeeded(inventory, (float)REQUIRED_IRON);
        JikiHelper.spawnAttractEffect(entity);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.CHARGE_CYBORG_BEAM_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.getChargeTime() == 39.0F) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.PRE_CYBORG_BEAM_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        }

        if (this.chargeComponent.getChargeTime() % 10.0F == 0.0F) {
            this.range += 7.0F;
        }

    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, 60.0F);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.projectileComponent.shoot(entity, 4.0F, 3.0F);
            BlockRayTraceResult trace = WyHelper.rayTraceBlocks(entity, (double)0.25F);
            Direction dir = Direction.fromYRot((double)entity.yRot);
            Vector3d hitVec = trace.getLocation().add((double)dir.getStepX(), (double)dir.getStepY(), (double)dir.getStepZ());
            Vector3d origin = VectorHelper.calculateRotationBasedOffsetPosition(entity.position(), (double)entity.yBodyRot, (double)0.5F, 1.2, 0.8);
            if (this.boltOuter == null) {
                this.spawnBeam(entity, origin, hitVec);
            } else {
                this.boltInner.moveTo(origin.x, origin.y, origin.z, entity.yRot, entity.xRot);
                this.boltOuter.moveTo(origin.x, origin.y, origin.z, entity.yRot, entity.xRot);
            }

            if (this.damageInterval.canTick()) {
                for(LivingEntity target : this.rangeComponent.getTargetsInLine(entity, this.range, 3.5F)) {
                    boolean flag = this.dealDamageComponent.hurtTarget(entity, target, DAMAGE);
                    if (flag) {
                        target.setSecondsOnFire(5);
                    }
                }
            }
        }

    }

    private void spawnBeam(LivingEntity entity, Vector3d origin, Vector3d hitVec) {
        int segments = (int)(this.range * 0.6F);
        this.boltInner = new LightningEntity(entity, origin.x, origin.y, origin.z, entity.yRot, entity.xRot, this.range, 20.0F, this.getCore());
        this.boltOuter = new LightningEntity(entity, origin.x, origin.y, origin.z, entity.yRot, entity.xRot, this.range, 20.0F, this.getCore());
        this.boltInner.setSize(0.2625F);
        this.boltOuter.setColor(new Color(15652091));
        this.boltInner.setDamage(0.0F);
        this.boltInner.setSegments(segments);
        this.boltInner.setBranches(2);
        this.boltInner.setAngle(85);
        this.boltInner.setBoxSizeDivision((double)0.22F);
        this.boltInner.setCollideWithEntities(false);
        this.boltInner.setLightningMimic(false);
        this.boltInner.setMaxLife(120);
        this.boltOuter.setSize(0.35F);
        this.boltOuter.setColor(new Color(11693284));
        this.boltOuter.setDamage(DAMAGE / 4.0F);
        this.boltOuter.setSegments(segments + 4);
        this.boltOuter.setBranches(4);
        this.boltOuter.setAngle(100);
        this.boltOuter.setBoxSizeDivision((double)0.22F);
        this.boltOuter.setExplosion(3, true, 0.3F);
        this.boltOuter.disableExplosionKnockback();
        this.boltOuter.setCollideWithEntities(false);
        this.boltOuter.setLightningMimic(false);
        this.boltOuter.setMaxLife(120);
        this.boltOuter.seed = this.boltInner.seed;
        entity.level.addFreshEntity(this.boltInner);
        entity.level.addFreshEntity(this.boltOuter);
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.boltInner != null) {
            this.boltInner.remove();
            this.boltInner = null;
        }

        if (this.boltOuter != null) {
            this.boltOuter.remove();
            this.boltOuter = null;
        }

        this.morphComponent.stopMorph(entity);
        if (this.magneticItems.size() > 0) {
            ItemStack stack = (ItemStack)this.magneticItems.get(0);
            ItemsHelper.itemBreakParticles(entity, 100, stack);
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), SoundEvents.ITEM_BREAK, entity.getSoundSource(), 0.5F, 1.0F);
            JikiHelper.dropComponentItems(entity, entity.position(), this.magneticItems);
        }
        this.cooldownComponent.startCooldown(entity, 500.0F);
    }

    private ExplosiveDamnedPunkProjectile createProjectile(LivingEntity entity) {
        ExplosiveDamnedPunkProjectile proj = new ExplosiveDamnedPunkProjectile(entity.level, entity);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Damned Punk", AbilityCategory.DEVIL_FRUITS, DamnedPunkRework::new)).addDescriptionLine(DESCRIPTION).addDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(DAMAGE), ChargeComponent.getTooltip(60.0F), ContinuousComponent.getTooltip(60.0F), CooldownComponent.getTooltip(500.0F), RangeComponent.getTooltip(20.0F, 62.0F, RangeType.LINE)}).setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    }
}