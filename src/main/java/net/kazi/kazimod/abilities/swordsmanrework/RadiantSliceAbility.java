package net.kazi.kazimod.abilities.swordsmanrework;

import java.awt.Color;
import java.util.List;
import java.util.function.Predicate;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.*;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.*;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.*;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.init.*;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import net.kazi.kazimod.events.components.DashComboComponent;

public class RadiantSliceAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod",
                    "radiant_slice",
                    new Pair[]{ImmutablePair.of(
                            "The user dashes forward and rapidly slashes the opponent with haki-infused strikes", null)});

    private static final float COOLDOWN = 800.0F;
    private static final int CHARGE_TIME = 20;
    private static final float DAMAGE = 35.0F;
    private static final float RANGE = 2.5F;
    public static int overuse = 2000;

    public static final AbilityCore<RadiantSliceAbility> INSTANCE;

    /* COMPONENTS */
    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::duringChargeEvent)
                    .addEndEvent(this::endChargeEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final DashComboComponent dashComboComponent = new DashComboComponent(this, this::performDash);

    /* HAKI DISCHARGE */
    private LightningDischargeEntity discharge;
    private Color color;
    private int radius;
    private int haoMastery;

    // True from the moment the charge starts until startCooldown is called.
    // This is the single source of truth for "ability is busy" — covers
    // charge phase, all dashes, and the gap between the last dash and cooldown.
    private boolean isRunning = false;

    public RadiantSliceAbility(AbilityCore<RadiantSliceAbility> core) {
        super(core);

        this.isNew = true;
        this.color = new Color(16711680);
        this.radius = 0;
        this.haoMastery = 0;

        this.addComponents(new AbilityComponent[]{
                chargeComponent,
                dealDamageComponent,
                rangeComponent,
                animationComponent,
                hitTrackerComponent
        });

        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addCanUseCheck(AbilityLimits::fruitless);

        this.addUseEvent(this::onUseEvent);
        this.addTickEvent(this::comboTick);
    }

    /* ================= USE ================= */

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        // isRunning stays true from charge start all the way through to
        // startCooldown(), so there is no window where re-pressing works.
        if (isRunning) {
            return;
        }

        // Haki Infusion Check
        if (!HakiHelper.hasInfusionActive(entity) && entity instanceof PlayerEntity) {
            entity.sendMessage(new StringTextComponent("You need to activate Hao Infusion to use this move!"), entity.getUUID());
            return;
        }

        // Haki Overuse Check
        if (!WyHelper.isInChallengeDimension(entity.level)) {
            boolean isOnMaxOveruse = HakiHelper.checkForHakiOveruse(entity, overuse);
            if (isOnMaxOveruse) {
                return;
            }
        }

        // Sword Check
        if (!AbilityHelper.canUseSwordsmanAbilities(entity)) {
            entity.sendMessage(new TranslationTextComponent(ModI18n.ABILITY_MESSAGE_NEED_SWORD), entity.getUUID());
            return;
        }

        isRunning = true;
        chargeComponent.startCharging(entity, CHARGE_TIME);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        hitTrackerComponent.clearHits();
        animationComponent.start(entity, ModAnimations.ITTORYU_CHARGE);

        entity.level.playSound((PlayerEntity)null, entity.blockPosition(),
                (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());

        IHakiData hakiProps = HakiDataCapability.get(entity);
        float haoLevel = hakiProps.getTotalHakiExp() / 100.0F;
        if (haoLevel <= 1.0F) {
            this.radius = 10;
            this.haoMastery = 0;
        } else if (haoLevel > 1.0F && haoLevel <= 1.75F) {
            this.radius = 25;
            this.haoMastery = 1;
        } else if (haoLevel > 1.75F) {
            this.radius = 40;
            this.haoMastery = 2;
        }

        if (entity instanceof PlayerEntity) {
            this.color = new Color(HakiHelper.getHaoshokuColour(entity));
        }

        this.discharge = new LightningDischargeEntity(entity, entity.getX(),
                entity.getY() + 1.5F, entity.getZ(), entity.yRot, entity.xRot);
        this.discharge.setAliveTicks(-1);
        this.discharge.setUpdateRate(8);
        this.discharge.setLightningLength((float)(this.radius * 2));
        this.discharge.setColor(new Color(0, 0, 0, 100));
        this.discharge.setOutlineColor(this.color);
        this.discharge.setRenderTransparent();
        this.discharge.setDetails(16);
        int density = this.haoMastery == 2 ? 32 : 16;
        this.discharge.setDensity(density);
        this.discharge.setSize(1.0F);
        this.discharge.setSkipSegments(1);
        if (this.haoMastery == 0) {
            this.discharge.setSplit();
        }

        if (entity instanceof PlayerEntity) {
            entity.level.addFreshEntity(this.discharge);
            if (this.discharge != null) {
                this.discharge.setAliveTicks(40);
            }
        }
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(),
                5, 1, false, false));

        if (DevilFruitHelper.getDifferenceToFloor(entity) < (double)51.0F) {
            AbilityHelper.slowEntityFall(entity);
            AbilityHelper.setDeltaMovement(entity, (double)0.0F, (double)0.0F, (double)0.0F);
        }

        if (this.chargeComponent.getChargeTime() % 5.0F == 0.0F) {
            if (this.discharge != null) {
                this.discharge.setPos(entity.getX(), entity.getY() + 1.0F, entity.getZ());
            }
        }

        if (this.chargeComponent.getChargeTime() % 10.0F == 0.0F) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(),
                    (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(),
                    SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());
        }

        if (this.discharge != null && !entity.isAlive()) {
            this.discharge.setAliveTicks(0);
        }
    }

    /* ================= CHARGE END ================= */

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        animationComponent.stop(entity);

        entity.level.playSound((PlayerEntity)null, entity.blockPosition(),
                (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(),
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);

        if (this.discharge != null) {
            this.discharge.setAliveTicks(30);
        }

        dashComboComponent.startCombo(entity, 3, 15);
    }

    /* ================= COMBO TICK ================= */

    private void comboTick(LivingEntity entity, IAbility ability) {
        dashComboComponent.tick(ability);
    }

    /* ================= DASH LOGIC ================= */

    private void performDash(LivingEntity entity, IAbility ability) {

        hitTrackerComponent.clearHits();

        ItemStack stack = entity.getMainHandItem();
        stack.hurtAndBreak(1, entity,
                user -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));

        if (DevilFruitHelper.getDifferenceToFloor(entity) < (double)51.0F) {
            AbilityHelper.slowEntityFall(entity);
            AbilityHelper.setDeltaMovement(entity, (double)0.0F, (double)0.0F, (double)0.0F);
        }

        BlockPos.Mutable blockPos =
                WyHelper.rayTraceBlockSafe(entity, 30.0F).mutable();

        AbilityDamageSource source =
                (AbilityDamageSource)((ModDamageSource)
                        dealDamageComponent.getDamageSource(entity)).setSlash();
        source.setUnavoidable();

        Vector3d startPos = entity.position();
        float actualDistance = 30.0F;

        for (double f = 0.0; f < 1.0; f += 0.13) {

            Vector3d pos = new Vector3d(
                    MathHelper.lerp(f, startPos.x(), blockPos.getX()),
                    MathHelper.lerp(f, startPos.y(), blockPos.getY()),
                    MathHelper.lerp(f, startPos.z(), blockPos.getZ()));

            List<ProjectileEntity> projectiles =
                    WyHelper.getNearbyEntities(
                            pos,
                            entity.level,
                            entity.getBbWidth(),
                            entity.getBbHeight(),
                            entity.getBbWidth(),
                            (Predicate<Entity>) null,
                            ProjectileEntity.class);

            if (!projectiles.isEmpty()) {
                projectiles.sort(TargetHelper.closestComparator(startPos));
                actualDistance =
                        MathHelper.sqrt(projectiles.get(0).distanceToSqr(startPos));
                break;
            }
        }

        blockPos.set(WyHelper.rayTraceBlockSafe(entity, actualDistance));

        for (LivingEntity target :
                rangeComponent.getTargetsInLine(entity, actualDistance, RANGE)) {

            if (hitTrackerComponent.canHit(target)) {

                boolean hit =
                        dealDamageComponent.hurtTarget(entity, target, DAMAGE, source);

                if (hit && !entity.level.isClientSide) {
                    WyHelper.spawnParticles(
                            ParticleTypes.SWEEP_ATTACK,
                            (ServerWorld) entity.level,
                            target.getX(),
                            target.getY() + target.getEyeHeight(),
                            target.getZ());
                }
            }
        }

        entity.stopRiding();
        entity.teleportToWithTicket(
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ());

        if (!entity.level.isClientSide) {
            ((ServerWorld) entity.level)
                    .getChunkSource()
                    .broadcastAndSend(entity,
                            new SAnimateHandPacket(entity, 0));
        }

        entity.level.playSound(null,
                entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS,
                2.0F,
                1.0F);

        // Only after the final dash: clear the lock and start cooldown.
        // isRunning is reset here so the cooldown component's own
        // isCoolingDown() gate takes over from this point forward.
        if (!dashComboComponent.isActive()) {
            isRunning = false;
            cooldownComponent.startCooldown(entity, COOLDOWN);
        }
    }

    /* ================= UNLOCK ================= */

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isSwordsman() && questProps.hasFinishedQuest(ModQuests.SWORDSMAN_TRIAL_05);
        }
    }

    /* ================= CORE ================= */

    static {
        INSTANCE = new AbilityCore.Builder(
                "radiant_slice",
                AbilityCategory.STYLE,
                RadiantSliceAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(30.0F, RangeType.LINE))
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(SourceType.SLASH)
                .setUnlockCheck(RadiantSliceAbility::canUnlock)
                .build();
    }
}