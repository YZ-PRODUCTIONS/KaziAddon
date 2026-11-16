package net.kazi.kazimod.abilities.NagiAwakening;

import java.util.Map;
import java.util.WeakHashMap;
import net.kazi.kazimod.entities.RealityMarbleWeaponEntity;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;

public final class NagiAwakeningSupport {
    // Server-thread only. Weak keys do not retain disconnected players/worlds.
    private static final Map<LivingEntity, Long> LAST_NULLIFICATION_SOUND = new WeakHashMap<>();

    private NagiAwakeningSupport() {}

    public static boolean canUnlock(LivingEntity user) {
        if (user == null) return false;
        IDevilFruit fruit = DevilFruitCapability.get(user);
        return fruit != null && fruit.hasAwakenedFruit()
                && fruit.hasDevilFruit(ModAbilities.NAGI_NAGI_NO_MI);
    }

    static boolean isEnemy(LivingEntity user, Entity other) {
        return other != null && other != user && !user.isAlliedTo(other)
                && !other.isAlliedTo(user) && ModEntityPredicates.getEnemyFactions(user).test(other);
    }

    static boolean canNegate(LivingEntity user, Entity entity) {
        if (!(entity instanceof ProjectileEntity) || !entity.isAlive() || !isFruitProjectile(entity)) return false;
        // Embedded marble swords inherit ArrowEntity for rendering, but are scenery.
        if (entity instanceof RealityMarbleWeaponEntity) return false;
        ProjectileEntity projectile = (ProjectileEntity) entity;
        // The projectile itself must come from a fruit ability; its owner's
        // powers do not turn ordinary arrows or non-fruit attacks into fruit shots.
        Entity owner = projectile instanceof AbilityProjectileEntity
                ? ((AbilityProjectileEntity) projectile).getThrower() : null;
        if (owner == null) owner = projectile.getOwner();
        if (owner == null) return true;
        if (owner instanceof LivingEntity) return isEnemy(user, owner);
        return owner != user && !user.isAlliedTo(owner) && !owner.isAlliedTo(user);
    }

    static boolean hasFruitPowers(LivingEntity target) {
        IDevilFruit fruit = DevilFruitCapability.get(target);
        if (fruit != null && fruit.hasAnyDevilFruit()) return true;
        // NPCs and borrowed fruit moves may not have an eaten fruit recorded.
        IAbilityData data = AbilityDataCapability.get(target);
        return data != null && data.getEquippedAndPassiveAbilities().stream()
                .anyMatch(ability -> ability != null
                        && ability.getCore().getCategory() == AbilityCategory.DEVIL_FRUITS);
    }

    static boolean isDevilFruitAttack(DamageSource source) {
        if (source == null) return false;
        if (source instanceof AbilityDamageSource
                && isFruitAbility(((AbilityDamageSource) source).getAbilitySource())) return true;
        // Legacy/addon projectiles can retain fruit attribution through their parent ability.
        return isFruitProjectile(source.getDirectEntity()) || isFruitProjectile(source.getEntity());
    }

    static boolean isFruitProjectile(Entity entity) {
        if (!(entity instanceof AbilityProjectileEntity)) return false;
        AbilityProjectileEntity projectile = (AbilityProjectileEntity) entity;
        if (isFruitAbility(projectile.getParent())) return true;
        DamageSource projectileSource = projectile.getDamageSource();
        return projectileSource instanceof AbilityDamageSource
                && isFruitAbility(((AbilityDamageSource) projectileSource).getAbilitySource());
    }

    private static boolean isFruitAbility(AbilityCore<?> core) {
        // Covers MMNM, Cart and Kazi without a fruit-name whitelist.
        return core != null && core.getCategory() == AbilityCategory.DEVIL_FRUITS;
    }

    static void playNullificationSound(LivingEntity user, Vector3d point) {
        if (!(user.level instanceof ServerWorld)) return;
        long tick = user.level.getGameTime();
        Long previousTick = LAST_NULLIFICATION_SOUND.put(user, tick);
        // Combine same-tick callbacks/barrages; this does not delay nullification.
        if (previousTick != null && previousTick.longValue() == tick) return;
        // sounds.json picks one of the three equally weighted recordings.
        // The recordings carry the 2x volume boost; playback gains above 1 are clamped by Minecraft.
        user.level.playSound(null, point.x, point.y, point.z, KaziSounds.FRUIT_NULLIFICATION_SFX.get(),
                SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    static void shatterParticles(LivingEntity user, Vector3d point) {
        if (!(user.level instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) user.level;
        world.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 12, .25, .25, .25, .08);
        world.sendParticles(ParticleTypes.POOF, point.x, point.y, point.z, 6, .18, .18, .18, .03);
    }

}
