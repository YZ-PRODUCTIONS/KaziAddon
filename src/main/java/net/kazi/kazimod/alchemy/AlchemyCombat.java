package net.kazi.kazimod.alchemy;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.kazi.kazimod.petal.PetalFeatures;
import java.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class AlchemyCombat {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void block(LivingAttackEvent event) {
        LivingEntity victim = event.getEntityLiving();
        if (victim.level.isClientSide || event.getSource().isBypassInvul() || event.getAmount() <= 0) return;
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null || direct == victim) return;
        Entity attacker = event.getSource().getEntity();
        Vector3d start = direct instanceof LivingEntity ? ((LivingEntity)direct).getEyePosition(1)
                : direct.tickCount <= 1 ? direct.position() : new Vector3d(direct.xo, direct.yo, direct.zo);
        Vector3d end = victim.getEyePosition(1);
        List<AlchemyField> fields = victim.level.getEntitiesOfClass(AlchemyField.class,
                new AxisAlignedBB(start, end).inflate(5), f -> f.kind() == 2 && f.isPickable());
        fields.sort(Comparator.comparingDouble(f -> f.getBoundingBox().clip(start, end)
                .map(point -> point.distanceToSqr(start)).orElse(Double.POSITIVE_INFINITY)));
        for (AlchemyField field : fields) {
            if (attacker == field.owner() || field.getBoundingBox().contains(start)) continue;
            if (field.getBoundingBox().clip(start, end).isPresent()) {
                float remaining = field.durability();
                field.hurt(event.getSource(), event.getAmount());
                if (event.getAmount() <= remaining) { event.setCanceled(true); return; }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void projectile(ProjectileImpactEvent event) {
        if (event.getEntity().level.isClientSide || !(event.getRayTraceResult() instanceof EntityRayTraceResult)) return;
        Entity target = ((EntityRayTraceResult)event.getRayTraceResult()).getEntity();
        if (!(target instanceof AlchemyField) || ((AlchemyField)target).kind() != 2) return;
        AlchemyField field = (AlchemyField)target;
        Entity shot = event.getEntity();
        Entity shooter = shot instanceof xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity
                ? ((xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity)shot).getThrower()
                : shot instanceof ProjectileEntity ? ((ProjectileEntity)shot).getOwner() : null;
        if (!field.isPickable()) return;
        if (field.owner() != null && (shooter == field.owner()
                || shooter instanceof LivingEntity && !PetalFeatures.canHit(field.owner(), (LivingEntity)shooter))) {
            event.setCanceled(true);
            return;
        }
        float amount = AlchemyField.projectileDamage(shot);
        if (!Float.isFinite(amount)) return;
        float remaining = field.durability();
        event.setCanceled(true);
        field.hurt(new net.minecraft.util.IndirectEntityDamageSource("alchemy_projectile", shot, shooter), amount);
        if (amount <= remaining) shot.remove();
    }
    private AlchemyCombat() {}
}
