package net.kazi.kazimod.entities.boss.luffy;

import net.kazi.kazimod.abilities.GomuRework.*;
import net.kazi.kazimod.abilities.boss.luffy.BossKaminariAbility;
import net.kazi.kazimod.entities.boss.luffy.goals.*;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;
import xyz.pixelatedw.mineminenomi.abilities.brawler.BrawlerPassiveBonusesAbility;
import xyz.pixelatedw.mineminenomi.api.challenges.InProgressChallenge;
import xyz.pixelatedw.mineminenomi.api.challenges.OPBossEntity;
import xyz.pixelatedw.mineminenomi.api.helpers.MobsHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.*;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.AlwaysActiveAbilityWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.brawler.*;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.*;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.GeppoWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.KamieWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.SoruWrapperGoal;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModValues;

public class LuffyBossEntity extends OPBossEntity<LuffyBossEntity> {

    private static final String NBT_GEAR5 = "LuffyGearFifthAwakened";

    private static final DataParameter<Boolean> GEAR5 =
            EntityDataManager.defineId(LuffyBossEntity.class, DataSerializers.BOOLEAN);

    /** True once Luffy crosses 50% HP — switches to Gear Fifth. */
    public boolean gear5Awakened = false;

    public LuffyBossEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public LuffyBossEntity(InProgressChallenge challenge) {
        super((EntityType) KaziEntities.LUFFY_BOSS.get(), challenge);
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GEAR5, false);
    }

    public boolean isGear5Awakened()           { return this.entityData.get(GEAR5); }
    public void    setGear5Awakened(boolean v) { this.gear5Awakened = v; this.entityData.set(GEAR5, v); }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean(NBT_GEAR5, gear5Awakened);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains(NBT_GEAR5)) setGear5Awakened(nbt.getBoolean(NBT_GEAR5));
    }

    @Override public boolean causeFallDamage(float d, float m) { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.isFire()) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void initBoss() {
        entityStats.setFaction(ModValues.PIRATE);
        entityStats.setRace(ModValues.HUMAN);
        entityStats.setFightingStyle(ModValues.BRAWLER);

        double scalingFactor = CommonConfig.INSTANCE.getDorikiLimit() / 10000.0;
        double scaledHp     = 750.0   * scalingFactor;
        double scaledDoriki = 50000.0 * scalingFactor; // must be >= 5000 for hasNativeHaki() to return true

        getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledHp);
        setHealth((float) scaledHp);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.93);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(16.0);
        getAttribute(Attributes.ARMOR).setBaseValue(2.0);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(10.0);
        getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(3.0);

        if (getAttribute(ModAttributes.TOUGHNESS.get()) != null)
            getAttribute(ModAttributes.TOUGHNESS.get()).setBaseValue(12.0);
        if (getAttribute(ModAttributes.GCD.get()) != null)
            getAttribute(ModAttributes.GCD.get()).setBaseValue(15.0);
        if (getAttribute(ModAttributes.PUNCH_DAMAGE.get()) != null)
            getAttribute(ModAttributes.PUNCH_DAMAGE.get()).setBaseValue(8.0);
        if (getAttribute(ModAttributes.FAUX_PROTECTION.get()) != null)
            getAttribute(ModAttributes.FAUX_PROTECTION.get()).setBaseValue(6.0);
        if (getAttribute(ModAttributes.STEP_HEIGHT.get()) != null)
            getAttribute(ModAttributes.STEP_HEIGHT.get()).setBaseValue(1.0);
        if (getAttribute(ForgeMod.SWIM_SPEED.get()) != null)
            getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(2.5);

        hakiCapability.setBusoshokuHakiExp(100.0f);
        hakiCapability.setKenbunshokuHakiExp(100.0f);
        hakiCapability.setHaoshokuHakiColour(0xFFD700); // gold
        entityStats.setDoriki(scaledDoriki);

        // Gomu fruit + awakened so GearFifthRework can unlock
        devilFruitData.setDevilFruit(
                new net.minecraft.util.ResourceLocation("mineminenomi", "gomu_gomu_no_mi"));
        devilFruitData.setAwakenedFruit(true);

        // ── Rework abilities ──────────────────────────────────────────────────
        this.goalSelector.addGoal(0, new ClimbOutOfHoleGoal(this));
        this.goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal(this, BrawlerPassiveBonusesAbility.INSTANCE));
        MobsHelper.unlockAndEquipAbility(this, GomuGomuNoPistolRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, GomuGomuNoGatlingRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, GomuGomuNoBazookaRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, GomuGomuNoRocketRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, GomuGomuNoRedRocAbility.INSTANCE);

        // Gear Fifth only rework abilities
        MobsHelper.unlockAndEquipAbility(this, GomuGomuNoDawnWhipRework.INSTANCE);  // G5 only
        MobsHelper.unlockAndEquipAbility(this, BossKaminariAbility.INSTANCE);       // G5 only, grounded + built-in aim

        // GearFifthRework (awakening trigger)
        MobsHelper.unlockAndEquipAbility(this, GearFifthRework.INSTANCE);

        // ── Haki ─────────────────────────────────────────────────────────────


        // ── Rokushiki ─────────────────────────────────────────────────────────
        MobsHelper.unlockAndEquipAbility(this,
                xyz.pixelatedw.mineminenomi.abilities.rokushiki.SoruAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this,
                xyz.pixelatedw.mineminenomi.abilities.rokushiki.GeppoAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this,
                xyz.pixelatedw.mineminenomi.abilities.rokushiki.KamieAbility.INSTANCE);

        // ── Brawler ───────────────────────────────────────────────────────────
        MobsHelper.unlockAndEquipAbility(this,
                xyz.pixelatedw.mineminenomi.abilities.brawler.SpinningBrawlAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this,
                xyz.pixelatedw.mineminenomi.abilities.brawler.SuplexAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this,
                xyz.pixelatedw.mineminenomi.abilities.brawler.HakaiHoAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this,
                xyz.pixelatedw.mineminenomi.abilities.brawler.DamageAbsorptionAbility.INSTANCE);

        // ── Goals ─────────────────────────────────────────────────────────────
        MobsHelper.addBasicNPCGoals(this);

        // Priority 0: always-active passives + future sight
        this.goalSelector.addGoal(0, new ClimbOutOfHoleGoal(this));
        this.goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal(this, BrawlerPassiveBonusesAbility.INSTANCE));
        this.goalSelector.addGoal(0, new KenbunshokuHakiFutureSightWrapperGoal(this));
        this.goalSelector.addGoal(0, new HaoshokuHakiInfusionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiInternalDestructionWrapperGoal(this));
        goalSelector.addGoal(0, new BusoshokuHakiHardeningWrapperGoal((MobEntity) this));

        // Kamie (same as Gojo/Sukuna — priority 0)
        goalSelector.addGoal(0, new KamieWrapperGoal((MobEntity) this));

        // Priority 1: movement — SukunaGeppoGoal chases airborne targets aggressively
        goalSelector.addGoal(1, new ImprovedMeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(1, new SprintTowardsTargetGoal((MobEntity) this));
        goalSelector.addGoal(2, new SoruWrapperGoal((MobEntity) this));
        goalSelector.addGoal(2, new GeppoWrapperGoal((MobEntity) this));

        goalSelector.addGoal(2, new LuffyPistolWrapperGoal((MobEntity) this));     // switches to Star Gun in G5
        goalSelector.addGoal(2, new LuffyGatlingWrapperGoal((MobEntity) this));    // switches to Dawn Gatling in G5
        // Priority 3: heavier cooldown moves
        goalSelector.addGoal(3, new LuffyBazookaWrapperGoal((MobEntity) this));
        goalSelector.addGoal(3, new LuffyRedRocWrapperGoal((MobEntity) this));     // → Bajrang Gun at 10% HP

        // Priority 4: Gear Fifth-only attacks (blocked before awakening)
        goalSelector.addGoal(4, new LuffyDawnWhipWrapperGoal((MobEntity) this));   // G5 only
        goalSelector.addGoal(4, new LuffyKaminariWrapperGoal((MobEntity) this));   // G5 only, uses BossKaminariAbility
        goalSelector.addGoal(4, new LuffyRocketWrapperGoal((MobEntity) this));

        // Priority 2: brawler
        goalSelector.addGoal(2, new DamageAbsorptionWrapperGoal((MobEntity) this));
        goalSelector.addGoal(2, new SpinningBrawlWrapperGoal((MobEntity) this));
        goalSelector.addGoal(2, new SuplexWrapperGoal((MobEntity) this));
        goalSelector.addGoal(2, new HakaiHoWrapperGoal((MobEntity) this));

        // Priority 2: ranged gomu attacks — pistol and gatling at same priority as brawler
        // so Luffy constantly mixes punches and ranged attacks
           // Dawn Rocket in G5

        // Priority 1: Gear Fifth awakening at 50% HP — runs in canUse() every tick
        goalSelector.addGoal(1, new LuffyGearFifthWrapperGoal((MobEntity) this));
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes()
                .add(Attributes.MAX_HEALTH,           750.0)
                .add(Attributes.MOVEMENT_SPEED,       0.93)
                .add(Attributes.FOLLOW_RANGE,         250.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR,                10.0)
                .add(Attributes.ARMOR_TOUGHNESS,      4.0)
                .add(Attributes.ATTACK_DAMAGE,        5.0)
                .add(Attributes.ATTACK_KNOCKBACK,     0.0);
    }
}
