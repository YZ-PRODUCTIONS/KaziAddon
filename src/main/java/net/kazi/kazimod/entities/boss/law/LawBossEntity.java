package net.kazi.kazimod.entities.boss.law;

import net.MrMagicalCart.cartaddon.abilities.opeextra.CurtainAbility;
import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedInjectionShotAbility;
import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedRoomAbility;
import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedShamblesAbility;
import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedTaktAbility;
import net.MrMagicalCart.cartaddon.abilities.opeextra.TaktTossAbility;
import net.kazi.kazimod.abilities.OpeRework.KRoomAnesthesiaRework;
import net.kazi.kazimod.abilities.OpeRework.PunctureWilleRework;
import net.kazi.kazimod.abilities.OpeRework.ShockWilleRework;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
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

import java.lang.reflect.Field;
import java.util.EnumSet;

public class LawBossEntity extends OPBossEntity<LawBossEntity> {
    private static final int FULL_ROOM_SIZE = 82;
    private static final Field ROOM_SIZE_FIELD = findField("roomSize");
    private static final Field ROOM_ENTITY_FIELD = findField("roomEntity");

    private boolean phaseKRoom;
    private boolean phasePuncture;

    public LawBossEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public LawBossEntity(InProgressChallenge challenge) {
        super((EntityType) KaziEntities.LAW_BOSS.get(), challenge);
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

        this.devilFruitData.setDevilFruit(new ResourceLocation("mineminenomi", "ope_ope_no_mi"));

        double scalingFactor = CommonConfig.INSTANCE.getDorikiLimit() / 10000.0;
        double scaledHp = 760.0 * scalingFactor;
        double scaledDoriki = 50000.0 * scalingFactor;

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledHp);
        this.setHealth((float) scaledHp);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.94);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(14.0);
        this.getAttribute(Attributes.ARMOR).setBaseValue(4.0);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        this.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(2.0);

        if (this.getAttribute(ModAttributes.TOUGHNESS.get()) != null) {
            this.getAttribute(ModAttributes.TOUGHNESS.get()).setBaseValue(12.0);
        }
        if (this.getAttribute(ModAttributes.GCD.get()) != null) {
            this.getAttribute(ModAttributes.GCD.get()).setBaseValue(15.0);
        }
        if (this.getAttribute(ModAttributes.FAUX_PROTECTION.get()) != null) {
            this.getAttribute(ModAttributes.FAUX_PROTECTION.get()).setBaseValue(10.0);
        }
        if (this.getAttribute(ModAttributes.STEP_HEIGHT.get()) != null) {
            this.getAttribute(ModAttributes.STEP_HEIGHT.get()).setBaseValue(1.0);
        }
        if (this.getAttribute(ForgeMod.SWIM_SPEED.get()) != null) {
            this.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(2.5);
        }

        this.hakiCapability.setBusoshokuHakiExp(100.0f);
        this.hakiCapability.setKenbunshokuHakiExp(100.0f);
        this.entityStats.setDoriki(scaledDoriki);

        this.setItemSlot(net.minecraft.inventory.EquipmentSlotType.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));

        MobsHelper.unlockAndEquipAbility(this, ReworkedRoomAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, CurtainAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, ReworkedInjectionShotAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, ReworkedShamblesAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, ReworkedTaktAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, TaktTossAbility.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, KRoomAnesthesiaRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, ShockWilleRework.INSTANCE);
        MobsHelper.unlockAndEquipAbility(this, PunctureWilleRework.INSTANCE);

        MobsHelper.addBasicNPCGoals(this);

        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, MonsterEntity.class, true, true));

        this.goalSelector.addGoal(0, new ClimbOutOfHoleGoal(this));
        this.goalSelector.addGoal(0, new KenbunshokuHakiFutureSightWrapperGoal(this));
        this.goalSelector.addGoal(0, new HaoshokuHakiInfusionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiInternalDestructionWrapperGoal(this));
        this.goalSelector.addGoal(0, new BusoshokuHakiHardeningWrapperGoal(this));
        this.goalSelector.addGoal(0, new KamieWrapperGoal(this));

        this.goalSelector.addGoal(1, new SprintTowardsTargetGoal(this));
        this.goalSelector.addGoal(1, new ImprovedMeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(2, new SoruWrapperGoal(this));
        this.goalSelector.addGoal(2, new GeppoWrapperGoal(this));

        // Swordsman trainer-style pressure goals.
        this.goalSelector.addGoal(3, new ShiShishiSonsonWrapperGoal(this));
        this.goalSelector.addGoal(3, new SanbyakurokujuPoundHoWrapperGoal(this));
        this.goalSelector.addGoal(3, new HiryuKaenWrapperGoal(this));
        this.goalSelector.addGoal(3, new OTatsumakiWrapperGoal(this));
        this.goalSelector.addGoal(3, new YakkodoriWrapperGoal(this));

        // Ope phase controller goals.
        this.goalSelector.addGoal(1, new RoomOpenGoal(this));
        this.goalSelector.addGoal(4, new SimpleAbilityGoal<>(this, CurtainAbility.INSTANCE, 1.0f, 0.40f));
        this.goalSelector.addGoal(4, new SimpleAbilityGoal<>(this, ReworkedInjectionShotAbility.INSTANCE, 1.0f, 0.40f));
        this.goalSelector.addGoal(4, new SimpleAbilityGoal<>(this, ReworkedShamblesAbility.INSTANCE, 1.0f, 0.40f));
        this.goalSelector.addGoal(4, new SimpleAbilityGoal<>(this, ReworkedTaktAbility.INSTANCE, 1.0f, 0.40f));
        this.goalSelector.addGoal(4, new SimpleAbilityGoal<>(this, TaktTossAbility.INSTANCE, 1.0f, 0.40f));
        this.goalSelector.addGoal(2, new EnsureKRoomGoal(this));
        this.goalSelector.addGoal(5, new ShockWilleGoal(this));
        this.goalSelector.addGoal(5, new PunctureWilleGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide || this.abilityData == null) {
            return;
        }

        float hpPercent = this.getHealth() / this.getMaxHealth();
        if (!this.phaseKRoom && hpPercent <= 0.40f) {
            this.phaseKRoom = true;
            this.closeRoom();
            this.enableKRoom();
        }
        if (!this.phasePuncture && hpPercent <= 0.20f) {
            this.phasePuncture = true;
            this.enableKRoom();
        }

        if (!this.phaseKRoom) {
            this.ensureMaxRoomSize();
        }
    }

    private void closeRoom() {
        ReworkedRoomAbility room = this.getAbility(ReworkedRoomAbility.INSTANCE);
        if (room != null && room.isContinuous()) {
            room.use(this);
        }
    }

    private void enableKRoom() {
        KRoomAnesthesiaRework kRoom = this.getAbility(KRoomAnesthesiaRework.INSTANCE);
        if (kRoom != null && !kRoom.isContinuous()) {
            kRoom.use(this);
        }
    }

    private boolean isRoomActive() {
        ReworkedRoomAbility room = this.getAbility(ReworkedRoomAbility.INSTANCE);
        return room != null && room.isContinuous();
    }

    private void ensureMaxRoomSize() {
        ReworkedRoomAbility room = this.getAbility(ReworkedRoomAbility.INSTANCE);
        if (room == null || !room.isContinuous()) {
            return;
        }

        try {
            if (ROOM_SIZE_FIELD != null) {
                ROOM_SIZE_FIELD.setInt(room, FULL_ROOM_SIZE);
            }
            if (ROOM_ENTITY_FIELD != null) {
                Object roomEntity = ROOM_ENTITY_FIELD.get(room);
                if (roomEntity instanceof xyz.pixelatedw.mineminenomi.entities.SphereEntity) {
                    ((xyz.pixelatedw.mineminenomi.entities.SphereEntity) roomEntity).setRadius(FULL_ROOM_SIZE);
                }
            }
        } catch (IllegalAccessException ignored) {
        }
    }

    private boolean isKRoomActive() {
        KRoomAnesthesiaRework kRoom = this.getAbility(KRoomAnesthesiaRework.INSTANCE);
        return kRoom != null && kRoom.isContinuous();
    }

    public float getHpPercent() {
        return this.getMaxHealth() <= 0.0f ? 1.0f : this.getHealth() / this.getMaxHealth();
    }

    public boolean isPhaseKRoom() {
        return this.phaseKRoom;
    }

    public boolean isPhasePuncture() {
        return this.phasePuncture;
    }

    @SuppressWarnings("unchecked")
    private <T extends Ability> T getAbility(AbilityCore<T> core) {
        if (this.abilityData == null) {
            return null;
        }
        return (T) this.abilityData.getEquippedAbility(core);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 760.0)
                .add(Attributes.MOVEMENT_SPEED, 0.94)
                .add(Attributes.FOLLOW_RANGE, 250.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0);
    }

    private static Field findField(String name) {
        try {
            Field field = ReworkedRoomAbility.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static abstract class BaseAbilityGoal<A extends Ability> extends Goal {
        protected final LawBossEntity boss;
        private final AbilityCore<A> core;

        protected BaseAbilityGoal(LawBossEntity boss, AbilityCore<A> core) {
            this.boss = boss;
            this.core = core;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @SuppressWarnings("unchecked")
        protected A getAbility() {
            if (this.boss.abilityData == null) {
                return null;
            }
            return (A) this.boss.abilityData.getEquippedAbility(this.core);
        }

        protected boolean isOnCooldown(A ability) {
            return ability.getComponent(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.COOLDOWN)
                    .map(c -> c.isOnCooldown())
                    .orElse(false);
        }

        protected boolean hasTarget() {
            return GoalUtil.hasAliveTarget(this.boss);
        }

        protected void lookAtTarget() {
            if (this.boss.getTarget() != null && this.boss.getTarget().isAlive()) {
                GoalUtil.lookAtEntity(this.boss, this.boss.getTarget());
            }
        }
    }

    private static class RoomOpenGoal extends BaseAbilityGoal<ReworkedRoomAbility> {

        private RoomOpenGoal(LawBossEntity boss) {
            super(boss, ReworkedRoomAbility.INSTANCE);
        }

        @Override
        public boolean canUse() {
            if (!this.hasTarget()) {
                return false;
            }
            if (this.boss.isPhaseKRoom()) {
                return false;
            }
            ReworkedRoomAbility ability = this.getAbility();
            if (ability == null) {
                return false;
            }
            if (ability.isContinuous()) {
                return false;
            }
            return !this.isOnCooldown(ability);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.lookAtTarget();
            ReworkedRoomAbility ability = this.getAbility();
            if (ability != null) {
                ability.use(this.boss);
            }
        }
    }

    private static class EnsureKRoomGoal extends BaseAbilityGoal<KRoomAnesthesiaRework> {

        private EnsureKRoomGoal(LawBossEntity boss) {
            super(boss, KRoomAnesthesiaRework.INSTANCE);
        }

        @Override
        public boolean canUse() {
            if (!this.hasTarget()) {
                return false;
            }
            if (!this.boss.isPhaseKRoom()) {
                return false;
            }
            if (this.boss.isKRoomActive()) {
                return false;
            }
            KRoomAnesthesiaRework ability = this.getAbility();
            return ability != null && !this.isOnCooldown(ability);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.lookAtTarget();
            KRoomAnesthesiaRework ability = this.getAbility();
            if (ability != null) {
                ability.use(this.boss);
            }
        }
    }

    private static class SimpleAbilityGoal<A extends Ability> extends BaseAbilityGoal<A> {
        private final float maxHpPct;
        private final float minHpPct;

        private SimpleAbilityGoal(LawBossEntity boss, AbilityCore<A> core, float maxHpPct, float minHpPct) {
            super(boss, core);
            this.maxHpPct = maxHpPct;
            this.minHpPct = minHpPct;
        }

        @Override
        public boolean canUse() {
            if (!this.hasTarget()) {
                return false;
            }
            float hp = this.boss.getHpPercent();
            if (hp > this.maxHpPct || hp <= this.minHpPct) {
                return false;
            }
            if (!this.boss.isRoomActive()) {
                return false;
            }
            A ability = this.getAbility();
            return ability != null && !this.isOnCooldown(ability);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.lookAtTarget();
            A ability = this.getAbility();
            if (ability != null) {
                ability.use(this.boss);
            }
        }
    }

    private static class ShockWilleGoal extends BaseAbilityGoal<ShockWilleRework> {

        private ShockWilleGoal(LawBossEntity boss) {
            super(boss, ShockWilleRework.INSTANCE);
        }

        @Override
        public boolean canUse() {
            if (!this.hasTarget()) {
                return false;
            }
            float hp = this.boss.getHpPercent();
            if (hp > 0.40f || hp <= 0.20f) {
                return false;
            }
            if (!this.boss.isKRoomActive()) {
                return false;
            }
            ShockWilleRework ability = this.getAbility();
            if (ability == null || ability.isBusy()) {
                return false;
            }
            return !this.isOnCooldown(ability);
        }

        @Override
        public boolean canContinueToUse() {
            ShockWilleRework ability = this.getAbility();
            return ability != null && ability.isBusy();
        }

        @Override
        public void start() {
            this.lookAtTarget();
            ShockWilleRework ability = this.getAbility();
            if (ability != null) {
                ability.use(this.boss);
            }
        }
    }

    private static class PunctureWilleGoal extends BaseAbilityGoal<PunctureWilleRework> {

        private PunctureWilleGoal(LawBossEntity boss) {
            super(boss, PunctureWilleRework.INSTANCE);
        }

        @Override
        public boolean canUse() {
            if (!this.hasTarget()) {
                return false;
            }
            if (this.boss.getHpPercent() > 0.20f) {
                return false;
            }
            if (!this.boss.isKRoomActive()) {
                return false;
            }
            PunctureWilleRework ability = this.getAbility();
            if (ability == null || ability.isBusy()) {
                return false;
            }
            return !this.isOnCooldown(ability);
        }

        @Override
        public boolean canContinueToUse() {
            PunctureWilleRework ability = this.getAbility();
            return ability != null && ability.isBusy();
        }

        @Override
        public void start() {
            this.lookAtTarget();
            PunctureWilleRework ability = this.getAbility();
            if (ability != null) {
                ability.use(this.boss);
            }
        }
    }
}
