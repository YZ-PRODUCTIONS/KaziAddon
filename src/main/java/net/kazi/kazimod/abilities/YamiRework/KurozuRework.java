package net.kazi.kazimod.abilities.YamiRework;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class KurozuRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "kurouzu", new Pair[]{ImmutablePair.of((Object)"Creates a strong gravitational force, that pulls the opponent towards the user.", null)});
    private static final float COOLDOWN = 300.0f;
    private static final float DAMAGE = 30.0f;
    private static final float CONTINUITY_TIME = 100.0f;
    private static final float CHARGE_TIME = 20.0f;
    public static final AbilityCore<KurozuRework> INSTANCE = new AbilityCore.Builder<KurozuRework>("Kurouzu", AbilityCategory.DEVIL_FRUITS, KurozuRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(240.0f), ContinuousComponent.getTooltip(200.0f), ChargeComponent.getTooltip(20.0f), DealDamageComponent.getTooltip(30.0f)).setIcon(new net.minecraft.util.ResourceLocation("mineminenomi", "textures/abilities/kurouzu.png")).setSourceType(SourceType.FIST).build();
    private final ContinuousComponent continuousComponent = new ContinuousComponent((IAbility)this, true).addStartEvent(this::onContinuityStart).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private final GrabEntityComponent grabEntityComponent = new GrabEntityComponent(this, false, false, 1.0f)
            .addGrabEvent((user, target, ability) -> target != null && !AbilityHelper.isDodging(target));
    private final ChargeComponent chargeComponent = new ChargeComponent(this).addStartEvent(this::onChargeStart).addTickEvent(this::onChargeTick).addEndEvent(this::onChargeEnd);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private Interval particleInterval = new Interval(5);

    public KurozuRework(AbilityCore<KurozuRework> core) {
        super(core);
        this.isNew = true;
        super.addComponents(this.continuousComponent, this.grabEntityComponent, this.chargeComponent, this.dealDamageComponent, this.animationComponent);
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging()) {
            return;
        }
        this.continuousComponent.triggerContinuity(entity, 200.0f);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.POINT_LEFT_ARM);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }
        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities((Entity)entity, 128.0);
        double i = mop.getLocation().x;
        double j = mop.getLocation().y - (mop instanceof EntityRayTraceResult ? 1.0 : 0.0);
        double k = mop.getLocation().z;
        boolean canTick = this.particleInterval.canTick();
        if (canTick) {
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.KUROUZU.get(), (Entity)entity, i, j, k);
            Vector3d lookVec = entity.getLookAngle().normalize();
            Vector3d particlePos = entity.position().add(lookVec.x, lookVec.y + 1.5, lookVec.z);
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.KUROUZU_CHARGING.get(), (Entity)entity, particlePos.x(), particlePos.y(), particlePos.z());
        }
        if (mop.getType() == RayTraceResult.Type.MISS) {
            return;
        }
        List<LivingEntity> targets = WyHelper.getNearbyLiving(new Vector3d(i, j, k), (IWorld)entity.level, 5.0, ModEntityPredicates.getEnemyFactions(entity));
        for (LivingEntity target : targets) {
            // Pulling and debuffs bypass damage events, so honor the dodge pool first.
            if (AbilityHelper.isDodging(target)) continue;
            Vector3d pos = entity.position().subtract(target.position()).normalize().scale(0.5D);
            AbilityHelper.setDeltaMovement((Entity)target, pos);
            if (!canTick) continue;
            target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 10, 5));
            target.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 10, 5));
            if (!this.grabEntityComponent.grabNearest(entity, false)) continue;
            this.continuousComponent.stopContinuity(entity);
            this.chargeComponent.startCharging(entity, 20.0f);
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }
        if (!this.grabEntityComponent.hasGrabbedEntity()) {
            this.animationComponent.stop(entity);
            this.cooldownComponent.startCooldown(entity, 240.0f);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }
        if (!this.grabEntityComponent.canContinueGrab(entity)) {
            this.chargeComponent.stopCharging(entity);
            return;
        }
        LivingEntity target = this.grabEntityComponent.getGrabbedEntity();
        target.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN, 100, 5));
        if (target instanceof PlayerEntity) {
            AbilityHelper.disableAbilities(target, 20, abl -> abl.getCore().getCategory() == AbilityCategory.DEVIL_FRUITS);
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }
        if (!this.grabEntityComponent.canContinueGrab(entity)) {
            this.chargeComponent.stopCharging(entity);
            return;
        }
        LivingEntity target = this.grabEntityComponent.getGrabbedEntity();
        Vector3d pos = VectorHelper.calculateRotationBasedOffsetPosition(entity.position(), entity.yBodyRot, -0.5, (double)(-target.getBbHeight()) / 2.0, (double)target.getBbWidth() - 0.2);
        AbilityHelper.setDeltaMovement((Entity)target, pos.subtract(target.position()), true);
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        LivingEntity target;
        if (entity.level.isClientSide) {
            return;
        }
        if (this.grabEntityComponent.canContinueGrab(entity) && this.dealDamageComponent.hurtTarget(entity, target = this.grabEntityComponent.getGrabbedEntity(), 30.0f)) {
            Vector3d dir = entity.getLookAngle().multiply(3.0, 0.0, 3.0);
            AbilityHelper.setDeltaMovement((Entity)target, target.getDeltaMovement().add(dir.x, 0.2, dir.z));
        }
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 240.0f);
    }
}
