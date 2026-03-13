//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.SpearRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.entities.projectiles.trident.AbsolutePierceProjectile;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class AbsolutePierceRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "absolute_pierce", new Pair[]{ImmutablePair.of("The user inflates his forearm until they release it. On release, the pressure explodes and fires a flamming fist that deals great knockback. (When charging this move, the damage and range of the move increases)", (Object)null)});
    private static final int HOLD_TIME = 10;
    private static final int COOLDOWN = 600;
    private static final float RANGE = 1.4F;
    private static final float DAMAGE = 75.0F;
    public static final AbilityCore<AbsolutePierceRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public AbsolutePierceRework(AbilityCore<AbsolutePierceRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.rangeComponent, this.dealDamageComponent, this.animationComponent, this.hitTrackerComponent, this.projectileComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityLimits::requiresSpear);
        this.addCanUseCheck(AbilityLimits::fruitless);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 10.0F);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.animationComponent.start(entity, ModAnimations.CHARGE_PUNCH);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive()) {
            Vector3d look = entity.getLookAngle();
            Vector3d speed = look.multiply(1.4, (double)0.0F, 1.4);
            entity.move(MoverType.SELF, speed);

            for(LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 2.0F)) {
                if (this.hitTrackerComponent.canHit(target)) {
                    this.dealDamageComponent.hurtTarget(entity, target, 75.0F);
                    target.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 100, 0, false, false));
                }
            }
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            EntityRayTraceResult trace = WyHelper.rayTraceEntities(entity, (double)1.5F);
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.SHORT_DISCHARGE_SFX.get(), SoundCategory.PLAYERS, 5.0F, 1.0F);
            AbsolutePierceProjectile proj = new AbsolutePierceProjectile(entity.level, entity);
            proj.setDamage(40.0F);
            proj.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 2.0F, 1.0F);
            entity.level.addFreshEntity(proj);
            ExplosionAbility explosion = AbilityHelper.newExplosion(entity, entity.level, entity.getX(), entity.getY(), entity.getZ(), 1.0F);
            explosion.setDamageOwner(false);
            explosion.setDestroyBlocks(false);
            explosion.setSmokeParticles(new CommonExplosionParticleEffect(2));
            explosion.setDamageEntities(true);
            explosion.doExplosion();
            this.animationComponent.stop(entity);
            this.cooldownComponent.startCooldown(entity, 600.0F);
        }

    }

    private AbsolutePierceProjectile createProjectile(LivingEntity entity) {
        AbsolutePierceProjectile proj = new AbsolutePierceProjectile(entity.level, entity);
        return proj;
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.SPEAR) && questProps.hasFinishedQuest(CartQuests.SPEAR_07);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Absolute Pierce", AbilityCategory.STYLE, AbsolutePierceRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(75.0F), RangeComponent.getTooltip(1.4F, RangeType.LINE), CooldownComponent.getTooltip(600.0F)}).setSourceType(new SourceType[]{SourceType.SLASH}).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.SHOCKWAVE).setUnlockCheck(AbsolutePierceRework::canUnlock).build();
    }
}