package net.kazi.kazimod.entities.boss.sunjinwoo;

import net.MrMagicalCart.cartaddon.init.CartWeapons;
import net.kazi.kazimod.abilities.NagiRework.SilentBoxAbility;
import net.kazi.kazimod.abilities.NagiRework.SilentDeathAbility;
import net.kazi.kazimod.abilities.NagiRework.SilentSliceAbility;
import net.kazi.kazimod.abilities.NagiRework.SilentStepAbility;
import net.kazi.kazimod.abilities.NagiRework.SilentStrideAbility;
import net.kazi.kazimod.entities.boss.luffy.goals.DirectAbilityGoal;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;
import xyz.pixelatedw.mineminenomi.api.challenges.InProgressChallenge;
import xyz.pixelatedw.mineminenomi.api.challenges.OPBossEntity;
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

public class SunJinWooBossEntity extends OPBossEntity<SunJinWooBossEntity> {
    private static final double SILENT_SLICE_MIN_DISTANCE = 6.0D;
    private static final double SILENT_SLICE_MAX_DISTANCE = 24.0D;
    private static final double SILENT_DEATH_MIN_DISTANCE = 8.0D;
    private static final double SILENT_DEATH_MAX_DISTANCE = 28.0D;
    private static final double SILENT_STEP_MAX_DISTANCE = 18.0D;
    private static final double SILENT_BOX_MAX_DISTANCE = 4.0D;
    private static final double SILENT_STRIDE_TRIGGER_DISTANCE = 12.0D;

    public SunJinWooBossEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public SunJinWooBossEntity(InProgressChallenge challenge) {
        super(KaziEntities.SUN_JIN_WOO_BOSS.get(), challenge);
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
    public boolean hurt(DamageSource source, float amount) {
        if (source.isFire()) {
            return false;
        }
        return super.hurt(source, amount);
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
        this.devilFruitData.setDevilFruit(new ResourceLocation("mineminenomi", "nagi_nagi_no_mi"));
        this.devilFruitData.setAwakenedFruit(true);

        this.setItemSlot(net.minecraft.inventory.EquipmentSlotType.MAINHAND,
                new ItemStack(CartWeapons.BARTOLOMEO_TANTO.get()));
        this.setCustomName(new StringTextComponent("Sun Jin Woo"));

        MobsHelper.unlockAndEquipAbility(this, SilentStrideAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SilentSliceAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SilentStepAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SilentBoxAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SilentDeathAbility.INSTANCE);

        MobsHelper.addBasicNPCGoals(this);

        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, MonsterEntity.class, true, true));

        this.goalSelector.addGoal(0, new ClimbOutOfHoleGoal(this));
        this.goalSelector.addGoal(0, new KenbunshokuHakiFutureSightWrapperGoal(this));
        this.goalSelector.addGoal(0, new HaoshokuHakiInfusionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiInternalDestructionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiHardeningWrapperGoal(this));
        this.goalSelector.addGoal(0, new KamieWrapperGoal(this));

        this.goalSelector.addGoal(1, new ImprovedMeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(1, new SprintTowardsTargetGoal(this));
        this.goalSelector.addGoal(2, new SoruWrapperGoal(this));
        this.goalSelector.addGoal(2, new GeppoWrapperGoal(this));

        this.goalSelector.addGoal(3, new ShiShishiSonsonWrapperGoal(this));
        this.goalSelector.addGoal(3, new SanbyakurokujuPoundHoWrapperGoal(this));
        this.goalSelector.addGoal(3, new HiryuKaenWrapperGoal(this));
        this.goalSelector.addGoal(3, new OTatsumakiWrapperGoal(this));
        this.goalSelector.addGoal(3, new YakkodoriWrapperGoal(this));

        this.goalSelector.addGoal(4, new SilentStrideGoal(this));
        this.goalSelector.addGoal(4, new SilentStepGoal(this));
        this.goalSelector.addGoal(4, new SilentBoxGoal(this));
        this.goalSelector.addGoal(5, new SilentSliceGoal(this));
        this.goalSelector.addGoal(5, new SilentDeathGoal(this));
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

    private abstract static class SunJinWooAbilityGoal<A extends xyz.pixelatedw.mineminenomi.api.abilities.Ability>
            extends DirectAbilityGoal<A> {

        protected SunJinWooAbilityGoal(MobEntity entity, xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<A> core) {
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

    private static class SilentStrideGoal extends SunJinWooAbilityGoal<SilentStrideAbility> {
        private SilentStrideGoal(MobEntity entity) {
            super(entity, SilentStrideAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            LivingEntity target = this.target();
            if (target == null) {
                return false;
            }
            double distance = this.distanceToTarget();
            double heightDifference = target.getY() - this.entity.getY();
            return distance >= SILENT_STRIDE_TRIGGER_DISTANCE || heightDifference >= 3.0D;
        }
    }

    private static class SilentSliceGoal extends SunJinWooAbilityGoal<SilentSliceAbility> {
        private SilentSliceGoal(MobEntity entity) {
            super(entity, SilentSliceAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            LivingEntity target = this.target();
            if (target == null) {
                return false;
            }
            double distance = this.distanceToTarget();
            return distance >= SILENT_SLICE_MIN_DISTANCE
                    && distance <= SILENT_SLICE_MAX_DISTANCE
                    && this.entity.getSensing().canSee(target);
        }
    }

    private static class SilentDeathGoal extends SunJinWooAbilityGoal<SilentDeathAbility> {
        private SilentDeathGoal(MobEntity entity) {
            super(entity, SilentDeathAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            LivingEntity target = this.target();
            if (target == null) {
                return false;
            }
            double distance = this.distanceToTarget();
            return distance >= SILENT_DEATH_MIN_DISTANCE
                    && distance <= SILENT_DEATH_MAX_DISTANCE
                    && this.entity.getSensing().canSee(target);
        }
    }

    private static class SilentStepGoal extends SunJinWooAbilityGoal<SilentStepAbility> {
        private SilentStepGoal(MobEntity entity) {
            super(entity, SilentStepAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            LivingEntity target = this.target();
            if (target == null) {
                return false;
            }
            return this.distanceToTarget() <= SILENT_STEP_MAX_DISTANCE && this.entity.getSensing().canSee(target);
        }
    }

    private static class SilentBoxGoal extends SunJinWooAbilityGoal<SilentBoxAbility> {
        private SilentBoxGoal(MobEntity entity) {
            super(entity, SilentBoxAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            LivingEntity target = this.target();
            if (target == null) {
                return false;
            }
            return this.distanceToTarget() <= SILENT_BOX_MAX_DISTANCE && this.entity.getSensing().canSee(target);
        }

        @Override
        protected boolean canContinueExtra() {
            return this.isContinuous();
        }
    }
}
