package net.kazi.kazimod.ope;

import java.util.*;
import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedRoomAbility;
import net.MrMagicalCart.cartaddon.init.CartEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.util.TargetsPredicate;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class OpeAmputate {
    public static final double RANGE = 120;
    private static final TargetsPredicate TARGETS = new TargetsPredicate().testEnemyFaction();
    private static final Map<LivingEntity, Long> SPLIT = new WeakHashMap<>();

    public static LivingEntity aimedTarget(LivingEntity user) {
        ReworkedRoomAbility room = AbilityDataCapability.get(user).getEquippedAbility(ReworkedRoomAbility.INSTANCE);
        if (room == null || !room.isEntityInRoom(user)) return null;
        Vector3d start = user.getEyePosition(1), end = start.add(user.getLookAngle().scale(RANGE));
        BlockRayTraceResult wall = user.level.clip(new RayTraceContext(start, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
        end = wall.getLocation();
        double closest = start.distanceToSqr(end);
        LivingEntity selected = null;
        for (LivingEntity target : user.level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(start, end).inflate(1), e -> e != user && e.isAlive() && !e.isSpectator()
                        && e.isPickable() && TARGETS.test(user, e) && room.isEntityInRoom(e))) {
            AxisAlignedBB box = target.getBoundingBox().inflate(.1);
            Optional<Vector3d> hit = box.contains(start) ? Optional.of(start) : box.clip(start, end);
            if (!hit.isPresent()) continue;
            double distance = start.distanceToSqr(hit.get());
            if (distance < closest) { closest = distance; selected = target; }
        }
        return selected;
    }

    public static void use(LivingEntity user, IAbility ability, float damage) {
        if (user.level.isClientSide) return;
        LivingEntity target = aimedTarget(user);
        if (target == null) return;
        ModDamageSource source = ModDamageSource.causeAbilityDamage(user, ability)
                .setHakiNature(SourceHakiNature.SPECIAL).setPiercing(1).setUnavoidable();
        if (target.hurt(source, damage)) apply(user, target);
    }

    public static void apply(LivingEntity user, LivingEntity target) {
        if (target.level.isClientSide) return;
        boolean stronger = HakiDataCapability.get(user).getBusoshokuHakiExp()
                > HakiDataCapability.get(target).getBusoshokuHakiExp();
        int ticks = stronger ? 80 : 40;
        target.addEffect(new EffectInstance(ModEffects.ANTI_KNOCKBACK.get(), ticks, 0, false, false));
        target.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), ticks, 0, false, false));
        if (!stronger || target instanceof PlayerEntity)
            target.addEffect(new EffectInstance(ModEffects.NO_HANDS.get(), 60, 0));
        if (stronger && target instanceof OPEntity) {
            target.addEffect(new EffectInstance(CartEffects.DISABLED_ABILITIES.get(), 80, 0));
            target.addEffect(new EffectInstance(ModEffects.NO_HANDS.get(), 80, 0));
        }
        if (target.hasEffect(ModEffects.MOVEMENT_BLOCKED.get())) {
            SPLIT.put(target, target.level.getGameTime() + ticks);
            OpeVisualPacket.send(target, OpeVisualPacket.SPLIT, ticks);
        }
    }

    public static int remaining(LivingEntity target) {
        Long until = SPLIT.get(target);
        return until == null || !target.isAlive() || !target.hasEffect(ModEffects.MOVEMENT_BLOCKED.get())
                ? 0 : (int)Math.max(0, until - target.level.getGameTime());
    }

    @SubscribeEvent public static void tick(LivingUpdateEvent event) {
        LivingEntity target = event.getEntityLiving();
        if (target.level.isClientSide || !SPLIT.containsKey(target)) return;
        if (remaining(target) == 0) {
            SPLIT.remove(target);
            OpeVisualPacket.send(target, OpeVisualPacket.SPLIT, 0);
        }
    }

    @SubscribeEvent public static void track(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof LivingEntity) || !(event.getPlayer() instanceof ServerPlayerEntity)) return;
        LivingEntity target = (LivingEntity)event.getTarget();
        int ticks = remaining(target);
        if (ticks > 0) OpeVisualPacket.sendTo((ServerPlayerEntity)event.getPlayer(), target, OpeVisualPacket.SPLIT, ticks);
    }
    private OpeAmputate() {}
}
