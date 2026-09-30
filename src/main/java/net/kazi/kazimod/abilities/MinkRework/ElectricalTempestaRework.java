//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.MinkRework;

import java.util.List;

import net.MrMagicalCart.cartaddon.abilities.electroextra.CartEleclawAbility;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartElectroHelper;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartSulongAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.BonusOperation;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.events.passives.MinkPassiveEvents;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ElectricalTempestaRework extends Ability {
    private static final float COOLDOWN_BONUS = 0.8F;
    private static final float DAMAGE_BONUS = 2.0F;
    private static final ITextComponent[] DESCRIPTION;
    private static final int COOLDOWN = 240;
    private static final int CHARGE_TIME = 5;
    private static final int WATER_RANGE = 10;
    private static final int RANGE = 8;
    private static final float RANGE_BONUS = 1.1F;
    private static final int DAMAGE = 20;
    private static final int ELECLAW_STACKS = 1;
    public static final AbilityCore<ElectricalTempestaRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private LightningDischargeEntity ballEntity = null;
    private boolean eleClawBuff = false;
    private final AnimationComponent animationComponent = new AnimationComponent(this);

    public ElectricalTempestaRework(AbilityCore<ElectricalTempestaRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.animationComponent, this.chargeComponent, this.rangeComponent, this.dealDamageComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.startCharging(entity, 5.0F);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.RYU_NO_IBUKI);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5, 1, false, false));
        if (this.chargeComponent.getChargeTime() % 2.0F == 0.0F) {
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.ELECTRO_CHARGING.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        }

        if (this.ballEntity == null) {
            LightningDischargeEntity ball = new LightningDischargeEntity(entity, entity.getX(), entity.getY(), entity.getZ(), entity.yRot, entity.xRot);
            entity.level.addFreshEntity(ball);
            this.ballEntity = ball;
        } else {
            float distance = 0.5F;
            Vector3d lookVec = entity.getLookAngle();
            double px = entity.getX() + lookVec.x * (double)distance;
            double py = entity.getY() + lookVec.x * (double)distance;
            double pz = entity.getZ() + lookVec.z * (double)distance;
            Vector3d pos = new Vector3d(px, py, pz);
            float percentage = 1.0F - this.chargeComponent.getChargeTime() / this.chargeComponent.getMaxChargeTime();
            this.ballEntity.setSize(percentage * 0.1F);
            this.ballEntity.setLightningLength(2.0F);
            this.ballEntity.moveTo(pos.x(), pos.y(), pos.z(), entity.yRot, entity.xRot);
        }

    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        this.eleClawBuff = false;
        if (!entity.level.isClientSide) {
            CartEleclawAbility eleclaw = (CartEleclawAbility)AbilityDataCapability.get(entity).getEquippedAbility(CartEleclawAbility.INSTANCE);
            if (eleclaw != null && eleclaw.isContinuous()) {
                eleclaw.reduceUsage(entity, 1);
                this.eleClawBuff = true;
            }

            boolean hasSulongActive = CartElectroHelper.hasSulongActive(entity);
            this.dealDamageComponent.getBonusManager().removeBonus(CartElectroHelper.SULONG_DAMAGE_BONUS);
            this.rangeComponent.getBonusManager().removeBonus(CartElectroHelper.SULONG_RANGE_BONUS);
            if (hasSulongActive) {
                this.dealDamageComponent.getBonusManager().addBonus(CartElectroHelper.SULONG_DAMAGE_BONUS, "Sulong Damage Bonus", BonusOperation.MUL, 2.0F);
                this.rangeComponent.getBonusManager().addBonus(CartElectroHelper.SULONG_RANGE_BONUS, "Sulong Range Bonus", BonusOperation.MUL, 2.0F);
            }

            if (this.ballEntity != null) {
                this.ballEntity.remove();
                this.ballEntity = null;
            }

            for(int i = 0; i < 3; ++i) {
                WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.ELECTRICAL_TEMPESTA_2.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            }

            float horizontal = entity.isInWater() ? 10.0F : 8.0F;
            horizontal = hasSulongActive ? horizontal * 1.1F : horizontal;
            float vertical = 4.0F;
            List<LivingEntity> targets = WyHelper.getNearbyLiving(entity.position(), entity.level, (double)horizontal, (double)vertical, (double)horizontal, ModEntityPredicates.getEnemyFactions(entity));
            ModDamageSource source = (ModDamageSource)this.dealDamageComponent.getDamageSource(entity);

            for(LivingEntity target : targets) {
                if (this.dealDamageComponent.hurtTarget(entity, target, this.eleClawBuff ? 20.0F * CartElectroHelper.getEleClawAmp() : 20.0F, source)) {
                    Vector3d dirVec = entity.position().subtract(target.position()).normalize();
                    target.addEffect(new EffectInstance((Effect)ModEffects.PARALYSIS.get(), 10, 0, false, false, true));
                    AbilityHelper.setDeltaMovement(target, -dirVec.x * (double)2.0F, (double)1.0F, -dirVec.z * (double)2.0F);
                }
            }

            int amount = 32;

            for(int j = 0; j < amount; ++j) {
                float boltSize = (float)WyHelper.randomWithRange(3, (int)horizontal);
                LightningEntity bolt = new LightningEntity(entity, entity.getX(), entity.getY(), entity.getZ(), (float)WyHelper.randomWithRange(0, 360), (float)WyHelper.randomWithRange(0, 5), boltSize, 8.0F, this.getCore());
                bolt.setColor(MinkPassiveEvents.MINK_LIGHTNING_COLOR);
                bolt.setAngle(60);
                bolt.setMaxLife(20);
                bolt.setDamage(0.0F);
                bolt.setExplosion(0, false);
                bolt.setSize(boltSize / 600.0F);
                bolt.setBranches((int)WyHelper.randomWithRange(1, 3));
                bolt.setSegments((int)((double)boltSize * 0.6));
                bolt.setLightningMimic(false);
                entity.level.addFreshEntity(bolt);
            }

            this.cooldownComponent.startCooldown(entity, 240.0F);
        }

    }

    private static boolean canUnlock(LivingEntity user) {
        IEntityStats props = EntityStatsCapability.get(user);
        return props.isMink() && props.getDoriki() >= (double)3000.0F;
    }

    static {
        DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "electrical_tempesta", new Pair[]{ImmutablePair.of("The user releases a charge of energy that deals damage to nearby enemies and knocks them back.", (Object)null), ImmutablePair.of("While %s is active the damage is increased by %s and the range of the ability is increased by %s.", new Object[]{AbilityHelper.mentionAbility(CartSulongAbility.INSTANCE), AbilityHelper.mentionText(Math.round(10.0F) + "%"), AbilityHelper.mentionText(Math.round(Math.abs(-1.0F) * 100.0F) + "%")})});
        INSTANCE = (new AbilityCore.Builder("Electrical Tempesta", AbilityCategory.RACIAL, ElectricalTempestaRework::new)).addDescriptionLine(new ITextComponent[]{DESCRIPTION[0]}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{IDescriptionLine.of(DESCRIPTION[1])}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(240.0F), ChargeComponent.getTooltip(10.0F), RangeComponent.getTooltip(8.0F, 10.0F, RangeType.AOE), DealDamageComponent.getTooltip(20.0F)}).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.LIGHTNING).setUnlockCheck(ElectricalTempestaRework::canUnlock).build();
    }
}
