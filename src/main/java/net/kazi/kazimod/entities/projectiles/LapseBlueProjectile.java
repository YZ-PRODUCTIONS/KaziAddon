package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.Gojo.DomainExpansionInfiniteVoidAbility;
import net.kazi.kazimod.abilities.Gojo.HollowPurpleAbility;
import net.kazi.kazimod.abilities.Gojo.MaxOutputLapseBlueAbility;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LapseBlueProjectile extends AbilityProjectileEntity {

    public static final Map<UUID, LapseBlueProjectile> ACTIVE_PROJECTILES = new HashMap<>();

    private LivingEntity caster = null;
    private Ability storedAbility = null;
    private boolean stopped = false;

    private static final double PULL_RADIUS = 23.0;
    private static final double PULL_STRENGTH = 0.65;
    private static final int MAX_LIFE = 140;
    private static final int BLOCK_ABSORB_RADIUS = 2;
    private static final int DAMAGE_INTERVAL_TICKS = 40;
    private static final float DAMAGE = 35.0F;

    private int damageTicker = 0;

    public LapseBlueProjectile(EntityType type, World world) {
        super(type, world);
    }

    public LapseBlueProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) GojoProjectiles.LAPSE_BLUE.get(), world, player, ability);
        this.caster = player;
        this.storedAbility = ability;
        this.setDamage(DAMAGE);
        this.setMaxLife(MAX_LIFE);
        this.setPassThroughBlocks();
        this.setPassThroughEntities();
        this.onTickEvent = this::onTickEvent;
    }

    public void toggleStopped() {
        this.stopped = !this.stopped;
        if (stopped) {
            this.setDeltaMovement(Vector3d.ZERO);
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide && caster != null && this.isAlive()) {
            ACTIVE_PROJECTILES.put(caster.getUUID(), this);
        }
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide && caster != null) {
            ACTIVE_PROJECTILES.remove(caster.getUUID());
            if (storedAbility instanceof MaxOutputLapseBlueAbility && caster instanceof PlayerEntity) {
                ((MaxOutputLapseBlueAbility) storedAbility).startCooldown((PlayerEntity) caster);
            }
        }
        super.remove();
    }

    private void absorbNearbyBlocks() {
        if (!CommonConfig.INSTANCE.isAbilityGriefingEnabled()) return;
        BlockPos center = this.blockPosition();
        for (int x = -BLOCK_ABSORB_RADIUS; x <= BLOCK_ABSORB_RADIUS; x++) {
            for (int y = -BLOCK_ABSORB_RADIUS; y <= BLOCK_ABSORB_RADIUS; y++) {
                for (int z = -BLOCK_ABSORB_RADIUS; z <= BLOCK_ABSORB_RADIUS; z++) {
                    BlockPos pos = center.offset(x, y, z);

                    // Respect ability protection zones
                    if (ProtectedAreasData.get(this.level)
                            .isInsideRestrictedArea(pos.getX(), pos.getY(), pos.getZ())) continue;

                    BlockState state = this.level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.is(Blocks.BEDROCK)) continue;
                    if (state.is(Blocks.BARRIER)) continue;
                    if (state.is(Blocks.COMMAND_BLOCK)) continue;
                    if (state.is(Blocks.CHAIN_COMMAND_BLOCK)) continue;
                    if (state.is(Blocks.REPEATING_COMMAND_BLOCK)) continue;
                    if (state.is(Blocks.END_PORTAL_FRAME)) continue;
                    if (state.getDestroySpeed(this.level, pos) < 0) continue;
                    this.level.removeBlock(pos, false);
                }
            }
        }
    }

    private void onTickEvent() {
        if (!this.level.isClientSide) {
            // Steer toward caster's look direction
            if (caster != null && !stopped) {
                Vector3d lookDir = caster.getLookAngle().normalize().scale(1.2);
                Vector3d currentVel = this.getDeltaMovement();
                Vector3d newVel = currentVel.scale(0.5).add(lookDir.scale(0.5));
                if (newVel.length() > 1.2) {
                    newVel = newVel.normalize().scale(1.2);
                }
                this.setDeltaMovement(newVel);
            } else if (stopped) {
                this.setDeltaMovement(Vector3d.ZERO);
            }

            // Spawn blue projectile particle
            for (net.minecraft.entity.player.ServerPlayerEntity player :
                    ((net.minecraft.world.server.ServerWorld) this.level).players()) {
                xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData blueData =
                        new xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData(
                                (net.minecraft.particles.ParticleType) net.kazi.kazimod.init.KaziParticleTypes.GOJO_BLUE_PROJECTILE.get());
                blueData.setLife(2);
                blueData.setMotion(0.0, 0.0, 0.0);
                ((net.minecraft.world.server.ServerWorld) this.level).sendParticles(
                        player, blueData, true,
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        1, 0, 0, 0, 0
                );
            }

            absorbNearbyBlocks();

            // Pull nearby entities toward projectile
            for (LivingEntity target : WyHelper.getNearbyLiving(
                    this.position(), this.level, PULL_RADIUS,
                    ModEntityPredicates.getEnemyFactions(caster))) {
                if (target.isAlive() && target != caster) {
                    Vector3d toProjectile = this.position().subtract(target.position());
                    double dist = toProjectile.length();
                    if (dist > 0.001) {
                        double falloff = 1.0 - (dist / PULL_RADIUS);
                        double strength = PULL_STRENGTH * falloff;
                        Vector3d pull = toProjectile.normalize().scale(strength);
                        target.setDeltaMovement(target.getDeltaMovement().add(pull));
                        target.hurtMarked = true;
                    }
                }
            }

            // Deal damage every 3 seconds
            damageTicker++;
            if (damageTicker >= DAMAGE_INTERVAL_TICKS) {
                damageTicker = 0;
                for (LivingEntity target : WyHelper.getNearbyLiving(
                        this.position(), this.level, PULL_RADIUS,
                        ModEntityPredicates.getEnemyFactions(caster))) {
                    if (target.isAlive() && target != caster) {
                        target.hurt(this.getDamageSource(), DAMAGE);
                    }
                }
            }

            // Absorb nearby projectiles
            // Absorb nearby projectiles
            List<Entity> nearbyEntities = this.level.getEntities(
                    this, this.getBoundingBox().inflate(PULL_RADIUS)
            );
            for (Entity e : nearbyEntities) {
                if (e == this) continue;
                if (e instanceof AbilityProjectileEntity) {
                    AbilityProjectileEntity proj = (AbilityProjectileEntity) e;
                    if (proj.getThrower() == caster) continue;
                    // Don't absorb high-damage projectiles
                    if (proj.getDamage() > 60.0F) continue;
                    e.remove();
                } else if (e instanceof ProjectileEntity) {
                    ProjectileEntity proj = (ProjectileEntity) e;
                    if (proj.getOwner() == caster) continue;
                    e.remove();
                }
            }

            // Check for collision with Red projectiles — spawn Hollow Nuke
            if (caster != null) {
                List<Entity> allNearby = this.level.getEntities(
                        this, this.getBoundingBox().inflate(4.0)
                );
                for (Entity e : allNearby) {
                    if (e instanceof RedProjectile || e instanceof MaxOutputRedProjectile) {
                        Vector3d spawnPos = this.position();

                        // Check if Hollow Purple is on cooldown
                        boolean hollowPurpleOnCooldown = false;
                        // Check if domain is active
                        boolean domainActive = false;

                        if (caster instanceof PlayerEntity) {
                            IAbilityData data = AbilityDataCapability.get(caster);

                            HollowPurpleAbility hollowPurple = (HollowPurpleAbility) data.getEquippedAbility(HollowPurpleAbility.INSTANCE);
                            if (hollowPurple != null && hollowPurple.isOnCooldown(caster)) {
                                hollowPurpleOnCooldown = true;
                            }

                            DomainExpansionInfiniteVoidAbility domain = (DomainExpansionInfiniteVoidAbility) data.getEquippedAbility(DomainExpansionInfiniteVoidAbility.INSTANCE);
                            if (domain != null && domain.isDomainActive()) {
                                domainActive = true;
                            }
                        }

                        if (!hollowPurpleOnCooldown && !domainActive && this.storedAbility != null) {
                            HollowNukeProjectile hollowNuke = new HollowNukeProjectile(
                                    this.level, caster, this.storedAbility
                            );
                            hollowNuke.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                            hollowNuke.setDeltaMovement(Vector3d.ZERO);
                            this.level.addFreshEntity(hollowNuke);

                            HollowPurpleAbility.startCooldownFromOutside(caster);
                        }

                        e.remove();
                        this.remove();
                        return;
                    }
                }
            }
        }
    }
}