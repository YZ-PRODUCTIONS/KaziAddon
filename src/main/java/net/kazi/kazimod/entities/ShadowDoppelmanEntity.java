package net.kazi.kazimod.entities;

import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.blacklegextra.CartBienCuitGrillShotWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.blacklegextra.CartPartyTableKickCourseWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.ThunderBaguaWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.StrikingSwingWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.VajraArrowWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.WhirlingMaceWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.ConquerorOfThreeWorldsRagnarakuWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.DestroyerOfDeathThunderBaguaWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.bludgeon.KundaliDragonSwarmWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.brawlerextra.FistsOfLoveBarrageWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.dagger.QuickstabWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.dagger.WhirlingFangWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.doubleaxe.TyrantCleaveWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.electroextra.EleclawWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.electroextra.SulongWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.greatblade.HeavySwingWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.greatblade.ThousandSlicesWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.hasshoken.ButoKaitenWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.hasshoken.ButoWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.lunarian.FlamesOnWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.lunarian.KaryudonWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.nitoryu.NitoryuIaiRashmonWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.nitoryu.SaiKuruWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.oni.SkullBasherWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.oni.ViciousRoarWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.ryusoken.RyuNoIbukiWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.ryusoken.RyuNoKagizumeWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.ryusoken.TalonRushWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.saber.DivineDepartureWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.saber.WildFuryWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.santoryu.OniGiriWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.santoryu.ToraGariWrapperGoal;
import net.MrMagicalCart.cartaddon.entities.mobs.goals.abilities.skyfolk.AxeDialWrapperGoal;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.kazi.kazimod.abilities.KageRework.DoppelmanRework;
import net.kazi.kazimod.abilities.SpearRework.DrillJabRework;
import net.kazi.kazimod.abilities.SpearRework.VaultRework;
import net.kazi.kazimod.abilities.swordsmanrework.HiryuKaenRework;
import net.kazi.kazimod.abilities.swordsmanrework.OTatsumakiRework;
import net.kazi.kazimod.abilities.swordsmanrework.SanbyakurokujoPoundHoRework;
import net.kazi.kazimod.abilities.swordsmanrework.ShiShishiSonsonRework;
import net.kazi.kazimod.abilities.swordsmanrework.YakkodoriRework;
import net.kazi.kazimod.entities.goals.ShadowCopiedAbilityGoal;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziItems2;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.OpenDoorGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.abilities.CommandAbility;
import xyz.pixelatedw.mineminenomi.abilities.haki.BusoshokuHakiHardeningAbility;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.GeppoAbility;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.KamieAbility;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.SoruAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCoreUnlockWrapper;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.entities.TrainerEntity;
import xyz.pixelatedw.mineminenomi.api.entities.ICommandReceiver;
import xyz.pixelatedw.mineminenomi.api.enums.NPCCommand;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.MobsHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.entities.mobs.ability.ITamableEntity;
import xyz.pixelatedw.mineminenomi.entities.mobs.ability.NightmareSoldierEntity;
import xyz.pixelatedw.mineminenomi.entities.mobs.bandits.AbstractBanditEntity;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.FactionHurtByTargetGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.ImprovedMeleeAttackGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.SprintTowardsTargetGoal;
import xyz.pixelatedw.mineminenomi.entities.mobs.goals.abilities.AlwaysActiveAbilityWrapperGoal;
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
import xyz.pixelatedw.mineminenomi.entities.mobs.marines.AbstractMarineEntity;
import xyz.pixelatedw.mineminenomi.entities.mobs.pirates.AbstractPirateEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModItems;
import xyz.pixelatedw.mineminenomi.init.ModValues;
import xyz.pixelatedw.mineminenomi.init.ModWeapons;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class ShadowDoppelmanEntity extends CreatureEntity implements ICommandReceiver, IEntityAdditionalSpawnData, ITamableEntity {

    private static final UUID BLACK_BOX_COOLDOWN_BONUS_UUID = UUID.fromString("8459d1b9-2362-4c63-b89c-af761a6c9f18");
    private static final DataParameter<Integer> SHADOWS =
            EntityDataManager.defineId(ShadowDoppelmanEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> PLAYER_ILLUSION =
            EntityDataManager.defineId(ShadowDoppelmanEntity.class, DataSerializers.BOOLEAN);
    private static final Set<String> KAGE_COPY_PATHS = new HashSet<>(Arrays.asList(
            "black_box",
            "brick_bat",
            "kage_giri",
            "kage_kakumei",
            "nightmare_soldiers",
            "shadows_asgard",
            "tsuno_tokage"
    ));
    private static final Set<String> FRUITLESS_ABILITY_PATHS = new HashSet<>(Arrays.asList(
            "galaxy_impact",
            "futenraku",
            "yasotakeru",
            "absolute_pierce",
            "sky_splitter_descent",
            "foxfire_style",
            "radiant_slice"
    ));
    private static final Set<String> CLONE_SUMMON_ABILITY_PATHS = new HashSet<>(Arrays.asList(
            "illusion_clone_barrage",
            "invisible_execution",
            "illusion_counter"
    ));
    private static final Set<String> KYOKA_ABILITY_PATHS = new HashSet<>(Arrays.asList(
            "kanzen_saimin",
            "hado_90_kurohitsugi",
            "kurohitsugi",
            "hado_99_goryutenmetsu",
            "goryutenmetsu",
            "illusion_clone_barrage",
            "invisible_execution",
            "illusion_counter"
    ));

    @Nullable
    private UUID ownerId;
    @Nullable
    private LivingEntity owner;
    private long lastCommandTime;
    private LivingEntity lastCommandSender;
    private NPCCommand currentCommand = NPCCommand.IDLE;
    private boolean deathTriggerHandled;
    private int aggressiveAbilityCooldown;
    private int aggressiveSoruCooldown;
    private int aggressiveGeppoCooldown;

    public ShadowDoppelmanEntity(EntityType<? extends ShadowDoppelmanEntity> type, World world) {
        super(type, world);
    }

    public ShadowDoppelmanEntity(World world, LivingEntity owner) {
        this(KaziEntities.SHADOW_DOPPELMAN.get(), world);
        if (world != null && !world.isClientSide) {
            initFromOwner(owner);
        }
    }

    private void initFromOwner(LivingEntity owner) {
        this.setOwner(owner);
        this.deathTriggerHandled = false;
        this.aggressiveAbilityCooldown = 0;
        this.aggressiveSoruCooldown = 0;
        this.aggressiveGeppoCooldown = 0;
        IEntityStats ownerStats = EntityStatsCapability.get(owner);
        IEntityStats myStats = EntityStatsCapability.get(this);
        myStats.setHeart(false);
        myStats.setShadow(true);
        myStats.setFaction(ownerStats.getFaction());
        myStats.setRace(ownerStats.getRace());
        myStats.setFightingStyle(ownerStats.getFightingStyle());

        IHakiData ownerHaki = HakiDataCapability.get(owner);
        IHakiData myHaki = HakiDataCapability.get(this);
        myHaki.setBusoshokuHakiExp(ownerHaki.getBusoshokuHakiExp());
        myHaki.setKenbunshokuHakiExp(ownerHaki.getKenbunshokuHakiExp());
        myHaki.setHaoshokuHakiColour(ownerHaki.getHaoshokuHakiColour());

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                Math.max(200.0, owner.getMaxHealth() * 1.0));
        this.setHealth(this.getMaxHealth());
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                Math.max(18.0, owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.35));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                Math.max(0.3, owner.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.9));
        this.getAttribute(Attributes.ARMOR).setBaseValue(18.0);
        this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(12.0);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64.0);
        Attribute toughness = ModAttributes.TOUGHNESS.get();
        if (this.getAttribute(toughness) != null) {
            this.getAttribute(toughness).setBaseValue(10.0);
        }
        Attribute stepHeight = ModAttributes.STEP_HEIGHT.get();
        if (this.getAttribute(stepHeight) != null) {
            this.getAttribute(stepHeight).setBaseValue(2.0);
        }

        ((GroundPathNavigator) this.getNavigation()).setCanOpenDoors(true);

        copyOwnerAbilities(owner);
        addBossStyleGoals(owner, ownerStats);
        applyStyleLoadout(ownerStats.getFightingStyle());
    }

    @Override
    protected void registerGoals() {
        CommandAbility.addCommandGoals(this);
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(0, new OpenDoorGoal(this, false));
        this.goalSelector.addGoal(1, new ImprovedMeleeAttackGoal(this, 1.3, true).setAttackInterval(12));
        this.goalSelector.addGoal(1, new SprintTowardsTargetGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomWalkingGoal(this, 0.9));
        this.goalSelector.addGoal(5, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(5, new LookAtGoal(this, AbstractMarineEntity.class, 8.0F));
        this.goalSelector.addGoal(5, new LookAtGoal(this, AbstractPirateEntity.class, 8.0F));
        this.goalSelector.addGoal(5, new LookAtGoal(this, AbstractBanditEntity.class, 8.0F));
        this.goalSelector.addGoal(5, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes()
                .add(Attributes.FOLLOW_RANGE, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.17)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 18.0)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SHADOWS, 0);
        this.entityData.define(PLAYER_ILLUSION, false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity() != this.getOwner()) {
            amount *= 0.55F;
        }
        return source.getEntity() instanceof PlayerEntity && source.getEntity() == this.getOwner()
                ? false
                : super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) + this.getShadows() * 4.0F;
        int knockback = 0;
        if (target instanceof LivingEntity) {
            damage += EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity) target).getMobType());
            knockback += EnchantmentHelper.getKnockbackBonus(this);
        }

        boolean flag = target.hurt(DamageSource.mobAttack(this), damage);
        if (flag && knockback > 0) {
            target.push(
                    -MathHelper.sin(this.yRot * ((float) Math.PI / 180F)) * knockback * 0.5D,
                    0.1D,
                    MathHelper.cos(this.yRot * ((float) Math.PI / 180F)) * knockback * 0.5D
            );
            AbilityHelper.setDeltaMovement(this, this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
        }

        return flag;
    }

    @Override
    public void aiStep() {
        this.updateSwingTime();
        super.aiStep();
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide) {
            LivingEntity currentOwner = this.getOwner();
            if (currentOwner == null) {
                this.remove();
                return;
            }

            if (this.currentCommand == NPCCommand.ATTACK || this.currentCommand == NPCCommand.GUARD || this.currentCommand == NPCCommand.IDLE) {
                LivingEntity target = getOwnerCommandTarget(currentOwner);
                if (isValidHostileTarget(target)) {
                    this.setTarget(target);
                }
            }

            if (!isValidHostileTarget(this.getTarget())) {
                this.setTarget(null);
            }

            tickMovementAbilities();
            keepPressureOnTarget();
            tickAggressiveAbilityUsage();

            if (this.getHealth() <= 0.0F && !this.deathTriggerHandled) {
                this.deathTriggerHandled = true;
                AbilityDataCapability.getLazy(currentOwner).ifPresent(props -> {
                    DoppelmanRework abl = (DoppelmanRework) props.getEquippedAbility(DoppelmanRework.INSTANCE);
                    if (abl != null) {
                        abl.doppelmanDeathTrigger(currentOwner);
                    }
                });
            }
        }

        super.tick();
    }

    private void tickMovementAbilities() {
        if (this.aggressiveSoruCooldown > 0) {
            this.aggressiveSoruCooldown--;
        }
        if (this.aggressiveGeppoCooldown > 0) {
            this.aggressiveGeppoCooldown--;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        double distanceSqr = this.distanceToSqr(target);
        double heightDiff = target.getY() - this.getY();

        if (this.aggressiveGeppoCooldown <= 0
                && this.isOnGround()
                && canUseMovementAbility(GeppoAbility.INSTANCE)
                && (heightDiff > 3.0D
                || (!target.isOnGround() && heightDiff > 1.0D)
                || (heightDiff > 1.5D && distanceSqr < 100.0D))) {
            Ability geppo = (Ability) AbilityDataCapability.get(this).getEquippedAbility(GeppoAbility.INSTANCE);
            if (geppo != null) {
                Vector3d dir = target.position().subtract(this.position());
                if (dir.lengthSqr() <= 0.001D) {
                    dir = new Vector3d(0.0D, 1.0D, 0.0D);
                } else {
                    dir = dir.normalize();
                }
                geppo.use(this);
                this.setDeltaMovement(dir.x * 1.5D, 1.2D, dir.z * 1.5D);
                this.aggressiveGeppoCooldown = 30;
                return;
            }
        }

        if (this.aggressiveSoruCooldown <= 0
                && canUseMovementAbility(SoruAbility.INSTANCE)
                && (distanceSqr > 64.0D || (distanceSqr > 25.0D && !this.canSee(target)))) {
            Ability soru = (Ability) AbilityDataCapability.get(this).getEquippedAbility(SoruAbility.INSTANCE);
            if (soru != null) {
                Vector3d dir = target.position().subtract(this.position());
                Vector3d flatDir = new Vector3d(dir.x, 0.0D, dir.z);
                if (flatDir.lengthSqr() > 0.001D) {
                    flatDir = flatDir.normalize();
                    soru.use(this);
                    this.setDeltaMovement(flatDir.x * 2.4D, this.getDeltaMovement().y, flatDir.z * 2.4D);
                    this.aggressiveSoruCooldown = 18;
                }
            }
        }
    }

    private boolean canUseMovementAbility(AbilityCore<?> core) {
        IAbilityData data = AbilityDataCapability.get(this);
        if (data == null) {
            return false;
        }
        IAbility raw = data.getEquippedAbility(core);
        if (!(raw instanceof Ability)) {
            return false;
        }
        Ability ability = (Ability) raw;
        return !ability.getComponent(ModAbilityKeys.COOLDOWN)
                .map(c -> ((xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent) c).isOnCooldown())
                .orElse(false);
    }

    private void keepPressureOnTarget() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.yHeadRot = this.yRot;

        double distanceSqr = this.distanceToSqr(target);
        if (distanceSqr > 9.0D) {
            this.getNavigation().moveTo(target, 1.35D);
        } else {
            this.getNavigation().stop();
        }

        if (this.tickCount % 10 == 0 && distanceSqr > 25.0D) {
            this.setSprinting(true);
        }

        double attackReachSqr = 12.25D;
        if (distanceSqr <= attackReachSqr && this.canSee(target) && this.tickCount % 16 == 0) {
            this.swing(Hand.MAIN_HAND);
            this.doHurtTarget(target);
        }
    }

    private void tickAggressiveAbilityUsage() {
        if (this.aggressiveAbilityCooldown > 0) {
            this.aggressiveAbilityCooldown--;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.aggressiveAbilityCooldown > 0) {
            return;
        }

        IAbilityData data = AbilityDataCapability.get(this);
        if (data == null) {
            return;
        }

        double distance = this.distanceTo(target);

        for (IAbility rawAbility : data.getRawEquippedAbilities()) {
            if (!(rawAbility instanceof Ability)) {
                continue;
            }
            Ability ability = (Ability) rawAbility;
            AbilityCore<?> core = ability.getCore();
            if (core == null || shouldSkipCopiedAbility(core)) {
                continue;
            }

            AbilityCategory category = core.getCategory();
            if (core.getType() != AbilityType.ACTION
                    && category != AbilityCategory.HAKI
                    && !KAGE_COPY_PATHS.contains(core.getKey().getPath())) {
                continue;
            }

            if (!isReasonableRangeForAbility(category, distance)) {
                continue;
            }

            try {
                boolean onCooldown = ability.getComponent(ModAbilityKeys.COOLDOWN)
                        .map(c -> ((xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent) c).isOnCooldown())
                        .orElse(false);
                if (onCooldown) {
                    continue;
                }

                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                ability.use(this);
                this.aggressiveAbilityCooldown = category == AbilityCategory.HAKI ? 16 : 6;
                break;
            } catch (Exception ignored) {
                // Some player-oriented abilities are noisy on NPCs; wrappers still handle those.
            }
        }
    }

    private boolean isReasonableRangeForAbility(AbilityCategory category, double distance) {
        if (category == AbilityCategory.HAKI) {
            return distance <= 18.0D;
        }
        if (category == AbilityCategory.RACIAL) {
            return distance <= 24.0D;
        }
        if (category == AbilityCategory.STYLE) {
            return distance <= 20.0D;
        }
        if (category == AbilityCategory.DEVIL_FRUITS) {
            return distance <= 32.0D;
        }
        return distance <= 24.0D;
    }

    @Nullable
    private LivingEntity getOwnerCommandTarget(LivingEntity currentOwner) {
        if (currentOwner instanceof MobEntity) {
            LivingEntity ownerTarget = ((MobEntity) currentOwner).getTarget();
            if (ownerTarget != null && ownerTarget.isAlive()) {
                return ownerTarget;
            }
        }

        if (currentOwner.getLastHurtMob() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) currentOwner.getLastHurtMob();
            if (target.isAlive()) {
                return target;
            }
        }

        if (currentOwner.getLastHurtByMob() != null && currentOwner.getLastHurtByMob().isAlive()) {
            return currentOwner.getLastHurtByMob();
        }

        LivingEntity aimedTarget = findCommandedTargetInFront(currentOwner, 20.0D);
        if (aimedTarget != null) {
            return aimedTarget;
        }

        return this.getTarget();
    }

    @Nullable
    private LivingEntity findCommandedTargetInFront(LivingEntity owner, double range) {
        Vector3d eyePos = owner.getEyePosition(1.0F);
        Vector3d look = owner.getLookAngle();
        AxisAlignedBB searchBox = owner.getBoundingBox().inflate(range, 8.0D, range);
        LivingEntity bestTarget = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (LivingEntity candidate : this.level.getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (!isValidHostileTarget(candidate) || !owner.canSee(candidate)) {
                continue;
            }

            Vector3d toTarget = candidate.getEyePosition(1.0F).subtract(eyePos);
            double distance = toTarget.length();
            if (distance <= 0.001D || distance > range) {
                continue;
            }

            Vector3d direction = toTarget.normalize();
            double alignment = look.dot(direction);
            if (alignment < 0.55D) {
                continue;
            }

            double score = alignment * 1000.0D - distance;
            if (score > bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    @Override
    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        if (player == this.getOwner()) {
            ItemStack itemStack = player.getItemInHand(hand);
            if (!itemStack.isEmpty() && itemStack.getItem() == ModItems.SHADOW.get() && this.getShadows() < 15) {
                this.addShadow();
            }
        }
        return ActionResultType.PASS;
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> key) {
        if (key.equals(SHADOWS)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public EntitySize getDimensions(Pose pose) {
        float shadowsUsed = this.getShadows();
        if (shadowsUsed > 0.0F) {
            return this.getType().getDimensions().scale(1.0F + shadowsUsed / 6.0F);
        }
        return super.getDimensions(pose);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.ownerId != null) {
            nbt.putUUID("ownerId", this.ownerId);
        }
        nbt.putInt("shadows", this.entityData.get(SHADOWS));
        nbt.putBoolean("playerIllusion", this.isPlayerIllusion());
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("ownerId")) {
            this.ownerId = nbt.getUUID("ownerId");
        }
        this.entityData.set(SHADOWS, nbt.getInt("shadows"));
        this.setPlayerIllusion(nbt.getBoolean("playerIllusion"));
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeBoolean(this.ownerId != null);
        if (this.ownerId != null) {
            buffer.writeUUID(this.ownerId);
        }
        buffer.writeBoolean(this.isPlayerIllusion());
    }

    @Override
    public void readSpawnData(PacketBuffer data) {
        if (data.readBoolean()) {
            this.ownerId = data.readUUID();
        }
        this.setPlayerIllusion(data.readBoolean());
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
        this.ownerId = owner.getUUID();
        this.lastCommandSender = owner;
        this.lastCommandTime = this.level != null ? this.level.getGameTime() : 0L;
        this.currentCommand = NPCCommand.ATTACK;

        Predicate<Entity> factionScope = ModEntityPredicates.getEnemyFactions(this);
        Predicate<LivingEntity> livingFactionScope = entity -> factionScope != null && factionScope.test(entity);
        Predicate<LivingEntity> notSame = entity -> isValidHostileTarget(entity) && !(entity instanceof NightmareSoldierEntity);
        if (factionScope != null) {
            this.targetSelector.addGoal(1, new FactionHurtByTargetGoal(this, factionScope, new Class[0]));
            this.targetSelector.addGoal(2,
                    new NearestAttackableTargetGoal<>(this, MobEntity.class, 10, true, true,
                            livingFactionScope.and(notSame)));
        }
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerId != null) {
            this.owner = this.level.getPlayerByUUID(this.ownerId);
        }
        return this.owner;
    }

    public void addShadow() {
        this.entityData.set(SHADOWS, this.entityData.get(SHADOWS) + 1);
    }

    public void setShadow(int value) {
        this.entityData.set(SHADOWS, value);
    }

    public int getShadows() {
        return this.entityData.get(SHADOWS);
    }

    public void setPlayerIllusion(boolean value) {
        this.entityData.set(PLAYER_ILLUSION, value);
    }

    public boolean isPlayerIllusion() {
        return this.entityData.get(PLAYER_ILLUSION);
    }

    private boolean isValidHostileTarget(@Nullable LivingEntity target) {
        LivingEntity currentOwner = this.getOwner();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target instanceof PlayerEntity && (((PlayerEntity) target).isSpectator() || ((PlayerEntity) target).isCreative())) {
            return false;
        }
        if (target == this || target == currentOwner) {
            return false;
        }
        if (target instanceof ShadowDoppelmanEntity) {
            ShadowDoppelmanEntity otherShadow = (ShadowDoppelmanEntity) target;
            if (currentOwner != null && currentOwner.equals(otherShadow.getOwner())) {
                return false;
            }
        }
        if (currentOwner != null && (target.isAlliedTo(currentOwner) || currentOwner.isAlliedTo(target))) {
            return false;
        }
        if (target.isAlliedTo(this) || this.isAlliedTo(target)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canReceiveCommandFrom(LivingEntity commandSender) {
        LivingEntity currentOwner = this.getOwner();
        return currentOwner != null && currentOwner.equals(commandSender);
    }

    @Override
    public void setCurrentCommand(@Nullable LivingEntity commandSender, NPCCommand command) {
        this.lastCommandTime = this.level.getGameTime();
        this.lastCommandSender = commandSender;
        this.currentCommand = command;
    }

    @Override
    public NPCCommand getCurrentCommand() {
        return this.currentCommand;
    }

    @Nullable
    @Override
    public LivingEntity getLastCommandSender() {
        return this.lastCommandSender;
    }

    @Override
    public long getLastCommandTime() {
        return this.lastCommandTime;
    }

    private void copyOwnerAbilities(LivingEntity owner) {
        IAbilityData ownerData = AbilityDataCapability.get(owner);
        if (ownerData == null) {
            return;
        }

        Set<ResourceLocation> copied = new HashSet<>();

        for (IAbility passive : ownerData.getPassiveAbilities()) {
            if (passive == null || !hasValidAbilityKey(passive.getCore()) || shouldSkipCopiedAbility(passive.getCore())) {
                continue;
            }
            AbilityCore<?> core = passive.getCore();
            if (core.getCategory() != AbilityCategory.STYLE) {
                continue;
            }
            if (!copied.add(core.getKey())) {
                continue;
            }
            ensureUnlocked(core);
            this.goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal(this, (AbilityCore) core));
        }

        List<IAbility> equipped = ownerData.getRawEquippedAbilities();
        for (int slot = 0; slot < equipped.size(); slot++) {
            IAbility ability = equipped.get(slot);
            if (ability == null || !hasValidAbilityKey(ability.getCore()) || shouldSkipCopiedAbility(ability.getCore())) {
                continue;
            }
            AbilityCore<?> core = ability.getCore();
            if (core.getCategory() != AbilityCategory.STYLE) {
                continue;
            }
            if (!copied.add(core.getKey())) {
                continue;
            }
            ensureEquippedInSlot(slot, core);
            int priority = 2;
            double minDistance = 0.0;
            double maxDistance = 48.0;
            int randomInterval = 4;

            if (core.getCategory() == AbilityCategory.STYLE) {
                priority = 1;
                maxDistance = 18.0;
                randomInterval = 2;
            } else if (core.getCategory() == AbilityCategory.RACIAL) {
                priority = 1;
                maxDistance = 24.0;
                randomInterval = 3;
            } else if (core.getCategory() == AbilityCategory.HAKI) {
                priority = 0;
                maxDistance = 32.0;
                randomInterval = 2;
            } else if (core.getCategory() == AbilityCategory.DEVIL_FRUITS) {
                priority = 1;
                maxDistance = 32.0;
                randomInterval = 2;
            }

            this.goalSelector.addGoal(priority,
                    new ShadowCopiedAbilityGoal(this, (AbilityCore) core, minDistance, maxDistance, randomInterval));
        }

        for (AbilityCoreUnlockWrapper<?> unlock : ownerData.getUnlockedAbilities()) {
            AbilityCore<?> core = unlock.getAbilityCore();
            if (!hasValidAbilityKey(core) || shouldSkipCopiedAbility(core)) {
                continue;
            }
            if (core.getCategory() != AbilityCategory.STYLE) {
                continue;
            }
            if (!copied.add(core.getKey())) {
                continue;
            }
            ensureEquippedInOpenSlot(core);
            this.goalSelector.addGoal(1,
                    new ShadowCopiedAbilityGoal(this, (AbilityCore) core, 0.0, 24.0, 3));
        }
    }

    private void addBossStyleGoals(LivingEntity owner, IEntityStats ownerStats) {
        IAbilityData ownerData = AbilityDataCapability.get(owner);
        if (ownerData == null) {
            return;
        }

        ResourceLocation style = ownerStats.getFightingStyle();
        ResourceLocation race = ownerStats.getRace();

        if (ownerData.hasUnlockedAbility(BusoshokuHakiHardeningAbility.INSTANCE)
                || ownerData.hasEquippedAbility(BusoshokuHakiHardeningAbility.INSTANCE)) {
            ensureEquippedInOpenSlot(BusoshokuHakiHardeningAbility.INSTANCE);
            this.goalSelector.addGoal(0, new AlwaysActiveAbilityWrapperGoal(this, BusoshokuHakiHardeningAbility.INSTANCE));
        }

        if (ownerData.hasUnlockedAbility(SoruAbility.INSTANCE)) {
            ensureEquippedInOpenSlot(SoruAbility.INSTANCE);
            this.goalSelector.addGoal(1, new SoruWrapperGoal(this));
        }
        if (ownerData.hasUnlockedAbility(GeppoAbility.INSTANCE)) {
            ensureEquippedInOpenSlot(GeppoAbility.INSTANCE);
            this.goalSelector.addGoal(1, new GeppoWrapperGoal(this));
        }

        addStyleWrapperGoals(style, ownerData);
        addRaceWrapperGoals(race);
    }

    private void addStyleWrapperGoals(@Nullable ResourceLocation style, IAbilityData ownerData) {
        if (style == null) {
            return;
        }

        if (style.equals(ModValues.SWORDSMAN)) {
            this.goalSelector.addGoal(2, new ShiShishiSonsonWrapperGoal(this));
            SanbyakurokujuPoundHoWrapperGoal poundHoGoal = new SanbyakurokujuPoundHoWrapperGoal(this);
            poundHoGoal.getAbility().addCanUseCheck(TrainerEntity.BELOW_90_CHECK);
            this.goalSelector.addGoal(2, poundHoGoal);
            this.goalSelector.addGoal(2, new HiryuKaenWrapperGoal(this));
            this.goalSelector.addGoal(2, new OTatsumakiWrapperGoal(this));
            YakkodoriWrapperGoal yakkodoriGoal = new YakkodoriWrapperGoal(this);
            yakkodoriGoal.getAbility().addCanUseCheck(TrainerEntity.BELOW_90_CHECK);
            this.goalSelector.addGoal(2, yakkodoriGoal);
        } else if (style.equals(ModValues.BRAWLER)) {
            this.goalSelector.addGoal(2, new FistsOfLoveBarrageWrapperGoal(this));
        } else if (style.equals(ModValues.BLACK_LEG)) {
            this.goalSelector.addGoal(2, new CartBienCuitGrillShotWrapperGoal(this));
            this.goalSelector.addGoal(3, new CartPartyTableKickCourseWrapperGoal(this));
        } else if (style.equals(CartValues.RYUSOKEN)) {
            this.goalSelector.addGoal(2, new RyuNoIbukiWrapperGoal(this));
            this.goalSelector.addGoal(2, new RyuNoKagizumeWrapperGoal(this));
            this.goalSelector.addGoal(3, new TalonRushWrapperGoal(this));
        } else if (style.equals(CartValues.HASSHOKEN)) {
            this.goalSelector.addGoal(2, new ButoWrapperGoal(this));
            this.goalSelector.addGoal(3, new ButoKaitenWrapperGoal(this));
        } else if (style.equals(CartValues.BLUDGEON)) {
            this.goalSelector.addGoal(2, new ThunderBaguaWrapperGoal(this));
            this.goalSelector.addGoal(2, new StrikingSwingWrapperGoal(this));
            this.goalSelector.addGoal(2, new KundaliDragonSwarmWrapperGoal(this));
            this.goalSelector.addGoal(3, new VajraArrowWrapperGoal(this));
            this.goalSelector.addGoal(3, new WhirlingMaceWrapperGoal(this));
            this.goalSelector.addGoal(3, new ConquerorOfThreeWorldsRagnarakuWrapperGoal(this));
            this.goalSelector.addGoal(3, new DestroyerOfDeathThunderBaguaWrapperGoal(this));
        } else if (style.equals(CartValues.DAGGER)) {
            this.goalSelector.addGoal(2, new QuickstabWrapperGoal(this));
            this.goalSelector.addGoal(3, new WhirlingFangWrapperGoal(this));
        } else if (style.equals(CartValues.SABER)) {
            this.goalSelector.addGoal(2, new WildFuryWrapperGoal(this));
            this.goalSelector.addGoal(3, new DivineDepartureWrapperGoal(this));
        } else if (style.equals(CartValues.GREAT_BLADE)) {
            this.goalSelector.addGoal(2, new HeavySwingWrapperGoal(this));
            this.goalSelector.addGoal(3, new ThousandSlicesWrapperGoal(this));
        } else if (style.equals(CartValues.NITORYU)) {
            this.goalSelector.addGoal(2, new SaiKuruWrapperGoal(this));
            this.goalSelector.addGoal(3, new NitoryuIaiRashmonWrapperGoal(this));
        } else if (style.equals(CartValues.SANTORYU)) {
            this.goalSelector.addGoal(2, new OniGiriWrapperGoal(this));
            this.goalSelector.addGoal(3, new ToraGariWrapperGoal(this));
        } else if (style.equals(CartValues.DOUBLE_AXE)) {
            this.goalSelector.addGoal(3, new TyrantCleaveWrapperGoal(this));
        } else if (style.equals(CartValues.SPEAR)) {
            addOwnedDirectStyleGoal(ownerData, DrillJabRework.INSTANCE, 1, 0.0D, 16.0D, 2);
            addOwnedDirectStyleGoal(ownerData, VaultRework.INSTANCE, 1, 0.0D, 18.0D, 2);
        }
    }

    @SuppressWarnings("unchecked")
    private void addOwnedDirectStyleGoal(IAbilityData ownerData, AbilityCore<?> core,
                                         int priority, double minDistance, double maxDistance, int randomInterval) {
        if (ownerData == null || !hasValidAbilityKey(core)) {
            return;
        }
        if (!ownerData.hasUnlockedAbility((AbilityCore) core) && !ownerData.hasEquippedAbility((AbilityCore) core)) {
            return;
        }
        ensureEquippedInOpenSlot(core);
        this.goalSelector.addGoal(priority,
                new ShadowCopiedAbilityGoal(this, (AbilityCore) core, minDistance, maxDistance, randomInterval));
    }

    private void addRaceWrapperGoals(@Nullable ResourceLocation race) {
        if (race == null) {
            return;
        }

        if (race.equals(ModValues.MINK)) {
            this.goalSelector.addGoal(1, new EleclawWrapperGoal(this));
            this.goalSelector.addGoal(1, new SulongWrapperGoal(this));
        } else if (race.equals(CartValues.LUNARIAN)) {
            this.goalSelector.addGoal(0, new FlamesOnWrapperGoal(this));
            this.goalSelector.addGoal(2, new KaryudonWrapperGoal(this));
        } else if (race.equals(CartValues.ONI)) {
            this.goalSelector.addGoal(2, new SkullBasherWrapperGoal(this));
            this.goalSelector.addGoal(3, new ViciousRoarWrapperGoal(this));
        } else if (race.equals(CartValues.SKY_FOLK)) {
            this.goalSelector.addGoal(2, new AxeDialWrapperGoal(this));
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureUnlocked(AbilityCore<?> core) {
        IAbilityData data = AbilityDataCapability.get(this);
        if (data == null || !hasValidAbilityKey(core)) {
            return;
        }
        if (!data.hasUnlockedAbility((AbilityCore) core)) {
            data.addUnlockedAbility((AbilityCore) core, AbilityUnlock.PROGRESSION);
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureEquippedInSlot(int slot, AbilityCore<?> core) {
        IAbilityData data = AbilityDataCapability.get(this);
        if (data == null || !hasValidAbilityKey(core)) {
            return;
        }
        ensureUnlocked(core);
        IAbility existing = data.getEquippedAbility(slot);
        if (existing != null && core.equals(existing.getCore())) {
            return;
        }
        data.setEquippedAbility(slot, ((AbilityCore) core).createAbility());
    }

    private void ensureEquippedInOpenSlot(AbilityCore<?> core) {
        IAbilityData data = AbilityDataCapability.get(this);
        if (data == null || !hasValidAbilityKey(core)) {
            return;
        }
        if (data.hasEquippedAbility(core)) {
            return;
        }
        List<IAbility> equipped = data.getRawEquippedAbilities();
        for (int slot = 0; slot < equipped.size(); slot++) {
            if (equipped.get(slot) == null) {
                ensureEquippedInSlot(slot, core);
                return;
            }
        }
        ensureEquippedInSlot(0, core);
    }

    private boolean hasValidAbilityKey(@Nullable AbilityCore<?> core) {
        return core != null && core.getKey() != null;
    }

    private boolean shouldSkipCopiedAbility(AbilityCore<?> core) {
        if (!hasValidAbilityKey(core)) {
            return true;
        }
        String path = core.getKey().getPath();
        boolean isRegularHaoshoku = path != null && path.contains("haoshoku") && !path.contains("infusion");
        return "doppelman".equals(path)
                || "kagemusha".equals(path)
                || isRegularHaoshoku
                || KYOKA_ABILITY_PATHS.contains(path)
                || CLONE_SUMMON_ABILITY_PATHS.contains(path)
                || FRUITLESS_ABILITY_PATHS.contains(path);
    }

    private void applyStyleLoadout(ResourceLocation style) {
        this.setItemSlot(EquipmentSlotType.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlotType.OFFHAND, ItemStack.EMPTY);

        if (style == null) {
            return;
        }

        if (style.equals(ModValues.SWORDSMAN)) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, makeCloneWeapon(ModWeapons.WADO_ICHIMONJI.get()));
        } else if (style.equals(CartValues.SABER) || style.equals(CartValues.GREAT_BLADE)) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(KaziItems2.SHADOW_SWORD.get()));
        } else if (style.equals(CartValues.BLUDGEON)) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, makeCloneWeapon(ModWeapons.HASSAIKAI.get()));
        } else if (style.equals(CartValues.DAGGER)) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(KaziItems2.SHADOW_KNIFE.get()));
        } else if (style.equals(CartValues.SPEAR)) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, makeCloneWeapon(ModWeapons.SPEAR.get()));
        } else if (style.equals(CartValues.DOUBLE_AXE)) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(KaziItems2.SHADOW_AXE.get()));
            this.setItemSlot(EquipmentSlotType.OFFHAND, new ItemStack(KaziItems2.SHADOW_AXE.get()));
        } else if (style.equals(CartValues.NITORYU)) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(KaziItems2.SHADOW_SWORD.get()));
            this.setItemSlot(EquipmentSlotType.OFFHAND, new ItemStack(KaziItems2.SHADOW_SWORD.get()));
        } else if (style.equals(CartValues.SANTORYU)) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(KaziItems2.SHADOW_SWORD.get()));
            this.setItemSlot(EquipmentSlotType.OFFHAND, new ItemStack(KaziItems2.SHADOW_SWORD.get()));
        }

        this.setDropChance(EquipmentSlotType.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlotType.OFFHAND, 0.0F);
    }

    private ItemStack makeCloneWeapon(net.minecraft.item.Item item) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putBoolean("isClone", true);
        return stack;
    }
}
