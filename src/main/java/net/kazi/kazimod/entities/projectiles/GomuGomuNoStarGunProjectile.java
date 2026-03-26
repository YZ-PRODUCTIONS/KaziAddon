package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoPistolRework;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

import java.util.Optional;

public class GomuGomuNoStarGunProjectile extends AbilityProjectileEntity {
    private Optional<LivingEntity> target = Optional.empty();
    private boolean isLaunched = false;

    public GomuGomuNoStarGunProjectile(EntityType type, World world) {
        super(type, world);
    }

    public GomuGomuNoStarGunProjectile(World world, LivingEntity player) {
        super((EntityType) GomuReworkProjectiles.GOMU_GOMU_NO_STAR_GUN.get(), world, player, GomuGomuNoPistolRework.INSTANCE);
        this.setDamage(85.0F);
        this.setMaxLife(200);
        super.setFist();
        this.setEntityCollisionSize((double) 1.0F);
        this.setUnavoidable();
        this.setDamageSource(this.getDamageSource().setSourceElement(SourceElement.RUBBER));
        super.setGravity(0.0F);
        super.onTickEvent = this::onTickEvent;
    }

    public void setTarget(Optional<LivingEntity> target) {
        this.target = target;
    }

    public void setLaunched(boolean launched) {
        this.isLaunched = launched;
    }

    public boolean isLaunched() {
        return this.isLaunched;
    }

    private void onTickEvent() {
        if (!this.isLaunched) return;
        }

    private void onEntityImpactEvent(LivingEntity entity) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 60, 0));

    }
}

