//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.effects;

import net.MrMagicalCart.cartaddon.init.CartParticleTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.effects.DamageOverTimeEffect;
import xyz.pixelatedw.mineminenomi.api.effects.ITransmisibleByTouchEffect;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class FlamingRotEffect extends DamageOverTimeEffect implements ITransmisibleByTouchEffect {
    public FlamingRotEffect() {
        super(ModDamageSource.POISON, 10.0F, 20);
        this.setDamageFunction((a) -> this.getBaseDamage() + (float)a);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);

        // Spawn blue fire particles around the affected entity
        if (!entity.level.isClientSide) {
            for (int i = 0; i < 15; i++) {
                double offsetX = (WyHelper.randomDouble() - 0.5) * entity.getBbWidth() * 2;
                double offsetY = WyHelper.randomDouble() * entity.getBbHeight();
                double offsetZ = (WyHelper.randomDouble() - 0.5) * entity.getBbWidth() * 2;

                SimpleParticleData data = new SimpleParticleData((ParticleType)CartParticleTypes.BLUE_FIRE.get());
                data.setLife(20);
                data.setSize(5.0F);
                WyHelper.spawnParticles(data, (ServerWorld)entity.level,
                        entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ);
            }
        }
    }

    @Override
    public boolean isTransmisibleByTouch() {
        return true;
    }

    @Override
    public boolean isLingering() {
        return true;
    }
}