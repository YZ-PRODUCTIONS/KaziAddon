//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.AxeStyleRework;

import net.MrMagicalCart.cartaddon.abilities.axestyle.AxeHelper;
import net.MrMagicalCart.cartaddon.abilities.axestyle.BerserkAbility;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.command.arguments.EntityAnchorArgument.Type;
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
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.BonusOperation;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HealComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.TargetHelper;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.List;
import java.util.function.Predicate;

public class ReversalRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "reversal", new Pair[]{ImmutablePair.of("", (Object)null)});
    private static final float HOLD_TIME = 60.0F;
    private static final float MIN_COOLDOWN = 180.0F;
    private static final float COUNTER_RANGE = 10.0F;
    private static final float DASH_DISTANCE = 15.0F;
    private static final float DASH_DAMAGE = 35.0F;
    private static final float DASH_RANGE = 2.5F;
    public static final AbilityCore<ReversalRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::tickContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final PoolComponent poolComponent;
    private final DamageTakenComponent damageTakenComponent;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final HealComponent healComponent = new HealComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    // Dash delay system
    private int dashDelayTicks = 0;
    private boolean shouldPerformDash = false;
    private LivingEntity dashTarget = null;

    public ReversalRework(AbilityCore<ReversalRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.PARRY_COUNTER, new AbilityPool2[0]);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTakenEvent, DamageState.ATTACK);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.healComponent, this.dealDamageComponent, this.continuousComponent, this.animationComponent, this.poolComponent, this.damageTakenComponent, this.rangeComponent, this.hitTrackerComponent});
        this.addCanUseCheck(AbilityLimits::requiresAxe);
        this.addCanUseCheck(AbilityLimits::requirestwoAxe);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 60.0F);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, CartAnimations.FUTENRAKU);
        if (entity instanceof PlayerEntity) {
            ItemStack stack = entity.getMainHandItem();
            stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
            stack = entity.getOffhandItem();
            stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.OFFHAND));
        }

    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 0, 2, false, false));

        // Handle delayed dash
        if (shouldPerformDash) {
            dashDelayTicks++;
            // 0.5 seconds = 10 ticks (20 ticks per second)
            if (dashDelayTicks >= 15) {
                this.performDashAttack(entity, dashTarget);
                shouldPerformDash = false;
                dashDelayTicks = 0;
                dashTarget = null;
                // Stop continuity after dash is complete
                this.continuousComponent.stopContinuity(entity);
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 180.0F);

        // Reset dash delay system
        this.shouldPerformDash = false;
        this.dashDelayTicks = 0;
        this.dashTarget = null;
    }

    private float onDamageTakenEvent(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (!this.continuousComponent.isContinuous()) {
            return damage;
        } else {
            Entity sourceEntity = source.getEntity();
            if (sourceEntity == null) {
                return damage > 20.0F ? damage / 2.0F : damage;
            } else {
                entity.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 30, 3));
                if (sourceEntity instanceof LivingEntity) {
                    LivingEntity attacker = (LivingEntity)sourceEntity;
                    // Changed range check from 4.0F to 10.0F (10 blocks)
                    if ((double)attacker.distanceTo(entity) <= (double)COUNTER_RANGE) {
                        float reflectedDamage = damage * 0.5F;
                        BerserkRework berserk = (BerserkRework)AbilityDataCapability.get(entity).getEquippedAbility(BerserkRework.INSTANCE);
                        boolean isBerserk = berserk != null && berserk.isContinuous();
                        this.dealDamageComponent.getBonusManager().removeBonus(AxeHelper.AXE_DAMAGE_BONUS);
                        if (isBerserk) {
                            this.dealDamageComponent.getBonusManager().addBonus(AxeHelper.AXE_DAMAGE_BONUS, "Axe Damage Bonus", BonusOperation.MUL, 1.5F);
                        }

                        // Teleport behind the attacker before dealing damage
                        Vector3d targetLook = VectorHelper.calculateViewVectorFromBodyRot(attacker.xRot, attacker.yBodyRot).multiply((double)-2.0F, (double)0.0F, (double)-2.0F);
                        Vector3d newPos = attacker.position().add(targetLook);
                        entity.teleportToWithTicket(newPos.x, newPos.y, newPos.z);
                        entity.lookAt(Type.EYES, attacker.position().add((double)0.0F, (double)attacker.getEyeHeight(), (double)0.0F));

                        this.dealDamageComponent.hurtTarget(entity, attacker, reflectedDamage);

                        // Changed knockback: removed horizontal propulsion, added vertical launch similar to Anti-Manner Kick Course
                        AbilityHelper.setDeltaMovement(attacker, attacker.getDeltaMovement().add((double)0.0F, 1.2000000000000002, (double)0.0F));

                        // Added confusion and dizzy effects like Anti-Manner Kick Course
                        attacker.addEffect(new EffectInstance(Effects.CONFUSION, 50, 0, false, false));
                        attacker.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 50, 0, false, false));

                        if (entity.level instanceof ServerWorld) {
                            ServerWorld world = (ServerWorld)entity.level;

                            for(int i = 0; i < 5; ++i) {
                                WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, world, attacker.getX() + (world.random.nextDouble() - (double)0.5F) * (double)2.0F, attacker.getY() + (double)attacker.getEyeHeight() + (world.random.nextDouble() - (double)0.5F) * (double)2.0F, attacker.getZ() + (world.random.nextDouble() - (double)0.5F) * (double)2.0F, 1.0F, 0.0F, 0.0F);
                            }
                        }

                        // Schedule Shi Shishi Sonson-style dash after 0.5 second delay
                        this.shouldPerformDash = true;
                        this.dashDelayTicks = 0;
                        this.dashTarget = attacker;

                        // Don't stop continuity yet - let it continue for the dash delay
                        return 0.0F;
                    }
                }

                this.continuousComponent.stopContinuity(entity);
                return 0.0F;
            }
        }
    }

    private void performDashAttack(LivingEntity entity, LivingEntity originalAttacker) {
        // Clear hit tracker for the dash
        this.hitTrackerComponent.clearHits();

        // Damage weapon durability
        if (entity instanceof PlayerEntity) {
            ItemStack stack = entity.getMainHandItem();
            stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
            stack = entity.getOffhandItem();
            stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.OFFHAND));
        }

        BlockPos.Mutable blockPos = WyHelper.rayTraceBlockSafe(entity, DASH_DISTANCE).mutable();
        AbilityDamageSource source = (AbilityDamageSource)((ModDamageSource)this.dealDamageComponent.getDamageSource(entity)).setSlash();
        Vector3d startPos = entity.position();
        float actualTeleportDistance = DASH_DISTANCE;

        // Check for projectiles in the path
        for(double f = (double)0.0F; f < (double)1.0F; f += 0.13) {
            double x = MathHelper.lerp(f, startPos.x(), (double)blockPos.getX());
            double y = MathHelper.lerp(f, startPos.y(), (double)blockPos.getY());
            double z = MathHelper.lerp(f, startPos.z(), (double)blockPos.getZ());
            Vector3d pos = new Vector3d(x, y, z);
            List<ProjectileEntity> projectiles = WyHelper.getNearbyEntities(pos, entity.level, (double)entity.getBbWidth(), (double)entity.getBbHeight(), (double)entity.getBbWidth(), (Predicate)null, new Class[]{ProjectileEntity.class});
            if (!projectiles.isEmpty()) {
                projectiles.sort(TargetHelper.closestComparator(startPos));
                actualTeleportDistance = MathHelper.sqrt(((ProjectileEntity)projectiles.get(0)).distanceToSqr(startPos));
                break;
            }
        }

        blockPos.set(WyHelper.rayTraceBlockSafe(entity, actualTeleportDistance));
        double heightDifference = DevilFruitHelper.getDifferenceToFloor(entity);
        if (heightDifference > (double)1.0F && (double)blockPos.getY() > entity.getY()) {
            blockPos.setY((int)entity.getY());
        }

        // Hit all targets in the dash path
        for(LivingEntity target : this.rangeComponent.getTargetsInLine(entity, actualTeleportDistance, DASH_RANGE)) {
            if (this.hitTrackerComponent.canHit(target)) {
                boolean flag = this.dealDamageComponent.hurtTarget(entity, target, DASH_DAMAGE, source);
                if (flag && !entity.level.isClientSide) {
                    WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, (ServerWorld)entity.level, target.getX(), target.getY() + (double)target.getEyeHeight(), target.getZ());
                }
            }
        }

        // Perform the dash teleport
        entity.stopRiding();
        entity.teleportToWithTicket((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        // Play sound effect
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.DASH_ABILITY_SWOOSH_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.DOUBLE_AXE) && questProps.hasFinishedQuest(CartQuests.AXE_TRIAL_02);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Reversal", AbilityCategory.STYLE, ReversalRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ContinuousComponent.getTooltip(60.0F), CooldownComponent.getTooltip(180.0F), RangeComponent.getTooltip(10.0F, RangeType.AOE), DealDamageComponent.getTooltip(DASH_DAMAGE), RangeComponent.getTooltip(DASH_DISTANCE, RangeType.LINE)}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(new SourceType[]{SourceType.SLASH}).setUnlockCheck(ReversalRework::canUnlock).build();
    }
}