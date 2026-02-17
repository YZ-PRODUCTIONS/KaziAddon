//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.MrMagicalCart.cartaddon.abilities.axestyle.BerserkAbility;
import net.MrMagicalCart.cartaddon.abilities.axestyle.TyrantCleaveAbility;
import net.MrMagicalCart.cartaddon.entities.projectiles.axestyle.AxeProjectiles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;

public class TyrantCleaveProjectileRework extends AbilityProjectileEntity {
    Vector3d look;

    public TyrantCleaveProjectileRework(EntityType type, World world) {
        super(type, world);
    }

    public TyrantCleaveProjectileRework(World world, LivingEntity entity) {
        super((EntityType) AxeProjectiles.TYRANTCLEAVE.get(), world, entity, TyrantCleaveAbility.INSTANCE);
        this.setArmorPiercing(0.1F);
        this.setMaxLife(60);
        this.setBlocksAffectedLimit(100000);
        this.setUnavoidable();
        this.setEntityCollisionSize((double)1.0F, (double)10.0F, (double)1.0F);
        this.look = entity.getLookAngle();
        this.setPassThroughEntities();
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onEntityImpactEvent = this::onHit;
    }

    private void onHit(LivingEntity target) {
        if (this.getThrower() != null && this.getThrower() instanceof LivingEntity) {
            LivingEntity attacker = this.getThrower();
            ItemStack mainHand = attacker.getItemInHand(Hand.MAIN_HAND);
            float weaponDamage = (float)mainHand.getItem().getDamage(mainHand);
            float maxHealthDamage = target.getHealth() * 0.1F;
            float totalDamage = maxHealthDamage + weaponDamage;
            this.setDamage(totalDamage);
            BerserkAbility berserk = (BerserkAbility)AbilityDataCapability.get(attacker).getEquippedAbility(BerserkAbility.INSTANCE);
            boolean isBerserk = berserk != null && berserk.isContinuous();
            if (isBerserk) {
                totalDamage *= 1.5F;
            }

            target.hurt(this.getDamageSource(), totalDamage);
        }

    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level, (double)hit.getX(), (double)hit.getY(), (double)hit.getZ(), 3.0F);
        explosion.setStaticDamage(35.0F);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(3));
        explosion.doExplosion();
    }
}
