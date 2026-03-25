package net.kazi.kazimod.entities.boss.gojo;

import net.kazi.kazimod.abilities.BrawlerRework.SpinningBrawlRework;
import net.kazi.kazimod.abilities.BrawlerRework.SuplexRework;
import net.kazi.kazimod.abilities.Koku.*;
import net.kazi.kazimod.abilities.boss.gojo.BossInfinityAbility;
import net.kazi.kazimod.abilities.boss.gojo.BossHollowPurpleAbility;
import net.kazi.kazimod.abilities.boss.gojo.BossLapseBlueAbility;
import net.kazi.kazimod.abilities.boss.gojo.BossMaxOutputLapseBlueAbility;
import net.kazi.kazimod.abilities.boss.gojo.BossRedAbility;
import net.kazi.kazimod.entities.boss.gojo.goals.*;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;
import xyz.pixelatedw.mineminenomi.abilities.brawler.BrawlerPassiveBonusesAbility;
import xyz.pixelatedw.mineminenomi.api.challenges.InProgressChallenge;
import xyz.pixelatedw.mineminenomi.api.challenges.OPBossEntity;
import xyz.pixelatedw.mineminenomi.api.helpers.MobsHelper;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.ClimbOutOfHoleGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.ImprovedMeleeAttackGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.SprintTowardsTargetGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.AlwaysActiveAbilityWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.*;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.GeppoWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.SoruWrapperGoal;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModValues;

public class GojoBossEntity extends OPBossEntity<GojoBossEntity> {

    public boolean hollowPurpleFired;
    public boolean hollowNukeQueued;
    public boolean domainFinished;

    static final float NEAR_DEATH_THRESHOLD = 0.15f;
    private static final double RED_SPEED = 2.5;
    private static final double BLUE_SPEED = 2.4;

    public GojoBossEntity(EntityType<?> type, World world) {
        super(type, world);
        this.hollowPurpleFired = false;
        this.hollowNukeQueued = false;
        this.domainFinished = false;
    }

    public GojoBossEntity(InProgressChallenge challenge) {
        super(KaziEntities.GOJO_BOSS.get(), challenge);
        this.hollowPurpleFired = false;
        this.hollowNukeQueued = false;
        this.domainFinished = false;
    }

    @Override
    public void initBoss() {
        this.entityStats.setFaction(ModValues.PIRATE);
        this.entityStats.setRace(ModValues.HUMAN);
        this.entityStats.setFightingStyle(ModValues.BRAWLER);

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(750.0);
        this.setHealth(900.0f);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.91);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(15.0);
        this.getAttribute(Attributes.ARMOR).setBaseValue(1.0);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(10.0);
        this.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(8.0);

        if (this.getAttribute(ModAttributes.TOUGHNESS.get()) != null)
            this.getAttribute(ModAttributes.TOUGHNESS.get()).setBaseValue(12.0);
        if (this.getAttribute(ModAttributes.GCD.get()) != null)
            this.getAttribute(ModAttributes.GCD.get()).setBaseValue(15.0);
        if (this.getAttribute(ModAttributes.PUNCH_DAMAGE.get()) != null)
            this.getAttribute(ModAttributes.PUNCH_DAMAGE.get()).setBaseValue(8.0);
        if (this.getAttribute(ModAttributes.FAUX_PROTECTION.get()) != null)
            this.getAttribute(ModAttributes.FAUX_PROTECTION.get()).setBaseValue(14.0);
        if (this.getAttribute(ModAttributes.STEP_HEIGHT.get()) != null)
            this.getAttribute(ModAttributes.STEP_HEIGHT.get()).setBaseValue(1.0);
        if (this.getAttribute(ForgeMod.SWIM_SPEED.get()) != null)
            this.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(2.5);

        this.hakiCapability.setBusoshokuHakiExp(100.0f);
        this.hakiCapability.setKenbunshokuHakiExp(100.0f);
        this.entityStats.setDoriki(50000.0);

        // FIX: HollowPurpleAbility (player version) intentionally NOT equipped here.
        // Having both BossHollowPurpleAbility and HollowPurpleAbility equipped caused
        // BossHollowPurpleAbility.onUseEvent to also start HollowPurpleAbility's charge,
        // resulting in two projectiles firing when hollow purple was used.
        MobsHelper.unlockAndEquipAbility(this, BossHollowPurpleAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, BossLapseBlueAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, BossMaxOutputLapseBlueAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, BossRedAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, RedAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, LapseBlueAbility.INSTANCE);
        // HollowPurpleAbility.INSTANCE removed — caused double hollow purple projectile
        MobsHelper.unlockAndEquipAbility(this, DomainExpansionInfiniteVoidAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, MaxOutputLapseBlueAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SpinningBrawlRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SuplexRework.INSTANCE);

        MobsHelper.addBasicNPCGoals(this);

        this.goalSelector.addGoal(0, new ClimbOutOfHoleGoal(this));
        this.goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal(this, BrawlerPassiveBonusesAbility.INSTANCE));
        this.goalSelector.addGoal(0, new KenbunshokuHakiFutureSightWrapperGoal(this));
        this.goalSelector.addGoal(0, new HaoshokuHakiInfusionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiInternalDestructionWrapperGoal(this));
        // Infinity — boss version with 3× shorter cooldown, no pool restriction
        MobsHelper.unlockAndEquipAbility(this, BossInfinityAbility.INSTANCE);
        this.goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal<>((MobEntity) this,
                BossInfinityAbility.INSTANCE));
        this.goalSelector.addGoal(1, new ImprovedMeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(1, new SprintTowardsTargetGoal(this));
        this.goalSelector.addGoal(2, new GeppoWrapperGoal(this));
        this.goalSelector.addGoal(2, new SoruWrapperGoal(this));
        this.goalSelector.addGoal(3, new BossHollowPurpleWrapperGoal(this));
        this.goalSelector.addGoal(4, new BossSpinningBrawlWrapperGoal(this));
        this.goalSelector.addGoal(4, new BossSuplexWrapperGoal(this));
        this.goalSelector.addGoal(5, new BossLapseBlueWrapperGoal(this));
        this.goalSelector.addGoal(5, new BossMaxOutputLapseBlueWrapperGoal(this));
        this.goalSelector.addGoal(6, new BossRedWrapperGoal(this));
        this.goalSelector.addGoal(7, new GojoBossDomainGoal(this));
    }

    @Override
    public void remove() {
        // Force-cancel BossHollowPurpleAbility if it's mid-charge when the boss dies.
        // Without this, disableAbilities() tries to serialize the ability's ResourceLocation
        // which can be null for unregistered boss abilities, causing a NPE crash.
        if (!this.level.isClientSide && this.abilityData != null) {
            BossHollowPurpleAbility hollow = (BossHollowPurpleAbility)
                    this.abilityData.getEquippedAbility(BossHollowPurpleAbility.INSTANCE);
            if (hollow != null) hollow.forceCancel(this);
        }
        super.remove();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide || this.abilityData == null) return;

        net.minecraft.entity.LivingEntity target = this.getTarget();
        if (target == null) return;

        if (!hollowNukeQueued
                && this.getHealth() / this.getMaxHealth() <= NEAR_DEATH_THRESHOLD) {
            hollowNukeQueued = true;
            fireHollowNukeCombo(target);
        }
    }

    private void fireHollowNukeCombo(net.minecraft.entity.LivingEntity target) {
        if (net.kazi.kazimod.entities.projectiles.HollowNukeProjectile.ACTIVE_PROJECTILES
                .containsKey(this.getUUID())) return;

        net.kazi.kazimod.abilities.Koku.RedAbility redAbility =
                (net.kazi.kazimod.abilities.Koku.RedAbility) this.abilityData
                        .getEquippedAbility(RedAbility.INSTANCE);
        net.kazi.kazimod.abilities.Koku.LapseBlueAbility blueAbility =
                (net.kazi.kazimod.abilities.Koku.LapseBlueAbility) this.abilityData
                        .getEquippedAbility(LapseBlueAbility.INSTANCE);

        if (!(redAbility instanceof xyz.pixelatedw.mineminenomi.api.abilities.Ability)
                || !(blueAbility instanceof xyz.pixelatedw.mineminenomi.api.abilities.Ability)) {
            net.kazi.kazimod.KaziMod.LOGGER.warn("[GojoBoss] Near-death nuke: ability null");
            return;
        }

        net.minecraft.util.math.vector.Vector3d bossPos = this.position()
                .add(0, this.getEyeHeight() * 0.9, 0);
        net.minecraft.util.math.vector.Vector3d targetPos = target.position()
                .add(0, 5.0, 0);

        double dist = bossPos.distanceTo(targetPos);
        net.minecraft.util.math.vector.Vector3d targetVel = target.getDeltaMovement();

        net.minecraft.util.math.vector.Vector3d redDir = targetPos
                .add(targetVel.x * dist / 2.5, 0, targetVel.z * dist / 2.5)
                .subtract(bossPos).normalize();
        net.minecraft.util.math.vector.Vector3d blueDir = targetPos
                .add(targetVel.x * dist / 2.4, 0, targetVel.z * dist / 2.4)
                .subtract(bossPos).normalize();

        net.kazi.kazimod.entities.projectiles.RedProjectile red =
                new net.kazi.kazimod.entities.projectiles.RedProjectile(
                        this.level, this,
                        (xyz.pixelatedw.mineminenomi.api.abilities.Ability) redAbility);
        red.setPos(bossPos.x, bossPos.y, bossPos.z);
        red.setDeltaMovement(redDir.scale(RED_SPEED));
        this.level.addFreshEntity(red);

        net.kazi.kazimod.entities.projectiles.LapseBlueProjectile blue =
                new net.kazi.kazimod.entities.projectiles.LapseBlueProjectile(
                        this.level, this,
                        (xyz.pixelatedw.mineminenomi.api.abilities.Ability) blueAbility);
        blue.setPos(bossPos.x + blueDir.x * 0.5, bossPos.y, bossPos.z + blueDir.z * 0.5);
        blue.setDeltaMovement(blueDir.scale(BLUE_SPEED));
        this.level.addFreshEntity(blue);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 750.0)
                .add(Attributes.MOVEMENT_SPEED, 0.91)
                .add(Attributes.ATTACK_DAMAGE, 15.0)
                .add(Attributes.ARMOR, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 8.0)
                .add(Attributes.FOLLOW_RANGE, 250.0)
                .add(Attributes.FLYING_SPEED, 0.0);
    }
}