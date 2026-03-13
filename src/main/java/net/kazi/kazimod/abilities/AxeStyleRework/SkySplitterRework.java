package net.kazi.kazimod.abilities.AxeStyleRework;

import java.util.List;
import java.util.function.Predicate;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
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
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.*;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SkySplitterRework extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod",
                    "sky_splitter",
                    new Pair[]{ImmutablePair.of(
                            "The user dashes forward and rapidly slashes the opponent with double strikes", null)});

    private static final float COOLDOWN = 200.0F;
    private static final int CHARGE_TIME = 15;
    private static final float DAMAGE = 35.0F;
    private static final float RANGE = 2.5F;
    public static int overuse = 2000; // Haki overuse threshold

    public static final AbilityCore<SkySplitterRework> INSTANCE;

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

    /* COMBO STATE */
    private LivingEntity comboUser = null;
    private int comboDashesRemaining = 0;
    private int comboDelay = 0;

    public SkySplitterRework(AbilityCore<SkySplitterRework> core) {
        super(core);

        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                chargeComponent,
                dealDamageComponent,
                rangeComponent,
                animationComponent,
                hitTrackerComponent
        });

        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityLimits::requiresAxe);
        this.addCanUseCheck(AbilityLimits::requirestwoAxe);

        this.addUseEvent(this::onUseEvent);

        // combo tick
        this.addTickEvent(this::comboTick);
    }

    /* ================= USE ================= */

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!chargeComponent.isCharging() && comboUser == null) {
            chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        hitTrackerComponent.clearHits();
        animationComponent.start(entity, CartAnimations.PARADISE_TOTSUKA);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {

        if (DevilFruitHelper.getDifferenceToFloor(entity) < (double)51.0F) {
            AbilityHelper.slowEntityFall(entity);
            AbilityHelper.setDeltaMovement(entity, (double)0.0F, (double)0.0F, (double)0.0F);
        }
    }

    /* ================= CHARGE END ================= */

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        animationComponent.stop(entity);

        // START COMBO
        comboUser = entity;
        comboDashesRemaining = 2; // total dashes
        comboDelay = 0;           // first dash instantly
    }

    /* ================= COMBO TICK ================= */

    private void comboTick(LivingEntity entity, IAbility ability) {

        if (comboUser == null || comboUser != entity)
            return;

        if (!comboUser.isAlive()) {
            resetCombo();
            return;
        }

        if (DevilFruitHelper.getDifferenceToFloor(entity) < (double)51.0F) {
            AbilityHelper.slowEntityFall(entity);
            AbilityHelper.setDeltaMovement(entity, (double)0.0F, (double)0.0F, (double)0.0F);
        }

        if (comboDelay > 0) {
            comboDelay--;
            return;
        }

        if (comboDashesRemaining > 0) {
            performDash(comboUser);

            comboDashesRemaining--;

            if (comboDashesRemaining > 0) {
                comboDelay = 15; // 10 ticks between dashes
            } else {
                cooldownComponent.startCooldown(comboUser, COOLDOWN);
                resetCombo();
            }
        }
    }

    private void resetCombo() {
        comboUser = null;
        comboDashesRemaining = 0;
        comboDelay = 0;
    }

    /* ================= DASH LOGIC ================= */

    private void performDash(LivingEntity entity) {

        hitTrackerComponent.clearHits();

        ItemStack stack = entity.getMainHandItem();
        stack.hurtAndBreak(1, entity,
                user -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));

        if (DevilFruitHelper.getDifferenceToFloor(entity) < (double)51.0F) {
            AbilityHelper.slowEntityFall(entity);
            AbilityHelper.setDeltaMovement(entity, (double)0.0F, (double)0.0F, (double)0.0F);
        }

        BlockPos.Mutable blockPos =
                WyHelper.rayTraceBlockSafe(entity, 20.0F).mutable();

        AbilityDamageSource source =
                (AbilityDamageSource)((ModDamageSource)
                        dealDamageComponent.getDamageSource(entity)).setSlash();

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

        // DAMAGE
        for (LivingEntity target :
                rangeComponent.getTargetsInLine(entity, actualDistance, RANGE)) {

            if (hitTrackerComponent.canHit(target)) {

                boolean hit =
                        dealDamageComponent.hurtTarget(entity, target, DAMAGE, source);

                if (hit && !entity.level.isClientSide) {
                    // Sweep Attack Particles (from Divine Departure)
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

        // Dash Sound (from Divine Departure)
        entity.level.playSound(null,
                entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS,
                2.0F,
                1.0F);
    }

    /* ================= UNLOCK ================= */

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.DOUBLE_AXE) && questProps.hasFinishedQuest(CartQuests.AXE_TRIAL_04);
        }
    }

    /* ================= CORE ================= */

    static {
        INSTANCE = new AbilityCore.Builder(
                "sky_splitter",
                AbilityCategory.STYLE,
                SkySplitterRework::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(30.0F, RangeType.LINE))
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(SourceType.SLASH)
                .setUnlockCheck(SkySplitterRework::canUnlock)
                .build();
    }
}