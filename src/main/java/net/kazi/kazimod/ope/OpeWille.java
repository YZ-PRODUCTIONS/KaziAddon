package net.kazi.kazimod.ope;

import java.util.*;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.api.protection.block.RestrictedBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.util.TargetsPredicate;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.init.*;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class OpeWille {
    private static final Map<LivingEntity, Cast> CASTS = new WeakHashMap<>();
    private static final TargetsPredicate ENEMY = new TargetsPredicate().testEnemyFaction();
    static final class Cast {
        IAbility ability;
        boolean puncture, discharged;
        float progress;
        long refreshed, fired;
        Vector3d direction;
        ItemStack weapon;
        final Set<LivingEntity> pierced = new LinkedHashSet<>();
    }
    public static boolean active(LivingEntity user) { return CASTS.containsKey(user); }
    public static float length(boolean puncture, float progress) {
        float t = MathHelper.clamp((progress - .12F) / .68F, 0, 1);
        return 1.2F + (puncture ? 94.8F : 38.8F) * t * t * (3 - 2 * t);
    }
    public static Vector3d grip(LivingEntity user, float partial) {
        float yaw = user.yRotO + partial * MathHelper.wrapDegrees(user.yRot - user.yRotO);
        double angle = Math.toRadians(yaw);
        double side = user.getMainArm() == HandSide.RIGHT ? -.34 : .34;
        Vector3d eye = user.getEyePosition(partial);
        return eye.add(Math.cos(angle) * side, -.3, Math.sin(angle) * side).add(user.getViewVector(partial).scale(.6));
    }
    public static void begin(LivingEntity user, IAbility ability, boolean puncture) {
        if (user.level.isClientSide) return;
        cancel(user);
        Cast cast = new Cast(); cast.ability = ability; cast.puncture = puncture;
        cast.weapon = user.getMainHandItem().copy(); cast.direction = user.getLookAngle();
        cast.refreshed = user.level.getGameTime(); CASTS.put(user, cast);
        WillePacket.send(user, cast, null);
    }
    public static void charge(LivingEntity user, float progress) {
        Cast cast = CASTS.get(user);
        if (cast == null || cast.discharged) return;
        cast.refreshed = user.level.getGameTime(); cast.progress = progress;
        cast.direction = user.getLookAngle();
        float length = length(cast.puncture, progress);
        Vector3d start = grip(user, 1), end = start.add(cast.direction.scale(length));
        if (progress > .12F) {
            for (LivingEntity target : user.level.getEntitiesOfClass(LivingEntity.class,
                    new AxisAlignedBB(start, end).inflate(1), e -> valid(user, e))) {
                AxisAlignedBB box = target.getBoundingBox().inflate(.24);
                if (box.contains(start) || box.clip(start, end).isPresent()) cast.pierced.add(target);
            }
        }
        cast.pierced.removeIf(target -> !valid(user, target) || target.distanceToSqr(user) > 104 * 104
                || !target.getBoundingBox().inflate(.35).contains(start) && !target.getBoundingBox().inflate(.35).clip(start, end).isPresent());
        // The phasing blade pins locally; it never teleports a victim into terrain.
        for (LivingEntity target : cast.pierced) {
            target.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 3, 0, false, false));
            target.addEffect(new EffectInstance(ModEffects.ANTI_KNOCKBACK.get(), 3, 0, false, false));
        }
        user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 3, 0, false, false));
        if (user.tickCount % 2 == 0) WillePacket.send(user, cast, null);
    }
    private static boolean valid(LivingEntity user, LivingEntity target) {
        LivingEntity actual = owner(target);
        return actual != null && actual != user && target.isAlive() && actual.isAlive()
                && target.level == user.level && !actual.isSpectator() && ENEMY.test(user, actual);
    }
    private static LivingEntity owner(LivingEntity target) {
        if (target instanceof net.kazi.kazimod.seiryu.SeiryuPartEntity)
            return ((net.kazi.kazimod.seiryu.SeiryuPartEntity)target).owner();
        if (target instanceof net.kazi.kazimod.worldturtle.WorldTurtlePartEntity)
            return ((net.kazi.kazimod.worldturtle.WorldTurtlePartEntity)target).owner();
        return target;
    }
    public static void discharge(LivingEntity user) {
        Cast cast = CASTS.get(user);
        if (cast == null || cast.discharged) return;
        charge(user, 1);
        cast.discharged = true; cast.fired = cast.refreshed = user.level.getGameTime();
        ModDamageSource source = ModDamageSource.causeAbilityDamage(user, cast.ability)
                .setHakiNature(SourceHakiNature.SPECIAL).setPiercing(1).setUnavoidable();
        Set<UUID> damaged = new HashSet<>();
        for (LivingEntity part : cast.pierced) if (valid(user, part)) {
            LivingEntity target = owner(part);
            if (damaged.contains(target.getUUID()) || !part.hurt(source, cast.puncture ? 40 : 10)) continue;
            damaged.add(target.getUUID());
            if (cast.puncture) {
                target.removeEffect(ModEffects.ANTI_KNOCKBACK.get());
                target.removeEffect(ModEffects.MOVEMENT_BLOCKED.get());
                AbilityHelper.setDeltaMovement(target, cast.direction.x * 2.2, cast.direction.y * 2.2 + .55, cast.direction.z * 2.2);
            } else {
                target.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 60, 0, false, false));
                target.addEffect(new EffectInstance(ModEffects.ANTI_KNOCKBACK.get(), 60, 0, false, false));
                target.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN, 160, 0, false, false));
                target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 160, 1, false, false));
                target.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), 160, 1, false, false));
            }
        }
        if (cast.puncture) bore(user, grip(user, 1), cast.direction);
        user.level.playSound(null, user.blockPosition(), ModSounds.EL_THOR_SFX.get(), SoundCategory.PLAYERS, 3, cast.puncture ? .8F : 1.3F);
        WillePacket.send(user, cast, null);
    }
    private static void bore(LivingEntity user, Vector3d start, Vector3d direction) {
        if (!CommonConfig.INSTANCE.isAbilityGriefingEnabled()) return;
        int removed = 0;
        Set<BlockPos> visited = new HashSet<>();
        ProtectedAreasData data = ProtectedAreasData.get(user.level);
        for (int step = 2; step <= 96 && removed < 384; step++) {
            BlockPos center = new BlockPos(start.add(direction.scale(step)));
            if (!user.level.hasChunkAt(center) || center.getY() < 1 || center.getY() > 255) break;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
                if (removed >= 384 || !visited.add(pos.immutable()) || !user.level.hasChunkAt(pos)) continue;
                BlockState state = user.level.getBlockState(pos);
                ProtectedArea area = data.getProtectedArea(pos.getX(), pos.getY(), pos.getZ());
                if (state.isAir() || !state.getFluidState().isEmpty() || state.hasTileEntity()
                        || state.getDestroySpeed(user.level, pos) < 0 || RestrictedBlockProtectionRule.INSTANCE.isBanned(state)
                        || area != null && !area.canDestroyBlocks()) continue;
                if (user.level.destroyBlock(pos, false, user)) removed++;
            }
        }
    }
    public static void cancel(LivingEntity user) {
        Cast removed = CASTS.remove(user);
        if (removed != null) WillePacket.send(user, null, null);
    }
    @SubscribeEvent public static void tick(LivingUpdateEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (user.level.isClientSide) return;
        Cast cast = CASTS.get(user); if (cast == null) return;
        long now = user.level.getGameTime();
        if (!user.isAlive() || !ItemStack.isSame(user.getMainHandItem(), cast.weapon)
                || AbilityDataCapability.get(user).getEquippedAbility(cast.ability.getCore()) != cast.ability
                || cast.discharged && now - cast.fired >= 16 || !cast.discharged && now - cast.refreshed > 6) {
            cancel(user);
        } else if (cast.discharged && user.tickCount % 4 == 0) WillePacket.send(user, cast, null);
    }
    @SubscribeEvent public static void track(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity && event.getPlayer() instanceof ServerPlayerEntity) {
            LivingEntity user = (LivingEntity)event.getTarget();
            Cast cast = CASTS.get(user);
            if (cast != null) WillePacket.send(user, cast, (ServerPlayerEntity)event.getPlayer());
        }
    }
    private OpeWille() {}
}
