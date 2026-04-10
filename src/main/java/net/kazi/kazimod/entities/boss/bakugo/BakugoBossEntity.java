package net.kazi.kazimod.entities.boss.bakugo;

import net.kazi.kazimod.abilities.BomuRework.ClusterBombAbility;
import net.kazi.kazimod.abilities.BomuRework.ExplosiveHoldAbility;
import net.kazi.kazimod.abilities.BomuRework.ExplosivePunchRework;
import net.kazi.kazimod.abilities.BomuRework.KickBombRework;
import net.kazi.kazimod.abilities.BomuRework.PropellingBlastsAbility;
import net.kazi.kazimod.abilities.BomuRework.StunGrenadeAbility;
import net.kazi.kazimod.entities.boss.luffy.goals.DirectAbilityGoal;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;
import xyz.pixelatedw.mineminenomi.abilities.brawler.BrawlerPassiveBonusesAbility;
import xyz.pixelatedw.mineminenomi.abilities.brawler.DamageAbsorptionAbility;
import xyz.pixelatedw.mineminenomi.abilities.brawler.HakaiHoAbility;
import xyz.pixelatedw.mineminenomi.abilities.brawler.SpinningBrawlAbility;
import xyz.pixelatedw.mineminenomi.abilities.brawler.SuplexAbility;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.GeppoAbility;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.KamieAbility;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.SoruAbility;
import xyz.pixelatedw.mineminenomi.api.challenges.InProgressChallenge;
import xyz.pixelatedw.mineminenomi.api.challenges.OPBossEntity;
import xyz.pixelatedw.mineminenomi.api.helpers.MobsHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.ClimbOutOfHoleGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.ImprovedMeleeAttackGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.SprintTowardsTargetGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.AlwaysActiveAbilityWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.brawler.DamageAbsorptionWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.brawler.HakaiHoWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.brawler.SpinningBrawlWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.brawler.SuplexWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.BusoshokuHakiHardeningWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.BusoshokuHakiInternalDestructionWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.HaoshokuHakiInfusionWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.KenbunshokuHakiFutureSightWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.GeppoWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.KamieWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.SoruWrapperGoal;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModValues;

public class BakugoBossEntity extends OPBossEntity<BakugoBossEntity> {

    private static final double CLUSTER_MIN_DISTANCE = 7.0D;
    private static final double CLUSTER_MAX_DISTANCE = 30.0D;
    private static final double STUN_MAX_DISTANCE = 14.0D;
    private static final double HOLD_MAX_DISTANCE = 4.5D;
    private static final double KICK_MIN_DISTANCE = 4.0D;
    private static final double KICK_MAX_DISTANCE = 16.0D;
    private static final double PROPEL_TRIGGER_DISTANCE = 16.0D;

    public BakugoBossEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public BakugoBossEntity(InProgressChallenge challenge) {
        super(KaziEntities.BAKUGO_BOSS.get(), challenge);
    }

    @Override
    public void tick() {
        if (this.isPassenger()) {
            this.stopRiding();
        }
        super.tick();
    }

    @Override
    protected boolean canRide(Entity entity) {
        return false;
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    @Override
    public void initBoss() {
        this.entityStats.setFaction(ModValues.PIRATE);
        this.entityStats.setRace(ModValues.HUMAN);
        this.entityStats.setFightingStyle(ModValues.BRAWLER);

        double scalingFactor = CommonConfig.INSTANCE.getDorikiLimit() / 10000.0;
        double scaledHp = 800.0D * scalingFactor;
        double scaledDoriki = 50000.0D * scalingFactor;

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledHp);
        this.setHealth((float) scaledHp);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.96D);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(16.0D);
        this.getAttribute(Attributes.ARMOR).setBaseValue(4.0D);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(10.0D);
        this.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(3.0D);

        if (this.getAttribute(ModAttributes.TOUGHNESS.get()) != null) {
            this.getAttribute(ModAttributes.TOUGHNESS.get()).setBaseValue(10.0D);
        }
        if (this.getAttribute(ModAttributes.GCD.get()) != null) {
            this.getAttribute(ModAttributes.GCD.get()).setBaseValue(12.0D);
        }
        if (this.getAttribute(ModAttributes.PUNCH_DAMAGE.get()) != null) {
            this.getAttribute(ModAttributes.PUNCH_DAMAGE.get()).setBaseValue(10.0D);
        }
        if (this.getAttribute(ModAttributes.FAUX_PROTECTION.get()) != null) {
            this.getAttribute(ModAttributes.FAUX_PROTECTION.get()).setBaseValue(10.0D);
        }
        if (this.getAttribute(ModAttributes.STEP_HEIGHT.get()) != null) {
            this.getAttribute(ModAttributes.STEP_HEIGHT.get()).setBaseValue(1.0D);
        }
        if (this.getAttribute(ForgeMod.SWIM_SPEED.get()) != null) {
            this.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(2.5D);
        }

        this.hakiCapability.setBusoshokuHakiExp(100.0F);
        this.hakiCapability.setKenbunshokuHakiExp(100.0F);
        this.entityStats.setDoriki(scaledDoriki);

        this.devilFruitData.setDevilFruit(new net.minecraft.util.ResourceLocation("mineminenomi", "bomu_bomu_no_mi"));
        this.devilFruitData.setAwakenedFruit(true);

        MobsHelper.unlockAndEquipAbility(this, ExplosivePunchRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, KickBombRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, PropellingBlastsAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, ClusterBombAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, ExplosiveHoldAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, StunGrenadeAbility.INSTANCE);

        MobsHelper.unlockAndEquipAbility(this, SoruAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, GeppoAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, KamieAbility.INSTANCE);

        MobsHelper.unlockAndEquipAbility(this, DamageAbsorptionAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SpinningBrawlAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SuplexAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, HakaiHoAbility.INSTANCE);

        MobsHelper.addBasicNPCGoals(this);

        this.goalSelector.addGoal(0, new ClimbOutOfHoleGoal(this));
        this.goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal<>(this, BrawlerPassiveBonusesAbility.INSTANCE));
        this.goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal<>(this, ExplosivePunchRework.INSTANCE));
        this.goalSelector.addGoal(0, new KenbunshokuHakiFutureSightWrapperGoal(this));
        this.goalSelector.addGoal(0, new HaoshokuHakiInfusionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiInternalDestructionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiHardeningWrapperGoal(this));
        this.goalSelector.addGoal(0, new KamieWrapperGoal(this));

        this.goalSelector.addGoal(1, new ImprovedMeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(1, new SprintTowardsTargetGoal(this));

        this.goalSelector.addGoal(2, new SoruWrapperGoal(this));
        this.goalSelector.addGoal(2, new GeppoWrapperGoal(this));
        this.goalSelector.addGoal(2, new PropellingBlastsGoal(this));

        this.goalSelector.addGoal(3, new DamageAbsorptionWrapperGoal(this));
        this.goalSelector.addGoal(3, new SpinningBrawlWrapperGoal(this));
        this.goalSelector.addGoal(3, new SuplexWrapperGoal(this));
        this.goalSelector.addGoal(3, new HakaiHoWrapperGoal(this));
        this.goalSelector.addGoal(3, new ExplosiveHoldGoal(this));

        this.goalSelector.addGoal(4, new StunGrenadeGoal(this));
        this.goalSelector.addGoal(4, new ClusterBombGoal(this));
        this.goalSelector.addGoal(4, new KickBombGoal(this));
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 800.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.96D)
                .add(Attributes.FOLLOW_RANGE, 250.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D);
    }

    private abstract static class BakugoAbilityGoal<A extends xyz.pixelatedw.mineminenomi.api.abilities.Ability> extends DirectAbilityGoal<A> {
        protected BakugoAbilityGoal(MobEntity entity, xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<A> core) {
            super(entity, core);
        }

        protected LivingEntity target() {
            return this.entity.getTarget();
        }

        protected double distanceToTarget() {
            LivingEntity target = this.target();
            return target == null ? Double.MAX_VALUE : this.entity.distanceTo(target);
        }
    }

    private static class ClusterBombGoal extends BakugoAbilityGoal<ClusterBombAbility> {
        private ClusterBombGoal(MobEntity entity) {
            super(entity, ClusterBombAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            double distance = this.distanceToTarget();
            return distance >= CLUSTER_MIN_DISTANCE && distance <= CLUSTER_MAX_DISTANCE && this.entity.getSensing().canSee(this.target());
        }

        @Override
        protected boolean canContinueExtra() {
            return isContinuous();
        }
    }

    private static class StunGrenadeGoal extends BakugoAbilityGoal<StunGrenadeAbility> {
        private StunGrenadeGoal(MobEntity entity) {
            super(entity, StunGrenadeAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            LivingEntity target = this.target();
            if (target == null) {
                return false;
            }
            return this.distanceToTarget() <= STUN_MAX_DISTANCE && this.entity.getSensing().canSee(target);
        }

        @Override
        protected boolean canContinueExtra() {
            return isCharging();
        }
    }

    private static class ExplosiveHoldGoal extends BakugoAbilityGoal<ExplosiveHoldAbility> {
        private ExplosiveHoldGoal(MobEntity entity) {
            super(entity, ExplosiveHoldAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            LivingEntity target = this.target();
            if (target == null) {
                return false;
            }
            return this.distanceToTarget() <= HOLD_MAX_DISTANCE && this.entity.getSensing().canSee(target);
        }

        @Override
        protected boolean canContinueExtra() {
            return isCharging() || isContinuous();
        }
    }

    private static class KickBombGoal extends BakugoAbilityGoal<KickBombRework> {
        private KickBombGoal(MobEntity entity) {
            super(entity, KickBombRework.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            LivingEntity target = this.target();
            if (target == null) {
                return false;
            }
            double distance = this.distanceToTarget();
            return this.entity.isOnGround()
                    && distance >= KICK_MIN_DISTANCE
                    && distance <= KICK_MAX_DISTANCE
                    && this.entity.getSensing().canSee(target);
        }

        @Override
        protected boolean canContinueExtra() {
            return isContinuous();
        }
    }

    private static class PropellingBlastsGoal extends BakugoAbilityGoal<PropellingBlastsAbility> {
        private PropellingBlastsGoal(MobEntity entity) {
            super(entity, PropellingBlastsAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            LivingEntity target = this.target();
            if (target == null) {
                return false;
            }
            double distance = this.distanceToTarget();
            double heightDifference = target.getY() - this.entity.getY();
            return distance >= PROPEL_TRIGGER_DISTANCE || heightDifference >= 3.0D;
        }
    }
}
