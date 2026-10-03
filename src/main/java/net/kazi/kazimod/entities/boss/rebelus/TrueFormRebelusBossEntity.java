package net.kazi.kazimod.entities.boss.rebelus;

import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.ConquerorOfThreeWorldsRagnarakuWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.DestroyerOfDeathThunderBaguaWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.KundaliDragonSwarmWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.StrikingSwingWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.ThunderBaguaWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.VajraArrowWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.WhirlingMaceWrapperGoal;
import net.kazi.kazimod.abilities.TripelT.HomeRunSwingAbility;
import net.kazi.kazimod.abilities.TripelT.SahurYellAbility;
import net.kazi.kazimod.abilities.TripelT.SwingingCounterAbility;
import net.kazi.kazimod.abilities.TripelT.TripelTGodFormAbility;
import net.kazi.kazimod.abilities.TripelT.TungTungTungBarrageAbility;
import net.kazi.kazimod.entities.boss.luffy.goals.DirectAbilityGoal;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziItems2;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.inventory.EquipmentSlotType;
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
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
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
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModValues;

public class TrueFormRebelusBossEntity extends OPBossEntity<TrueFormRebelusBossEntity> {
    private static final ResourceLocation TRIPLE_T_FRUIT =
            new ResourceLocation("kazimod", "ki_ki_no_mi_model_tung_tung_tung_sahur");

    public TrueFormRebelusBossEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public TrueFormRebelusBossEntity(InProgressChallenge challenge) {
        super(KaziEntities.TRUE_FORM_REBELUS_BOSS.get(), challenge);
    }

    @Override
    public void tick() {
        if (this.isPassenger()) {
            this.stopRiding();
        }
        if (!this.level.isClientSide) {
            this.syncGodLoadout();
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
        this.entityStats.setFightingStyle(new ResourceLocation("cartaddon", "bludgeon"));

        double scalingFactor = CommonConfig.INSTANCE.getDorikiLimit() / 10000.0D;
        double scaledHp = 780.0D * scalingFactor;
        double scaledDoriki = 50000.0D * scalingFactor;

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledHp);
        this.setHealth((float) scaledHp);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.92D);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(16.0D);
        this.getAttribute(Attributes.ARMOR).setBaseValue(8.0D);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        this.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(3.0D);

        if (this.getAttribute(ModAttributes.TOUGHNESS.get()) != null) {
            this.getAttribute(ModAttributes.TOUGHNESS.get()).setBaseValue(14.0D);
        }
        if (this.getAttribute(ModAttributes.GCD.get()) != null) {
            this.getAttribute(ModAttributes.GCD.get()).setBaseValue(15.0D);
        }
        if (this.getAttribute(ModAttributes.FAUX_PROTECTION.get()) != null) {
            this.getAttribute(ModAttributes.FAUX_PROTECTION.get()).setBaseValue(12.0D);
        }
        if (this.getAttribute(ModAttributes.STEP_HEIGHT.get()) != null) {
            this.getAttribute(ModAttributes.STEP_HEIGHT.get()).setBaseValue(1.1D);
        }
        if (this.getAttribute(ForgeMod.SWIM_SPEED.get()) != null) {
            this.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(2.5D);
        }

        this.hakiCapability.setBusoshokuHakiExp(100.0F);
        this.hakiCapability.setKenbunshokuHakiExp(100.0F);
        this.hakiCapability.setHaoshokuHakiColour(0xFFD36B);
        this.entityStats.setDoriki(scaledDoriki);
        this.devilFruitData.setDevilFruit(TRIPLE_T_FRUIT);
        this.devilFruitData.setAwakenedFruit(true);
        this.setCustomName(new StringTextComponent("True Form Rebelus"));

        MobsHelper.unlockAndEquipAbility(this, TripelTGodFormAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, HomeRunSwingAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, TungTungTungBarrageAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SahurYellAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, SwingingCounterAbility.INSTANCE);
        this.syncGodLoadout();

        MobsHelper.addBasicNPCGoals(this);
        this.goalSelector.addGoal(0, new ClimbOutOfHoleGoal(this));
        this.goalSelector.addGoal(0, new KenbunshokuHakiFutureSightWrapperGoal(this));
        this.goalSelector.addGoal(0, new HaoshokuHakiInfusionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiInternalDestructionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiHardeningWrapperGoal(this));
        this.goalSelector.addGoal(0, new KamieWrapperGoal(this));

        this.goalSelector.addGoal(1, new ImprovedMeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(1, new SprintTowardsTargetGoal(this));
        this.goalSelector.addGoal(2, new SoruWrapperGoal(this));
        this.goalSelector.addGoal(2, new GeppoWrapperGoal(this));

        this.goalSelector.addGoal(2, new ThunderBaguaWrapperGoal(this));
        this.goalSelector.addGoal(2, new StrikingSwingWrapperGoal(this));
        this.goalSelector.addGoal(2, new KundaliDragonSwarmWrapperGoal(this));
        this.goalSelector.addGoal(3, new VajraArrowWrapperGoal(this));
        this.goalSelector.addGoal(3, new WhirlingMaceWrapperGoal(this));
        this.goalSelector.addGoal(3, new ConquerorOfThreeWorldsRagnarakuWrapperGoal(this));
        this.goalSelector.addGoal(3, new DestroyerOfDeathThunderBaguaWrapperGoal(this));

        this.goalSelector.addGoal(3, new RebelusCounterGoal(this));
        this.goalSelector.addGoal(4, new RebelusSeraphicWingsGoal(this));
        this.goalSelector.addGoal(4, new RebelusArrowsGoal(this));
        this.goalSelector.addGoal(4, new RebelusHomeRunGoal(this));
    }

    private void syncGodLoadout() {
        ItemStack desiredStaff = new ItemStack(KaziItems2.TRIPLE_T_STAFF.get());
        ItemStack heldStack = this.getItemBySlot(EquipmentSlotType.MAINHAND);
        if (heldStack.isEmpty() || heldStack.getItem() != desiredStaff.getItem()) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, desiredStaff);
            this.setDropChance(EquipmentSlotType.MAINHAND, 0.0F);
        }

        IAbilityData data = AbilityDataCapability.get(this);
        if (data == null) {
            return;
        }

        HomeRunSwingAbility homeRun = (HomeRunSwingAbility) data.getEquippedAbility(HomeRunSwingAbility.INSTANCE);
        if (homeRun != null) {
            homeRun.switchToAlt(this);
        }

        TungTungTungBarrageAbility barrage = (TungTungTungBarrageAbility) data.getEquippedAbility(TungTungTungBarrageAbility.INSTANCE);
        if (barrage != null) {
            barrage.switchToAlt(this);
        }

        SahurYellAbility sahurYell = (SahurYellAbility) data.getEquippedAbility(SahurYellAbility.INSTANCE);
        if (sahurYell != null) {
            sahurYell.switchToAlt(this);
        }

        SwingingCounterAbility counter = (SwingingCounterAbility) data.getEquippedAbility(SwingingCounterAbility.INSTANCE);
        if (counter != null) {
            counter.switchToJudgementation(this);
        }
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 780.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.92D)
                .add(Attributes.FOLLOW_RANGE, 250.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D);
    }

    private abstract static class RebelusAbilityGoal<A extends xyz.pixelatedw.mineminenomi.api.abilities.Ability>
            extends DirectAbilityGoal<A> {

        protected RebelusAbilityGoal(MobEntity entity, xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<A> core) {
            super(entity, core);
        }

        protected LivingEntity target() {
            return this.entity.getTarget();
        }

        protected double distanceToTarget() {
            LivingEntity target = this.target();
            return target == null ? Double.MAX_VALUE : this.entity.distanceTo(target);
        }

        protected boolean hasClearTarget() {
            LivingEntity target = this.target();
            return target != null && target.isAlive() && this.entity.getSensing().canSee(target);
        }
    }

    private static class RebelusHomeRunGoal extends RebelusAbilityGoal<HomeRunSwingAbility> {
        private RebelusHomeRunGoal(MobEntity entity) {
            super(entity, HomeRunSwingAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            double distance = this.distanceToTarget();
            return this.hasClearTarget() && distance >= 4.0D && distance <= 14.0D;
        }

        @Override
        protected boolean canContinueExtra() {
            return this.isCharging() || this.isContinuous();
        }
    }

    private static class RebelusArrowsGoal extends RebelusAbilityGoal<TungTungTungBarrageAbility> {
        private RebelusArrowsGoal(MobEntity entity) {
            super(entity, TungTungTungBarrageAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            double distance = this.distanceToTarget();
            return this.hasClearTarget() && distance >= 10.0D && distance <= 30.0D;
        }

        @Override
        protected boolean canContinueExtra() {
            return this.isCharging() || this.isContinuous();
        }
    }

    private static class RebelusSeraphicWingsGoal extends RebelusAbilityGoal<SahurYellAbility> {
        private RebelusSeraphicWingsGoal(MobEntity entity) {
            super(entity, SahurYellAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            double distance = this.distanceToTarget();
            return this.hasClearTarget() && distance >= 5.0D && distance <= 20.0D;
        }

        @Override
        protected boolean canContinueExtra() {
            return this.isContinuous();
        }
    }

    private static class RebelusCounterGoal extends RebelusAbilityGoal<SwingingCounterAbility> {
        private RebelusCounterGoal(MobEntity entity) {
            super(entity, SwingingCounterAbility.INSTANCE);
        }

        @Override
        protected boolean canUseExtra() {
            return this.hasClearTarget()
                    && this.distanceToTarget() <= 8.0D
                    && this.entity.getRandom().nextInt(20) == 0;
        }

        @Override
        protected boolean canContinueExtra() {
            return this.isContinuous();
        }
    }
}
