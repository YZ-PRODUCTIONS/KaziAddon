//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.SoruRework;

import net.MrMagicalCart.cartaddon.abilities.soru.SoulHelper;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HealComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SoulRecoveryRework extends Ability {
    public static final AbilityCore<SoulRecoveryRework> INSTANCE;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "soul_recovery", new Pair[]{ImmutablePair.of("The user mends their body with collected souls.", (Object)null), ImmutablePair.of("§aSHIFT-USE§r: With the users own life-span they take their own soul and mend their body back to full health.", (Object)null)});
    private final HealComponent healComponent = new HealComponent(this);
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent).addTickEvent(this::tickContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final DamageTakenComponent damageTakenComponent;
    private Interval tickHeal;
    private boolean fullHeal;
    private static float HEAL_MIN_CD = 800.0F;  // doubled from 400.0F
    private static float HEAL_HOLD = 800.0F;    // doubled from 400.0F
    private static float HEAL_MAX_CD;
    private static float FULL_RESTORE_CD;
    private static float HEAL_AMOUNT;

    public SoulRecoveryRework(AbilityCore<SoulRecoveryRework> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.tickHeal = new Interval(40);
        this.fullHeal = false;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.healComponent, this.continuousComponent, this.damageTakenComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (SoulHelper.hasEnoughSouls(entity, ability, 2).isFail()) {  // doubled from 1
            entity.sendMessage(SoulHelper.NOT_ENOUGH_SOULS_WARN, entity.getUUID());
        } else {
            this.continuousComponent.triggerContinuity(entity, HEAL_HOLD);
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.fullHeal = false;
        this.tickHeal.restartIntervalToZero();
    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 20, 0, false, false));
        if (entity.getHealth() == entity.getMaxHealth()) {
            this.continuousComponent.stopContinuity(entity);
        }

        if (this.tickHeal.canTick()) {
            if (SoulHelper.getAvailableSouls(entity) < 2) {  // changed from <= 0 to < 2
                entity.sendMessage(SoulHelper.NOT_ENOUGH_SOULS_WARN, Util.NIL_UUID);
                this.continuousComponent.stopContinuity(entity);
                return;
            }

            SoulHelper.removeSouls(entity, 2);  // doubled from 1
            this.healComponent.healTarget(entity, entity, HEAL_AMOUNT);
        }

        WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.WARRIORS_SPIRIT.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.ONI_AWAKENING.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.HEAL.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        if (entity.isCrouching()) {
            IEntityStats stats = EntityStatsCapability.get(entity);
            if (stats == null) {
                return;
            }

            int doriki = (int)stats.getDoriki();
            if (doriki < 500) {
                entity.sendMessage(new TranslationTextComponent("message.cartaddon.not_enough_doriki"), Util.NIL_UUID);
                return;
            }

            stats.setDoriki((double)(doriki - 500));
            entity.setHealth(entity.getMaxHealth());
            WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.WARRIORS_SPIRIT.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.ONI_AWAKENING.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            this.fullHeal = true;
            entity.addEffect(new EffectInstance(Effects.ABSORPTION, 600, 4));
            this.continuousComponent.stopContinuity(entity);
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, this.fullHeal ? FULL_RESTORE_CD : HEAL_MIN_CD + HEAL_HOLD * 1.5F);
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        }

        return damage;
    }

    static {
        HEAL_MAX_CD = HEAL_MIN_CD + HEAL_HOLD * 1.5F;
        FULL_RESTORE_CD = 8000.0F;   // doubled from 4000.0F
        HEAL_AMOUNT = 8.0F;        // reduced 25% from 15.0F
        INSTANCE = (new AbilityCore.Builder("Soul Recovery", AbilityCategory.DEVIL_FRUITS, SoulRecoveryRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, HealComponent.getTooltip(HEAL_AMOUNT), ContinuousComponent.getTooltip(HEAL_HOLD), CooldownComponent.getTooltip(HEAL_MIN_CD, HEAL_MAX_CD)}).setSourceHakiNature(SourceHakiNature.HARDENING).build();
    }
}
