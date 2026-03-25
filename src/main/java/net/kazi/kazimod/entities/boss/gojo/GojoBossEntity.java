package net.kazi.kazimod.entities.boss;

import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.abilities.Gojo.DomainExpansionInfiniteVoidAbility;
import net.kazi.kazimod.abilities.Gojo.HollowPurpleAbility;
import net.kazi.kazimod.abilities.Gojo.InfinityAbility;
import net.kazi.kazimod.abilities.Gojo.LapseBlueAbility;
import net.kazi.kazimod.abilities.Gojo.MaxOutputLapseBlueAbility;
import net.kazi.kazimod.abilities.Gojo.RedAbility;
import net.kazi.kazimod.entities.projectiles.HollowNukeProjectile;
import net.kazi.kazimod.entities.projectiles.LapseBlueProjectile;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.util.math.vector.Vector3d;
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
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.SlamWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.brawler.TackleWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.BusoshokuHakiInternalDestructionWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.HaoshokuHakiInfusionWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.KenbunshokuHakiFutureSightWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.GeppoWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.SoruWrapperGoal;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModValues;

public class GojoBossEntity extends OPBossEntity<GojoBossEntity> {

    // ── Fight-state flags (package-private so goal classes can read them) ─

    /** True once Hollow Purple has been fired at fight start. Never resets. */
    boolean hollowPurpleFired = false;

    /** True once the near-death Hollow Nuke combo has been queued. Never resets. */
    boolean hollowNukeQueued = false;

    /** HP fraction below which the near-death Hollow Nuke combo fires once. */
    static final float NEAR_DEATH_THRESHOLD = 0.15f;

    // ── Constructors ───────────────────────────────────────────────────────

    public GojoBossEntity(final EntityType<?> type, final World world) {
        super(type, world);
    }

    public GojoBossEntity(final InProgressChallenge challenge) {
        super((EntityType) KaziEntities.GOJO_BOSS.get(), challenge);
    }

    // ── initBoss ──────────────────────────────────────────────────────────

    @Override
    public void initBoss() {
        // Identity
        this.entityStats.setFaction(ModValues.PIRATE);
        this.entityStats.setRace(ModValues.HUMAN);
        this.entityStats.setFightingStyle(ModValues.BRAWLER);

        // Attributes
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1200.0);
        this.setHealth(1200.0f);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
        this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64.0);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        this.getAttribute(Attributes.ARMOR).setBaseValue(10.0);
        this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8.0);
        this.getAttribute((Attribute) ModAttributes.TOUGHNESS.get()).setBaseValue(5.0);
        this.getAttribute((Attribute) ModAttributes.GCD.get()).setBaseValue(15.0);
        this.getAttribute((Attribute) ModAttributes.PUNCH_DAMAGE.get()).setBaseValue(8.0);
        this.getAttribute((Attribute) ModAttributes.FAUX_PROTECTION.get()).setBaseValue(14.0);
        this.getAttribute((Attribute) ModAttributes.STEP_HEIGHT.get()).setBaseValue(1.0);
        if (this.getAttribute(ForgeMod.SWIM_SPEED.get()) != null) {
            this.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(2.5);
        }

        // Haki
        this.hakiCapability.setBusoshokuHakiExp(100.0f);
        this.hakiCapability.setKenbunshokuHakiExp(100.0f);

        // Doriki
        this.entityStats.setDoriki(50000.0);

        // Equip all abilities used by custom goal classes.
        // unlockAndEquipAbility handles both unlock AND equip so getEquippedAbility()
        // works correctly inside goal canUse()/start() methods.
        MobsHelper.unlockAndEquipAbility(this, RedAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, LapseBlueAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, HollowPurpleAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, DomainExpansionInfiniteVoidAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, MaxOutputLapseBlueAbility.INSTANCE);
        // InfinityAbility: handled by AlwaysActiveAbilityWrapperGoal — do NOT
        // also call unlockAndEquipAbility here or it double-registers.

        // Goals
        MobsHelper.addBasicNPCGoals(this);
        this.goalSelector.addGoal(0, new ClimbOutOfHoleGoal((MobEntity) this));

        // Always-active passives & haki
        this.goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal<>((MobEntity) this,
                BrawlerPassiveBonusesAbility.INSTANCE));
        this.goalSelector.addGoal(0, new KenbunshokuHakiFutureSightWrapperGoal((MobEntity) this));
        this.goalSelector.addGoal(0, new HaoshokuHakiInfusionWrapperGoal((MobEntity) this));
        this.goalSelector.addGoal(0, new BusoshokuHakiInternalDestructionWrapperGoal((MobEntity) this));
        this.goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal<>((MobEntity) this,
                InfinityAbility.INSTANCE));

        // Movement
        this.goalSelector.addGoal(1, new ImprovedMeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(1, new SprintTowardsTargetGoal((MobEntity) this));
        this.goalSelector.addGoal(2, new GeppoWrapperGoal((MobEntity) this));
        this.goalSelector.addGoal(2, new SoruWrapperGoal((MobEntity) this));

        // Gojo abilities — priority 3 fires first (Hollow Purple, fight opener).
        // Priority 4 runs throughout the rest of the fight.
        // Priority 5 fills gaps with physical brawler attacks.
        this.goalSelector.addGoal(3, new GojoBossHollowPurpleGoal(this));
        this.goalSelector.addGoal(4, new GojoBossRedGoal(this));
        this.goalSelector.addGoal(4, new GojoBossLapseBlueGoal(this));
        this.goalSelector.addGoal(4, new GojoBossDomainGoal(this));
        this.goalSelector.addGoal(5, new TackleWrapperGoal((MobEntity) this));
        this.goalSelector.addGoal(5, new SlamWrapperGoal((MobEntity) this));
    }

    // ── tick ──────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) return;
        if (this.abilityData == null) return;

        LivingEntity target = this.getTarget();
        if (target == null) return;

        // Near-death Hollow Nuke: fires exactly once when HP drops below 15%.
        // After that hollowNukeQueued stays true so it never fires again.
        if (!hollowNukeQueued && this.getHealth() / this.getMaxHealth() <= NEAR_DEATH_THRESHOLD) {
            hollowNukeQueued = true;
            fireHollowNukeCombo(target);
        }
    }

    // ── Hollow Nuke combo ─────────────────────────────────────────────────

    /**
     * Fires Red and Lapse Blue simultaneously at the target.
     * LapseBlueProjectile's existing tick logic spawns the HollowNukeProjectile
     * automatically when it detects a nearby Red projectile from the same owner.
     * Called only once — when the boss drops below NEAR_DEATH_THRESHOLD.
     */
    private void fireHollowNukeCombo(LivingEntity target) {
        if (this.abilityData == null) return;
        if (HollowNukeProjectile.ACTIVE_PROJECTILES.containsKey(this.getUUID())) return;

        RedAbility       redAbility  = this.abilityData.getEquippedAbility(RedAbility.INSTANCE);
        LapseBlueAbility blueAbility = this.abilityData.getEquippedAbility(LapseBlueAbility.INSTANCE);
        if (redAbility == null || blueAbility == null) return;

        if (!(redAbility  instanceof xyz.pixelatedw.mineminenomi.api.abilities.Ability)
                || !(blueAbility instanceof xyz.pixelatedw.mineminenomi.api.abilities.Ability)) {
            KaziMod.LOGGER.warn("[GojoBossEntity] Near-death nuke: ability cast failed.");
            return;
        }

        Vector3d myPos     = this.position().add(0, 1.5, 0);
        Vector3d targetPos = target.position().add(0, 1.0, 0);
        Vector3d dir       = targetPos.subtract(myPos).normalize();

        net.kazi.kazimod.entities.projectiles.RedProjectile red =
                new net.kazi.kazimod.entities.projectiles.RedProjectile(
                        this.level, this,
                        (xyz.pixelatedw.mineminenomi.api.abilities.Ability) redAbility);
        red.setPos(myPos.x, myPos.y, myPos.z);
        red.setDeltaMovement(dir.scale(2.5));
        this.level.addFreshEntity(red);

        LapseBlueProjectile blue = new LapseBlueProjectile(
                this.level, this,
                (xyz.pixelatedw.mineminenomi.api.abilities.Ability) blueAbility);
        blue.setPos(myPos.x + dir.x * 0.5, myPos.y, myPos.z + dir.z * 0.5);
        blue.setDeltaMovement(dir.scale(2.4));
        this.level.addFreshEntity(blue);
    }

    // ── createAttributes ──────────────────────────────────────────────────

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes()
                .add(Attributes.MAX_HEALTH,           1200.0)
                .add(Attributes.MOVEMENT_SPEED,       0.32)
                .add(Attributes.FOLLOW_RANGE,         64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR,                10.0)
                .add(Attributes.ARMOR_TOUGHNESS,      8.0)
                .add(Attributes.ATTACK_DAMAGE,        8.0)
                .add(Attributes.ATTACK_KNOCKBACK,     0.0);
    }
}