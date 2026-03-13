package net.kazi.kazimod.entities.projectiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.damagesource.ModIndirectEntityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.MobsHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.horo.HoroProjectiles;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class NegativeHollowReworkProjectile extends AbilityProjectileEntity {
    private Optional<LivingEntity> target = Optional.empty();

    public NegativeHollowReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    public NegativeHollowReworkProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) HoroReworkProjectiles.NEGATIVE_HOLLOW_REWORK.get(), world, player, ability);
        this.setDamage(10.0F);
        this.withEffects = () -> new EffectInstance[]{
                new EffectInstance(Effects.CONFUSION, 200, 1),
                new EffectInstance((Effect) ModEffects.NEGATIVE.get(), 40, 1),
                new EffectInstance((Effect) ModEffects.NO_HANDS.get(), 80, 0)  // ADD THIS
        };
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onTickEvent = this::onTickEvent;
    }

    public void setTarget(Optional<LivingEntity> target) {
        this.target = target;
    }

    private void onTickEvent() {
        if (this.target != null && this.target.isPresent() && this.target.get().isAlive()) {
            Vector3d dist = this.position()
                    .subtract(this.target.get().position())
                    .add(0.0D, -1.0D, 0.0D);
            double speedReduction = 12.0D;
            double speed = 0.5D;
            double xSpeed = Math.min(speed, -dist.x / speedReduction);
            double ySpeed = Math.min(speed, -dist.y / speedReduction);
            double zSpeed = Math.min(speed, -dist.z / speedReduction);
            AbilityHelper.setDeltaMovement(this, xSpeed, ySpeed, zSpeed);
        } else {
            List<LivingEntity> list = WyHelper.getNearbyLiving(
                    this.position(), this.level, 16.0D,
                    ModEntityPredicates.getEnemyFactions(this.getThrower())
            );
            list.remove(this.getThrower());
            list.sort(MobsHelper.ENTITY_THREAT);
            if (list.size() > 0) {
                this.target = list.stream().findAny();
            }
        }
    }

    private void onEntityImpactEvent(LivingEntity target) {
        ModDamageSource source = (ModDamageSource) (new ModIndirectEntityDamageSource(
                this.getDamageSource().msgId, this, this.getThrower()))
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setHakiNature(SourceHakiNature.IMBUING)
                .setSourceTypes(new ArrayList(Arrays.asList(SourceType.INTERNAL)))
                .setUnavoidable();
        target.hurtTime = target.invulnerableTime = 0;
        target.hurt(source, this.getDamage());
    }
}