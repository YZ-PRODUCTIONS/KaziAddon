package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.init.KaziParticleTypes;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.damagesource.ModIndirectEntityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.AirBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.CoreBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.FoliageBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.LiquidBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.OreBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HollowNukeProjectile extends AbilityProjectileEntity {

    // Static tracker so LapseBlueAbility can check if a nuke is active
    public static final Map<UUID, HollowNukeProjectile> ACTIVE_PROJECTILES = new HashMap<>();

    private boolean dealtAOE = false;
    private static final int   EXPLOSION_DELAY      = 100;
    private static final int   SPHERE_EXPAND_TICKS  = 40;
    private static final int   SPHERE_STAY_TICKS    = 100;
    private static final float TARGET_SPHERE_RADIUS = 34.0F;
    private static final float FLASH_RADIUS         = 100.0F;

    private static final BlockProtectionRule GRIEF_RULE =
            (new BlockProtectionRule.Builder(new BlockProtectionRule[]{
                    AirBlockProtectionRule.INSTANCE,
                    LiquidBlockProtectionRule.INSTANCE,
                    CoreBlockProtectionRule.INSTANCE,
                    FoliageBlockProtectionRule.INSTANCE,
                    OreBlockProtectionRule.INSTANCE
            })).build();

    public HollowNukeProjectile(EntityType type, World world) {
        super(type, world);
    }

    public HollowNukeProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) GojoProjectiles.HOLLOW_NUKE.get(), world, player, ability);
        this.setDamage(160.0F);
        this.setMaxLife(EXPLOSION_DELAY + SPHERE_EXPAND_TICKS + SPHERE_STAY_TICKS + 10);
        this.setEntityCollisionSize((double) 23.0F, (double) 23.0F, (double) 23.0F);
        this.setPassThroughEntities();
        this.setPassThroughBlocks();
        this.setUnavoidable();
        this.onTickEvent = this::onTickEvent;
    }

    @Override
    public void tick() {
        this.setDeltaMovement(this.getDeltaMovement().x, 0, this.getDeltaMovement().z);
        super.tick();
        this.noCulling = true;
        // Register in active map so other abilities can check
        if (!this.level.isClientSide && getThrower() != null && this.isAlive()) {
            ACTIVE_PROJECTILES.put(getThrower().getUUID(), this);
        }
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide && getThrower() != null) {
            ACTIVE_PROJECTILES.remove(getThrower().getUUID());
        }
        super.remove();
    }

    private void sendFlashbang() {
        for (ServerPlayerEntity player : ((ServerWorld) this.level).players()) {
            if (player.distanceTo(this) <= FLASH_RADIUS) {
                KaziPacketHandler.sendFlashbang(player);
            }
        }
    }

    private void doExplosion() {
        if (dealtAOE) return;
        dealtAOE = true;

        sendFlashbang();

        int explosionRadius  = 48;
        int shockwaveRadius  = 54;

        if (CommonConfig.INSTANCE.isAbilityGriefingEnabled()) {
            AbilityHelper.createSphere(
                    this.level,
                    this.blockPosition().below(explosionRadius / 8),
                    explosionRadius, explosionRadius,
                    false, Blocks.AIR, 3, GRIEF_RULE
            );
            AbilityHelper.createSphere(
                    this.level,
                    this.blockPosition().below(explosionRadius / 4),
                    (int)(explosionRadius * 1.2F),
                    (int)(explosionRadius * 0.4F),
                    false, Blocks.AIR, 3, GRIEF_RULE
            );
        }

        List<Entity> list = WyHelper.getNearbyEntities(
                this.position(), this.level, (double) shockwaveRadius,
                null, new Class[]{Entity.class}
        );
        list.remove(this.getThrower());

        for (Entity target : list) {
            if (target instanceof ThrowableEntity || target instanceof AbstractArrowEntity) {
                target.remove();
                continue;
            }
            if (target instanceof LivingEntity) {
                ModDamageSource source = (ModDamageSource) (new ModIndirectEntityDamageSource(
                        this.getDamageSource().msgId, this, this.getThrower()))
                        .setSourceElement(SourceElement.SHOCKWAVE)
                        .setHakiNature(SourceHakiNature.SPECIAL)
                        .setSourceTypes(new ArrayList<>(Arrays.asList(SourceType.INTERNAL)))
                        .setUnavoidable()
                        .setPiercing(1.00F);
                target.hurt(source, this.getDamage());

                Vector3d speed = target.getLookAngle().scale(-1.0F).multiply(5.0F, 0.0F, 5.0F);
                AbilityHelper.setDeltaMovement(target, speed.x, 1.0F, speed.z);
            }
        }

        this.remove();
    }

    private void onTickEvent() {
        if (!this.level.isClientSide) {
            this.setDeltaMovement(Vector3d.ZERO);

            // Play music and spawn growing particle on the very first tick
            if (this.tickCount <= 2) {
                if (this.tickCount == 1) {
                    net.minecraft.network.play.server.SPlaySoundEffectPacket musicPacket =
                            new net.minecraft.network.play.server.SPlaySoundEffectPacket(
                                    KaziSounds.HOLLOW_NUKE_MUSIC_SFX.get(),
                                    SoundCategory.PLAYERS,
                                    this.getX(), this.getY(), this.getZ(),
                                    1.0F, 1.0F
                            );
                    for (ServerPlayerEntity player : ((ServerWorld) this.level).players()) {
                        player.connection.send(musicPacket);
                    }
                }

                SimpleParticleData data = new SimpleParticleData(
                        (net.minecraft.particles.ParticleType) KaziParticleTypes.GOJO_PURPLE_GROWING.get());
                data.setMotion(0.0, 0.0, 0.0);
                for (ServerPlayerEntity player : ((ServerWorld) this.level).players()) {
                    ((ServerWorld) this.level).sendParticles(
                            player, data, true,
                            this.getX(), this.getY() + 0.5, this.getZ(),
                            1, 0, 0, 0, 0
                    );
                }
            }

            // Trigger explosion at tick 100
            if (this.tickCount == EXPLOSION_DELAY) {
                doExplosion();
            }
        }
    }
}