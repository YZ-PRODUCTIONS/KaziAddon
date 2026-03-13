//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.MinkRework;

import net.MrMagicalCart.cartaddon.abilities.electroextra.CartEleclawAbility;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartElectroHelper;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartSulongAbility;
import net.MrMagicalCart.cartaddon.entities.projectiles.electro.CartElectricalShowerProjectile;
import net.kazi.kazimod.entities.projectiles.ElectricalShowerReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
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
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RepeaterComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ElectricalShowerRework extends Ability {
    private static final float COOLDOWN_BONUS = 0.8F;
    private static final ITextComponent[] DESCRIPTION;
    private static final int COOLDOWN = 280;
    private static final int CHARGE_TIME = 20;
    private static final int DAMAGE = 30;
    private static final int ELECLAW_STACKS = 2;
    public static final AbilityCore<ElectricalShowerRework> INSTANCE;
    private final StackComponent stackComponent = new StackComponent(this);
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final DamageTakenComponent damageTakenComponent;
    private final ProjectileComponent projectileComponent;
    private final ContinuousComponent continuousComponent;
    private final RepeaterComponent repeaterComponent;
    private LightningDischargeEntity ballEntity;
    private boolean hasFallDamage;
    private boolean eleClawBuff = false;

    public ElectricalShowerRework(AbilityCore<ElectricalShowerRework> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.projectileComponent = new ProjectileComponent(this, this::createProjectile);
        this.continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
        this.repeaterComponent = (new RepeaterComponent(this)).addTriggerEvent(this::triggerRepeaterEvent).addStopEvent(this::stopRepeaterEvent);
        this.ballEntity = null;
        this.hasFallDamage = true;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.stackComponent, this.chargeComponent, this.damageTakenComponent, this.projectileComponent, this.continuousComponent, this.repeaterComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.chargeComponent.startCharging(entity, 20.0F);
        }

    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5, 1, false, false));
        if (this.chargeComponent.getChargeTime() % 5.0F == 0.0F) {
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.ELECTRO_CHARGING.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        }

        float percentage = 1.0F - this.chargeComponent.getChargeTime() / this.chargeComponent.getMaxChargeTime();
        if (this.ballEntity == null) {
            LightningDischargeEntity ball = new LightningDischargeEntity(entity, entity.getX(), entity.getY(), entity.getZ(), entity.yRot, entity.xRot);
            this.ballEntity = ball;
        } else {
            float distance = percentage * 2.0F;
            Vector3d lookVec = entity.getLookAngle();
            double px = entity.getX() + lookVec.x * (double)distance;
            double py = entity.getEyeY() * 0.85 + lookVec.y * (double)distance;
            double pz = entity.getZ() + lookVec.z * (double)distance;
            Vector3d pos = new Vector3d(px, py, pz);
            this.ballEntity.setSize(percentage * 0.3F);
            this.ballEntity.setLightningLength(3.0F);
            this.ballEntity.moveTo(pos.x(), pos.y(), pos.z(), entity.yRot, entity.xRot);
        }

    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.eleClawBuff = false;
        if (this.ballEntity != null) {
            this.ballEntity.remove();
            this.ballEntity = null;
        }

        CartEleclawAbility eleclaw = (CartEleclawAbility)AbilityDataCapability.get(entity).getEquippedAbility(CartEleclawAbility.INSTANCE);
        if (eleclaw != null && eleclaw.isContinuous()) {
            eleclaw.reduceUsage(entity, 2);
            this.eleClawBuff = true;
        }

        this.continuousComponent.startContinuity(entity);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hasFallDamage = false;
        this.repeaterComponent.start(entity, 12, 3);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        boolean hasSulongActive = CartElectroHelper.hasSulongActive(entity);
        this.cooldownComponent.startCooldown(entity, hasSulongActive ? 224.0F : 280.0F);
    }

    private void triggerRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity, 5.0F, 1.0F);
        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.LIGHTNING_TELEPORT.get(), SoundCategory.PLAYERS, 3.0F, 1.55F);
    }

    private void stopRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.stopContinuity(entity);
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (!this.hasFallDamage && damageSource == DamageSource.FALL) {
            this.hasFallDamage = true;
            return 0.0F;
        } else {
            return damage;
        }
    }

    private ElectricalShowerReworkProjectile createProjectile(LivingEntity entity) {
        ElectricalShowerReworkProjectile proj = new ElectricalShowerReworkProjectile(entity.level, entity);
        boolean hasSulongActive = CartElectroHelper.hasSulongActive(entity);
        if (this.eleClawBuff || hasSulongActive) {
            proj.setDamage(30.0F * CartElectroHelper.getEleClawAmp());
        }

        proj.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 3.0F, 0.0F);
        return proj;
    }

    private static boolean canUnlock(LivingEntity user) {
        IEntityStats props = EntityStatsCapability.get(user);
        return props.isMink() && props.getDoriki() >= (double)7000.0F;
    }

    static {
        DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "electrical_shower", new Pair[]{ImmutablePair.of("Launches the user into the air and showers down lightning bolts underneath.", (Object)null), ImmutablePair.of("While %s is active the cooldown of this ability is reduced by %s and the damage is increased by %s.", new Object[]{AbilityHelper.mentionAbility(CartSulongAbility.INSTANCE), AbilityHelper.mentionText(Math.round(20.0F) + "%"), AbilityHelper.mentionText(Math.round(Math.abs(-0.15F) * 100.0F) + "%")})});
        INSTANCE = (new AbilityCore.Builder("Electrical Shower", AbilityCategory.RACIAL, ElectricalShowerRework::new)).addDescriptionLine(new ITextComponent[]{DESCRIPTION[0]}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{IDescriptionLine.of(DESCRIPTION[1])}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(280.0F), ChargeComponent.getTooltip(60.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.LIGHTNING).setUnlockCheck(ElectricalShowerRework::canUnlock).build();
    }
}
