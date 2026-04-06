package net.kazi.kazimod.entities.boss.aizen;

import net.kazi.kazimod.abilities.Kyoka.GoryutenmetsuAbility;
import net.kazi.kazimod.abilities.Kyoka.IllusionCloneBarrageAbility;
import net.kazi.kazimod.abilities.Kyoka.IllusionCounterAbility;
import net.kazi.kazimod.abilities.Kyoka.InvisibleExecutionAbility;
import net.kazi.kazimod.abilities.Kyoka.KanzenSaiminAbility;
import net.kazi.kazimod.abilities.Kyoka.KurohitsugiAbility;
import net.kazi.kazimod.entities.boss.luffy.goals.DirectAbilityGoal;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;
import xyz.pixelatedw.mineminenomi.api.challenges.InProgressChallenge;
import xyz.pixelatedw.mineminenomi.api.challenges.OPBossEntity;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.api.helpers.MobsHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.ClimbOutOfHoleGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.ImprovedMeleeAttackGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.SprintTowardsTargetGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.BusoshokuHakiHardeningWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.BusoshokuHakiInternalDestructionWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.HaoshokuHakiInfusionWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.haki.KenbunshokuHakiFutureSightWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.GeppoWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.KamieWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.rokushiki.SoruWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.swordsman.HiryuKaenWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.swordsman.OTatsumakiWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.swordsman.SanbyakurokujuPoundHoWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.swordsman.ShiShishiSonsonWrapperGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.swordsman.YakkodoriWrapperGoal;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModValues;

import java.util.EnumSet;

public class AizenBossEntity extends OPBossEntity<AizenBossEntity> {
    private static final float KUROHITSUGI_FIRST_THRESHOLD = 0.70F;
    private static final float KUROHITSUGI_SECOND_THRESHOLD = 0.40F;
    private static final float GORYUTENMETSU_THRESHOLD = 0.20F;

    private int pendingKurohitsugiUses;
    private boolean queuedFirstKurohitsugi;
    private boolean queuedSecondKurohitsugi;
    private boolean queuedGoryutenmetsu;
    private boolean pendingGoryutenmetsu;

    public AizenBossEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public AizenBossEntity(InProgressChallenge challenge) {
        super(KaziEntities.AIZEN_BOSS.get(), challenge);
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
    public boolean causeFallDamage(float distance, float multiplier) {
        return false;
    }

    @Override
    public void initBoss() {
        this.entityStats.setFaction(ModValues.PIRATE);
        this.entityStats.setRace(ModValues.HUMAN);
        this.entityStats.setFightingStyle(ModValues.SWORDSMAN);

        double scalingFactor = CommonConfig.INSTANCE.getDorikiLimit() / 10000.0D;
        double scaledHp = 760.0D * scalingFactor;
        double scaledDoriki = 50000.0D * scalingFactor;

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledHp);
        this.setHealth((float) scaledHp);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.94D);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(14.0D);
        this.getAttribute(Attributes.ARMOR).setBaseValue(4.0D);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        this.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(2.0D);

        if (this.getAttribute(ModAttributes.TOUGHNESS.get()) != null) {
            this.getAttribute(ModAttributes.TOUGHNESS.get()).setBaseValue(12.0D);
        }
        if (this.getAttribute(ModAttributes.GCD.get()) != null) {
            this.getAttribute(ModAttributes.GCD.get()).setBaseValue(15.0D);
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
        this.devilFruitData.setDevilFruit(new ResourceLocation("kazimod", "kyoka_kyoka_no_mi"));
        this.devilFruitData.setAwakenedFruit(true);

        this.setItemSlot(net.minecraft.inventory.EquipmentSlotType.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));

        MobsHelper.unlockAndEquipAbility(this, KanzenSaiminAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, KurohitsugiAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, GoryutenmetsuAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, IllusionCloneBarrageAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, InvisibleExecutionAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, IllusionCounterAbility.INSTANCE);

        MobsHelper.addBasicNPCGoals(this);

        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, MonsterEntity.class, true, true));

        this.goalSelector.addGoal(0, new ClimbOutOfHoleGoal(this));
        this.goalSelector.addGoal(0, new KenbunshokuHakiFutureSightWrapperGoal(this));
        this.goalSelector.addGoal(0, new HaoshokuHakiInfusionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiInternalDestructionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiHardeningWrapperGoal(this));
        this.goalSelector.addGoal(0, new KamieWrapperGoal(this));

        this.goalSelector.addGoal(1, new ThresholdKurohitsugiGoal(this));
        this.goalSelector.addGoal(1, new ThresholdGoryutenmetsuGoal(this));
        this.goalSelector.addGoal(1, new ImprovedMeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(1, new SprintTowardsTargetGoal(this));
        this.goalSelector.addGoal(2, new SoruWrapperGoal(this));
        this.goalSelector.addGoal(2, new GeppoWrapperGoal(this));

        this.goalSelector.addGoal(3, new ShiShishiSonsonWrapperGoal(this));
        this.goalSelector.addGoal(3, new SanbyakurokujuPoundHoWrapperGoal(this));
        this.goalSelector.addGoal(3, new HiryuKaenWrapperGoal(this));
        this.goalSelector.addGoal(3, new OTatsumakiWrapperGoal(this));
        this.goalSelector.addGoal(3, new YakkodoriWrapperGoal(this));

        this.goalSelector.addGoal(4, new SimpleKyokaGoal<>(this, KanzenSaiminAbility.INSTANCE, 2.5D, 18.0D, 5));
        this.goalSelector.addGoal(4, new SimpleKyokaGoal<>(this, IllusionCloneBarrageAbility.INSTANCE, 3.0D, 20.0D, 6));
        this.goalSelector.addGoal(4, new SimpleKyokaGoal<>(this, InvisibleExecutionAbility.INSTANCE, 0.0D, 16.0D, 7));
        this.goalSelector.addGoal(4, new SimpleKyokaGoal<>(this, IllusionCounterAbility.INSTANCE, 0.0D, 14.0D, 8));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide || this.abilityData == null) {
            return;
        }

        float hpPercent = this.getMaxHealth() <= 0.0F ? 1.0F : this.getHealth() / this.getMaxHealth();
        if (!this.queuedFirstKurohitsugi && hpPercent <= KUROHITSUGI_FIRST_THRESHOLD) {
            this.queuedFirstKurohitsugi = true;
            this.pendingKurohitsugiUses++;
        }
        if (!this.queuedSecondKurohitsugi && hpPercent <= KUROHITSUGI_SECOND_THRESHOLD) {
            this.queuedSecondKurohitsugi = true;
            this.pendingKurohitsugiUses++;
        }
        if (!this.queuedGoryutenmetsu && hpPercent <= GORYUTENMETSU_THRESHOLD) {
            this.queuedGoryutenmetsu = true;
            this.pendingGoryutenmetsu = true;
        }
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 760.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.94D)
                .add(Attributes.FOLLOW_RANGE, 250.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D);
    }

    private static class SimpleKyokaGoal<A extends xyz.pixelatedw.mineminenomi.api.abilities.Ability> extends DirectAbilityGoal<A> {
        private final double minDistance;
        private final double maxDistance;
        private final int randomInterval;

        private SimpleKyokaGoal(MobEntity entity, xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<A> core,
                                double minDistance, double maxDistance, int randomInterval) {
            super(entity, core);
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
            this.randomInterval = Math.max(1, randomInterval);
        }

        @Override
        protected boolean canUseExtra() {
            LivingEntity target = this.entity.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }

            double distance = this.entity.distanceTo(target);
            if (distance < this.minDistance || distance > this.maxDistance) {
                return false;
            }

            return this.entity.getRandom().nextInt(this.randomInterval) == 0;
        }

        @Override
        protected boolean canContinueExtra() {
            return this.isCharging() || this.isContinuous();
        }
    }

    private abstract static class ThresholdAbilityGoal<A extends xyz.pixelatedw.mineminenomi.api.abilities.Ability> extends DirectAbilityGoal<A> {
        protected final AizenBossEntity boss;

        private ThresholdAbilityGoal(AizenBossEntity boss, xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<A> core) {
            super(boss, core);
            this.boss = boss;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE));
        }

        @Override
        protected boolean canContinueExtra() {
            return this.isCharging() || this.isContinuous();
        }

        protected boolean hasValidTarget() {
            return GoalUtil.hasAliveTarget(this.boss);
        }
    }

    private static class ThresholdKurohitsugiGoal extends ThresholdAbilityGoal<KurohitsugiAbility> {
        private ThresholdKurohitsugiGoal(AizenBossEntity boss) {
            super(boss, KurohitsugiAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            return this.boss.pendingKurohitsugiUses > 0 && this.hasValidTarget();
        }

        @Override
        public void start() {
            super.start();
            if (this.boss.pendingKurohitsugiUses > 0) {
                this.boss.pendingKurohitsugiUses--;
            }
        }
    }

    private static class ThresholdGoryutenmetsuGoal extends ThresholdAbilityGoal<GoryutenmetsuAbility> {
        private ThresholdGoryutenmetsuGoal(AizenBossEntity boss) {
            super(boss, GoryutenmetsuAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            return this.boss.pendingGoryutenmetsu && this.hasValidTarget();
        }

        @Override
        public void start() {
            super.start();
            this.boss.pendingGoryutenmetsu = false;
        }
    }
}
