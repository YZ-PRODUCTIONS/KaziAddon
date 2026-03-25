package net.kazi.kazimod.entities.boss.sukuna;

import net.MrMagicalCart.cartaddon.abilities.brawlerextra.FistsOfLoveBarrageAbility;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.brawlerextra.FistsOfLoveBarrageWrapperGoal;
import net.kazi.kazimod.abilities.BrawlerRework.SpinningBrawlRework;
import net.kazi.kazimod.abilities.BrawlerRework.SuplexRework;
import net.kazi.kazimod.abilities.KamaRework.CleaveAbility;
import net.kazi.kazimod.abilities.KamaRework.SpiderwebCleaveAbility;
import net.kazi.kazimod.abilities.boss.sukuna.BossDismantleAbility;
import net.kazi.kazimod.abilities.boss.sukuna.BossFugaAbility;
import net.kazi.kazimod.abilities.boss.sukuna.BossMalevolentShrineAbility;
import net.kazi.kazimod.entities.boss.gojo.goals.BossSpinningBrawlWrapperGoal;
import net.kazi.kazimod.entities.boss.gojo.goals.BossSuplexWrapperGoal;
import net.kazi.kazimod.entities.boss.sukuna.goals.*;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;
import xyz.pixelatedw.mineminenomi.abilities.brawler.BrawlerPassiveBonusesAbility;
import xyz.pixelatedw.mineminenomi.api.challenges.InProgressChallenge;
import xyz.pixelatedw.mineminenomi.api.challenges.OPBossEntity;
import xyz.pixelatedw.mineminenomi.api.helpers.MobsHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.ClimbOutOfHoleGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.ImprovedMeleeAttackGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.SprintTowardsTargetGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.AlwaysActiveAbilityWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.*;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.GeppoWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.SoruWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.KamieWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.KamieWrapperGoal;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModValues;

import java.util.EnumSet;

public class SukunaBossEntity extends OPBossEntity<SukunaBossEntity> {

    public boolean domainFinished = false;

    public SukunaBossEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public SukunaBossEntity(InProgressChallenge challenge) {
        super((EntityType) KaziEntities.SUKUNA_BOSS.get(), challenge);
    }

    @Override
    public void remove() {
        // Force domain cleanup when boss is removed for any reason
        // (challenge failed, boss killed, server shutdown)
        if (!level.isClientSide && abilityData != null) {
            net.kazi.kazimod.abilities.boss.sukuna.BossMalevolentShrineAbility domain =
                    (net.kazi.kazimod.abilities.boss.sukuna.BossMalevolentShrineAbility)
                            abilityData.getEquippedAbility(
                                    net.kazi.kazimod.abilities.boss.sukuna.BossMalevolentShrineAbility.INSTANCE);
            if (domain != null && (domain.isDomainActive() || domain.isCharging())) {
                domain.stopChargingAndCooldown(this);
            }
        }
        super.remove();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier) {
        return false; // immune to fall damage
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.isFire()) return false; // immune to fire damage
        return super.hurt(source, amount);
    }

    @Override
    public void initBoss() {
        entityStats.setFaction(ModValues.PIRATE);
        entityStats.setRace(ModValues.HUMAN);
        entityStats.setFightingStyle(ModValues.BRAWLER);

        double scalingFactor = CommonConfig.INSTANCE.getDorikiLimit() / 10000.0;
        double scaledHp     = 825.0 * scalingFactor;
        double scaledDoriki = 50000.0 * scalingFactor;

        getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledHp);
        setHealth((float) scaledHp);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.93);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(15.0);
        getAttribute(Attributes.ARMOR).setBaseValue(2.0);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(10.0);
        getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(3.0);

        if (getAttribute(ModAttributes.TOUGHNESS.get()) != null)
            getAttribute(ModAttributes.TOUGHNESS.get()).setBaseValue(12.0);
        if (getAttribute(ModAttributes.GCD.get()) != null)
            getAttribute(ModAttributes.GCD.get()).setBaseValue(15.0);
        if (getAttribute(ModAttributes.PUNCH_DAMAGE.get()) != null)
            getAttribute(ModAttributes.PUNCH_DAMAGE.get()).setBaseValue(2.0);
        if (getAttribute(ModAttributes.FAUX_PROTECTION.get()) != null)
            getAttribute(ModAttributes.FAUX_PROTECTION.get()).setBaseValue(12.0);
        if (getAttribute(ModAttributes.STEP_HEIGHT.get()) != null)
            getAttribute(ModAttributes.STEP_HEIGHT.get()).setBaseValue(1.0);
        if (getAttribute(ForgeMod.SWIM_SPEED.get()) != null)
            getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(2.5);

        hakiCapability.setBusoshokuHakiExp(100.0f);
        hakiCapability.setKenbunshokuHakiExp(100.0f);
        entityStats.setDoriki(scaledDoriki);

        MobsHelper.unlockAndEquipAbility(this, BossDismantleAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, BossFugaAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, BossMalevolentShrineAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, CleaveAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SpiderwebCleaveAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SpinningBrawlRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SuplexRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, FistsOfLoveBarrageAbility.INSTANCE);

        MobsHelper.addBasicNPCGoals(this);

        goalSelector.addGoal(0, new ClimbOutOfHoleGoal((MobEntity) this));
        goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal<>((MobEntity) this,
                BrawlerPassiveBonusesAbility.INSTANCE));
        goalSelector.addGoal(0, new KenbunshokuHakiFutureSightWrapperGoal((MobEntity) this));
        goalSelector.addGoal(0, new HaoshokuHakiInfusionWrapperGoal((MobEntity) this));
        goalSelector.addGoal(0, new BusoshokuHakiInternalDestructionWrapperGoal((MobEntity) this));
        goalSelector.addGoal(0, new BusoshokuHakiHardeningWrapperGoal((MobEntity) this));

        goalSelector.addGoal(1, new ImprovedMeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(1, new SprintTowardsTargetGoal((MobEntity) this));

        goalSelector.addGoal(0, new KamieWrapperGoal((MobEntity) this));

        goalSelector.addGoal(2, new SoruWrapperGoal((MobEntity) this));
        goalSelector.addGoal(2, new GeppoWrapperGoal(this));

        goalSelector.addGoal(3, new BossDismantleWrapperGoal((MobEntity) this));
        FistsOfLoveBarrageWrapperGoal fistsGoal = new FistsOfLoveBarrageWrapperGoal((MobEntity) this);
        ((FistsOfLoveBarrageAbility) fistsGoal.getAbility()).getComponent(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.COOLDOWN)
                .ifPresent(c -> c.startCooldown(this, 60.0F));
        goalSelector.addGoal(3, fistsGoal);
        goalSelector.addGoal(3, new BossCleaveWrapperGoal((MobEntity) this));

        goalSelector.addGoal(4, new BossSpiderwebCleaveWrapperGoal((MobEntity) this));

        goalSelector.addGoal(5, new BossSpinningBrawlWrapperGoal((MobEntity) this));
        goalSelector.addGoal(5, new BossSuplexWrapperGoal((MobEntity) this));

        goalSelector.addGoal(1, new SukunaBossDomainGoal(this));
        goalSelector.addGoal(2, new BossFugaWrapperGoal((MobEntity) this));
    }

    public static class AggressiveSoruGoal extends Goal {
        private final SukunaBossEntity boss;
        private int cooldown = 0;

        public AggressiveSoruGoal(SukunaBossEntity boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) { cooldown--; return false; }
            if (boss.getTarget() == null || !boss.getTarget().isAlive()) return false;
            return boss.distanceToSqr(boss.getTarget()) > 64.0;
        }

        @Override
        public void start() {
            Vector3d dir = boss.getTarget().position().subtract(boss.position()).normalize();
            boss.setDeltaMovement(dir.x * 3.5, boss.getDeltaMovement().y, dir.z * 3.5);
            cooldown = 40;
        }

        @Override public boolean canContinueToUse() { return false; }
        @Override public void stop() {}
    }

    public static class AggressiveGeppoGoal extends Goal {
        private final SukunaBossEntity boss;
        private int cooldown = 0;

        public AggressiveGeppoGoal(SukunaBossEntity boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) { cooldown--; return false; }
            if (boss.getTarget() == null || !boss.getTarget().isAlive()) return false;
            if (!boss.isOnGround()) return false;
            double heightDiff = boss.getTarget().getY() - boss.getY();
            if (heightDiff > 3.0) return true;
            if (heightDiff > 1.5 && boss.distanceToSqr(boss.getTarget()) < 100.0) return true;
            return false;
        }

        @Override
        public void start() {
            Vector3d dir = boss.getTarget().position().subtract(boss.position()).normalize();
            boss.setDeltaMovement(dir.x * 1.5, 1.2, dir.z * 1.5);
            cooldown = 30;
        }

        @Override public boolean canContinueToUse() { return false; }
        @Override public void stop() {}
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes()
                .add(Attributes.MAX_HEALTH,           825.0)
                .add(Attributes.MOVEMENT_SPEED,       0.93)
                .add(Attributes.FOLLOW_RANGE,         250.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR,                10.0)
                .add(Attributes.ARMOR_TOUGHNESS,      8.0)
                .add(Attributes.ATTACK_DAMAGE,        5.0)
                .add(Attributes.ATTACK_KNOCKBACK,     0.0);
    }
}