package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.kake.KakeVisuals;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;

public class PlayingCardProjectile extends AbilityProjectileEntity {

    public static final float DEFAULT_DAMAGE = 8.0f;

    public PlayingCardProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public PlayingCardProjectile(World world, LivingEntity shooter) {
        super(CasinoProjectiles.PLAYING_CARD.get(), world, shooter, (Ability) null);
        setDamage(50.0f);
        setMaxLife(50);
        setGravity(0.0f);
        setPassThroughEntities();
        setEntityCollisionSize(1.0);
        this.onEntityImpactEvent=target->KakeVisuals.impact(this,getThrower(),3);
        this.onBlockImpactEvent=pos->KakeVisuals.impact(this,getThrower(),3);
    }
}
