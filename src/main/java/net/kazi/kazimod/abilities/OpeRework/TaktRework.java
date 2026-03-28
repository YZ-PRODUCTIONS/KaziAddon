package net.kazi.kazimod.abilities.OpeRework;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedOpeHelper;
import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedRoomAbility;
import net.MrMagicalCart.cartaddon.entities.projectiles.opeextra.TaktEmergenceProjectile;
import net.MrMagicalCart.cartaddon.entities.projectiles.opeextra.TaktTossProjectile;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.api.protection.block.RestrictedBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.ope.TaktBlockEntity;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TaktRework extends Ability {

    // ── Description ───────────────────────────────────────────────────────────

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "mineminenomi", "takt",
            new Pair[]{
                    ImmutablePair.of("Manipulate the environment within ROOM.", (Object) null),
                    ImmutablePair.of("  §aEMERGENCE§r Summons a spike from the ground.", (Object) null),
                    ImmutablePair.of("  §aTOSS§r Lifts a chunk of earth to throw at enemies.", (Object) null)
            });

    // ── Icons ─────────────────────────────────────────────────────────────────

    private static final ResourceLocation EMERGENCE_ICON = new ResourceLocation("mineminenomi", "textures/abilities/takt_emergence.png");
    private static final ResourceLocation TOSS_ICON      = new ResourceLocation("mineminenomi", "textures/abilities/takt_toss.png");
    private static final ResourceLocation TAKT_ICON      = new ResourceLocation("mineminenomi", "textures/abilities/takt.png");

    // ── Cooldowns / charge times ──────────────────────────────────────────────

    private static final float EMERGENCE_COOLDOWN    = 160.0F;
    private static final float EMERGENCE_CHARGE_TIME = 10.0F;
    private static final float TOSS_COOLDOWN         = 200.0F;
    private static final float TOSS_CHARGE_TIME      = 20.0F;
    private static final float TAKT_COOLDOWN         = 240.0F;
    private static final float TAKT_HOLD_TIME        = 60.0F;

    // ── Static instance ───────────────────────────────────────────────────────

    public static final AbilityCore<TaktRework> INSTANCE;

    // ── Components ────────────────────────────────────────────────────────────

    private final AltModeComponent<Mode> altModeComponent;

    private final ChargeComponent chargeComponent = (new ChargeComponent(this))
            .addStartEvent(this::onChargeStart)
            .addTickEvent(this::onChargeTick)
            .addEndEvent(this::onChargeEnd);

    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this))
            .addStartEvent(this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);

    // Two separate typed projectile components — one per projectile type
    private final ProjectileComponent emergenceProjectileComponent =
            new ProjectileComponent(this, this::createEmergenceProjectile);
    private final ProjectileComponent tossProjectileComponent =
            new ProjectileComponent(this, this::createTossProjectile);

    // ── Shared state ──────────────────────────────────────────────────────────

    private Vector3d targetPos = null;

    // ── TOSS state ────────────────────────────────────────────────────────────

    private TaktTossProjectile rock  = null;
    private boolean            swung = false;

    // ── TAKT state ────────────────────────────────────────────────────────────

    private final List<Entity> grabbedEntities = new ArrayList<Entity>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public TaktRework(AbilityCore<TaktRework> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<Mode>(this, Mode.class, Mode.EMERGENCE))
                .addChangeModeEvent(this::onAltModeChange);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.chargeComponent,
                this.continuousComponent,
                this.emergenceProjectileComponent,
                this.tossProjectileComponent
        });
        this.addCanUseCheck(ReworkedOpeHelper::hasRoomActive);
        this.addUseEvent(this::onUseEvent);
        super.setDisplayIcon(EMERGENCE_ICON);
    }

    // ── Alt mode icon swap ────────────────────────────────────────────────────

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == Mode.EMERGENCE) {
            super.setDisplayIcon(EMERGENCE_ICON);
        } else if (mode == Mode.TOSS) {
            super.setDisplayIcon(TOSS_ICON);
        } else if (mode == Mode.TAKT) {
            super.setDisplayIcon(TAKT_ICON);
        }
    }

    // ── Use event ─────────────────────────────────────────────────────────────

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.isMode(Mode.EMERGENCE)) {
            useEmergence(entity);
        } else if (this.altModeComponent.isMode(Mode.TOSS)) {
            useToss(entity);
        } else if (this.altModeComponent.isMode(Mode.TAKT)) {
            useTakt(entity);
        }
    }

    // ── EMERGENCE use ─────────────────────────────────────────────────────────

    private void useEmergence(LivingEntity entity) {
        this.chargeComponent.startCharging(entity, EMERGENCE_CHARGE_TIME);
    }

    // ── TOSS use ──────────────────────────────────────────────────────────────

    private void useToss(LivingEntity entity) {
        if (!this.continuousComponent.isContinuous() && !this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, TOSS_CHARGE_TIME);
        }
        if (this.continuousComponent.isContinuous()) {
            if (this.rock != null) {
                this.rock.setDamage(60.0F);
                this.rock.setCollideWithBlocks(true);
                this.rock.setTossed(true);
                this.rock.setCanGetStuckInGround();
            }
            this.continuousComponent.stopContinuity(entity);
        }
    }

    // ── TAKT use ──────────────────────────────────────────────────────────────

    private void useTakt(LivingEntity entity) {
        ReworkedRoomAbility room = getRoom(entity);
        if (room == null) return;

        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double) room.getROOMSize());
        BlockPos blockPos = new BlockPos(mop.getLocation());
        if (mop.getType() == RayTraceResult.Type.BLOCK) {
            blockPos = new BlockPos(((BlockRayTraceResult) mop).getBlockPos());
        } else if (mop.getType() == RayTraceResult.Type.ENTITY) {
            blockPos = new BlockPos(((EntityRayTraceResult) mop).getEntity().blockPosition());
        }

        final BlockPos finalBlockPos = blockPos;
        final ReworkedRoomAbility finalRoom = room;

        WyHelper.getNearbyBlocks(finalBlockPos, entity.level, 2, isPositionGriefable(entity, finalRoom), ImmutableList.of(Blocks.AIR))
                .stream()
                .forEach(pos -> {
                    BlockState state = entity.level.getBlockState(pos);
                    TaktBlockEntity fallingBlock = new TaktBlockEntity(entity.level, pos.getX(), pos.getY(), pos.getZ(), state);
                    AbilityHelper.setDeltaMovement(fallingBlock, 0.0, 0.0, 0.0);
                    fallingBlock.time = 5;
                    fallingBlock.setNoGravity(true);
                    fallingBlock.dropItem = false;
                    entity.level.addFreshEntity(fallingBlock);
                    entity.level.removeBlock(pos, true);
                    this.grabbedEntities.add(fallingBlock);
                });

        WyHelper.getNearbyLiving(mop.getLocation(), entity.level, 2.0, ModEntityPredicates.getEnemyFactions(entity))
                .stream()
                .filter(ModEntityPredicates.IS_ALIVE_AND_SURVIVAL)
                .filter(living -> finalRoom.isPositionInRoom(living.blockPosition()))
                .forEach(this.grabbedEntities::add);

        if (!this.grabbedEntities.isEmpty()) {
            this.continuousComponent.triggerContinuity(entity, TAKT_HOLD_TIME);
        }
    }

    // ── Charge start ──────────────────────────────────────────────────────────

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.targetPos = null;
        if (this.altModeComponent.isMode(Mode.TOSS)) {
            this.rock = null;
        }
    }

    // ── Charge tick ───────────────────────────────────────────────────────────

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && ReworkedOpeHelper.hasRoomActive(entity, this).isFail()) {
            this.chargeComponent.stopCharging(entity);
            return;
        }

        if (this.targetPos == null) {
            ReworkedRoomAbility room = getRoom(entity);
            if (room != null) {
                RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double) room.getROOMSize());
                double x = mop.getLocation().x;
                double z = mop.getLocation().z;
                int y = entity.level.getHeight(Heightmap.Type.WORLD_SURFACE, (int) x, (int) z);
                this.targetPos = new Vector3d(x, (double) y, z);
            }
        }

        if (!entity.level.isClientSide && this.targetPos != null) {
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) CartParticleEffects.TAKT_EMERGENCE_IDLE.get(),
                    entity, this.targetPos.x, this.targetPos.y, this.targetPos.z);
        }

        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 2, 1, false, false));
    }

    // ── Charge end ────────────────────────────────────────────────────────────

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.isMode(Mode.EMERGENCE)) {
            if (!entity.level.isClientSide && this.targetPos != null) {
                TaktEmergenceProjectile pillar = (TaktEmergenceProjectile) this.emergenceProjectileComponent.getNewProjectile(entity);
                pillar.moveTo(this.targetPos.x, this.targetPos.y - 6.0, this.targetPos.z, 0.0F, 0.0F);
                pillar.shoot(0.0, 0.7, 0.0, 1.4F, 0.0F);
                entity.level.addFreshEntity(pillar);
            }
            this.cooldownComponent.startCooldown(entity, EMERGENCE_COOLDOWN);
            this.targetPos = null;
        } else if (this.altModeComponent.isMode(Mode.TOSS)) {
            if (!entity.level.isClientSide && this.rock == null && this.targetPos != null) {
                this.rock = (TaktTossProjectile) this.tossProjectileComponent.getNewProjectile(entity);
                this.rock.moveTo(this.targetPos.x, this.targetPos.y - 6.0, this.targetPos.z, 0.0F, 0.0F);
                this.rock.shoot(0.0, 1.0, 0.0, 1.4F, 0.0F);
                entity.level.addFreshEntity(this.rock);
            }
            this.targetPos = null;
            this.continuousComponent.startContinuity(entity);
        }
    }

    // ── Continuity start ──────────────────────────────────────────────────────

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        // Nothing extra needed on start for either mode
    }

    // ── Continuity tick ───────────────────────────────────────────────────────

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.isMode(Mode.TOSS)) {
            if (!entity.level.isClientSide && ReworkedOpeHelper.hasRoomActive(entity, this).isFail()) {
                this.continuousComponent.stopContinuity(entity);
                return;
            }
            if (entity.swinging && !this.swung && this.continuousComponent.getContinueTime() > 20.0F) {
                if (this.rock != null) {
                    RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, 80.0);
                    Vector3d pos = mop.getLocation();
                    this.rock.lookAt(net.minecraft.command.arguments.EntityAnchorArgument.Type.FEET, pos);
                    double speed = 2.75;
                    this.rock.setDeltaMovement(
                            this.rock.getLookAngle().x * speed,
                            this.rock.getLookAngle().y * speed,
                            this.rock.getLookAngle().z * speed);
                    this.rock.setCollideWithBlocks(true);
                    this.rock.setTossed(true);
                    this.rock.setCanGetStuckInGround();
                }
                this.swung = true;
                this.continuousComponent.stopContinuity(entity);
            }

        } else if (this.altModeComponent.isMode(Mode.TAKT)) {
            if (!entity.level.isClientSide) {
                ReworkedRoomAbility room = getRoom(entity);
                if (room == null || this.grabbedEntities.isEmpty()) {
                    this.continuousComponent.stopContinuity(entity);
                    return;
                }
                final ReworkedRoomAbility finalRoom = room;
                this.grabbedEntities.forEach(target -> {
                    target.xRot = target.xRotO;
                    target.yRot = target.yRotO;
                    Random rand = new Random((long) target.hashCode());
                    double offX = WyHelper.randomWithRange(rand, -2, 2);
                    double offY = WyHelper.randomWithRange(rand, -2, 2);
                    double offZ = WyHelper.randomWithRange(rand, -2, 2);
                    double dist = 8.0;
                    Vector3d look = entity.getLookAngle();
                    Vector3d pos = new Vector3d(
                            look.x * dist + offX,
                            (double) entity.getEyeHeight() / 2.0 + look.y * dist + offY,
                            look.z * dist + offZ);
                    if ((target instanceof LivingEntity && finalRoom.isPositionInRoom(target.blockPosition()))
                            || isPositionGriefable(entity, finalRoom).test(target.blockPosition())) {
                        AbilityHelper.setDeltaMovement(target, entity.position().add(pos).subtract(target.position()));
                        if (target instanceof ServerPlayerEntity) {
                            Set<SPlayerPositionLookPacket.Flags> flags =
                                    EnumSet.of(SPlayerPositionLookPacket.Flags.X,
                                            SPlayerPositionLookPacket.Flags.Y,
                                            SPlayerPositionLookPacket.Flags.Z);
                            ((ServerPlayerEntity) target).connection.teleport(
                                    target.getX(), target.getY(), target.getZ(),
                                    target.yRotO, target.xRotO, flags);
                        }
                    }
                    target.fallDistance = 0.0F;
                });
            }
        }
    }

    // ── Continuity end ────────────────────────────────────────────────────────

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.isMode(Mode.TOSS)) {
            this.swung = false;
            this.cooldownComponent.startCooldown(entity, TOSS_COOLDOWN);
        } else if (this.altModeComponent.isMode(Mode.TAKT)) {
            this.grabbedEntities.stream()
                    .filter(Entity::isNoGravity)
                    .forEach(e -> e.setNoGravity(false));
            this.grabbedEntities.clear();
            this.cooldownComponent.startCooldown(entity, TAKT_COOLDOWN);
        }
    }

    // ── Projectile factories ──────────────────────────────────────────────────

    private TaktEmergenceProjectile createEmergenceProjectile(LivingEntity entity) {
        return new TaktEmergenceProjectile(entity.level, entity);
    }

    private TaktTossProjectile createTossProjectile(LivingEntity entity) {
        return new TaktTossProjectile(entity.level, entity);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReworkedRoomAbility getRoom(LivingEntity entity) {
        return (ReworkedRoomAbility) AbilityDataCapability.get(entity).getEquippedAbility(ReworkedRoomAbility.INSTANCE);
    }

    private static Predicate<BlockPos> isPositionGriefable(LivingEntity entity, ReworkedRoomAbility room) {
        ProtectedAreasData worldData = ProtectedAreasData.get(entity.level);
        return pos -> {
            if (!CommonConfig.INSTANCE.isAbilityGriefingEnabled()) return false;
            ProtectedArea area = worldData.getProtectedArea(pos.getX(), pos.getY(), pos.getZ());
            if (area != null && !area.canDestroyBlocks()) return false;
            if (!room.isPositionInRoom(pos)) return false;
            BlockState state = entity.level.getBlockState(pos);
            return !RestrictedBlockProtectionRule.INSTANCE.isBanned(state);
        };
    }

    // ── Mode enum ─────────────────────────────────────────────────────────────

    public enum Mode {
        EMERGENCE,
        TOSS,
        TAKT
    }

    // ── Static init ───────────────────────────────────────────────────────────

    static {
        INSTANCE = (new AbilityCore.Builder<TaktRework>("Takt", AbilityCategory.DEVIL_FRUITS, TaktRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(EMERGENCE_COOLDOWN),
                        ChargeComponent.getTooltip(EMERGENCE_CHARGE_TIME)
                })
                .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .build();
    }
}