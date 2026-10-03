package net.kazi.kazimod.preserved.cerberus;

import java.util.*;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

/** Short-lived combat state is attached to live entities, never persisted across sessions. */
public final class CerberusCombat {
    private static final Map<LivingEntity, Map<UUID, Mark>> MARKS = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> WOUNDED = new WeakHashMap<>();
    private static final Map<LivingEntity, Combo> COMBOS = new WeakHashMap<>();
    private static final Map<LivingEntity, CerberusEffectEntity> TERRITORIES = new WeakHashMap<>();
    static final class Mark { long expires; int count; final Set<String> moves = new HashSet<>(); }
    static final class Combo { long last; int step; }
    public static boolean active(LivingEntity e) {
        return CartMorphs.CERBERUS_GUARD.get().isActive(e) || hybrid(e);
    }
    public static boolean hybrid(LivingEntity e) { return CartMorphs.CERBERUS_HEAVY.get().isActive(e); }
    public static boolean enemy(LivingEntity owner, LivingEntity target) {
        return owner != target && target.isAlive() && !target.isSpectator() && !owner.isAlliedTo(target)
                && (!(target instanceof PlayerEntity) || !((PlayerEntity) target).isCreative())
                && (!(owner instanceof PlayerEntity) || !(target instanceof PlayerEntity)
                    || ((PlayerEntity)owner).canHarmPlayer((PlayerEntity)target));
    }
    public static boolean fullyMarked(LivingEntity owner, LivingEntity target) {
        Map<UUID, Mark> map = MARKS.get(target); Mark m = map == null ? null : map.get(owner.getUUID());
        return m != null && m.expires > target.level.getGameTime() && m.count >= 3;
    }
    public static void mark(LivingEntity owner, LivingEntity target, String move, boolean bite) {
        long now = target.level.getGameTime();
        Map<UUID, Mark> map = MARKS.computeIfAbsent(target, t -> new HashMap<>());
        map.entrySet().removeIf(e -> e.getValue().expires <= now);
        Mark m = map.computeIfAbsent(owner.getUUID(), id -> new Mark());
        if (bite && m.count >= 3) {
            target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 30, 1));
            if (move.equals("EXECUTION")) WOUNDED.put(target, now + 80);
            map.remove(owner.getUUID());
            return;
        }
        if (m.moves.add(move)) m.count = Math.min(3, m.count + (inTerritory(owner, target) ? 2 : 1));
        m.expires = now + 120;
    }
    public static void heal(LivingHealEvent event) {
        Long until = WOUNDED.get(event.getEntityLiving());
        if (until == null) return;
        if (until <= event.getEntityLiving().level.getGameTime()) WOUNDED.remove(event.getEntityLiving());
        else event.setAmount(event.getAmount() * .5F);
    }
    public static void attack(AttackEntityEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player.level.isClientSide || !active(player) || !(event.getTarget() instanceof LivingEntity)
                || !enemy(player, (LivingEntity)event.getTarget()) || player.getAttackStrengthScale(.5F) < .85F) return;
        Combo combo = COMBOS.computeIfAbsent(player, p -> new Combo());
        long now = player.level.getGameTime();
        if (now - combo.last > 35) combo.step = 0;
        combo.last = now;
        combo.step = (combo.step + 1) % 3;
        if (combo.step == 0) {
            Vector3d look = player.getLookAngle();
            AbilityHelper.setDeltaMovement(player, look.x * .35, player.getDeltaMovement().y, look.z * .35);
        }
    }
    public static void meleeHit(LivingDamageEvent event) {
        if (event.getEntityLiving().level.isClientSide || event.getAmount() <= 0
                || !(event.getSource().getEntity() instanceof PlayerEntity)
                || event.getSource().getDirectEntity() != event.getSource().getEntity()
                || !event.getSource().getMsgId().equals("player")) return;
        PlayerEntity owner = (PlayerEntity)event.getSource().getEntity();
        if (active(owner) && enemy(owner, event.getEntityLiving())) {
            Combo combo=COMBOS.get(owner);
            if(combo!=null && combo.step==0 && owner.level.getGameTime()-combo.last<4)event.setAmount(event.getAmount()*1.3F);
            mark(owner, event.getEntityLiving(), "M1", true);
        }
    }
    public static boolean inTerritory(LivingEntity owner, LivingEntity target) {
        CerberusEffectEntity field = TERRITORIES.get(owner);
        return field != null && field.isAlive() && field.level == target.level
                && CerberusTerritory.contains(target.getX()-field.getX(), target.getY()-field.getY(), target.getZ()-field.getZ());
    }
    public static void territory(LivingEntity owner) {
        CerberusEffectEntity old = TERRITORIES.remove(owner);
        if (old != null) old.remove();
        TERRITORIES.put(owner, CerberusEffectEntity.spawn(owner, 3, owner.position(), Vector3d.ZERO));
    }
    public static void clear(LivingEntity owner) {
        CerberusEffectEntity old = TERRITORIES.remove(owner); if (old != null) old.remove();
        COMBOS.remove(owner); WOUNDED.remove(owner); MARKS.remove(owner);
    }
    public static void expired(LivingEntity owner, CerberusEffectEntity effect) {
        if(TERRITORIES.get(owner)==effect)TERRITORIES.remove(owner);
    }
    public static Vector3d mouth(LivingEntity owner, int head) {
        double scale = hybrid(owner) ? .065 : 1.65 / 16;
        double side = (head - 1) * (hybrid(owner) ? 13 : 20.5) * scale;
        double height = (hybrid(owner) ? (head == 1 ? 68 : 62) : (head == 1 ? 59 : 52)) * scale;
        double yaw = Math.toRadians(owner.yBodyRot);
        return owner.position().add(Math.cos(yaw) * side - Math.sin(yaw) * 2,
                height, Math.sin(yaw) * side + Math.cos(yaw) * 2);
    }
}
