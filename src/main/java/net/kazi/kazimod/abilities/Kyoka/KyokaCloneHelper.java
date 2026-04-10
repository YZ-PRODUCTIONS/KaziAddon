package net.kazi.kazimod.abilities.Kyoka;

import net.kazi.kazimod.entities.ShadowDoppelmanEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.util.math.vector.Vector3d;

public final class KyokaCloneHelper {

    private KyokaCloneHelper() {
    }

    public static ShadowDoppelmanEntity spawnClone(LivingEntity owner, Vector3d position, float healthScale, boolean harmless) {
        if (owner.level.isClientSide) {
            return null;
        }

        ShadowDoppelmanEntity clone = new ShadowDoppelmanEntity(owner.level, owner);
        clone.moveTo(position.x, position.y, position.z, owner.yRot, owner.xRot);
        clone.setPlayerIllusion(true);

        double scaledHealth = Math.max(4.0D, owner.getMaxHealth() * healthScale);
        if (clone.getAttribute(Attributes.MAX_HEALTH) != null) {
            clone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledHealth);
        }
        clone.setHealth((float) Math.min(clone.getMaxHealth(), scaledHealth));

        if (harmless && clone.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            clone.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0D);
        }

        owner.level.addFreshEntity(clone);
        if (owner instanceof MobEntity) {
            LivingEntity ownerTarget = ((MobEntity) owner).getTarget();
            if (ownerTarget != null && ownerTarget.isAlive() && ownerTarget != clone) {
                clone.setTarget(ownerTarget);
            }
        }
        return clone;
    }
}
