package net.kazi.kazimod.particles;

import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.RegistryObject;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import net.kazi.kazimod.init.KaziParticleTypes;

public class LuckySlotParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    private final int number;

    public LuckySlotParticleEffect(int number) {
        this.number = number;
    }

    @Override
    public void spawn(Entity entity, World world, double x, double y, double z, ParticleEffect.NoDetails details) {
        if (!(world instanceof ServerWorld)) return;
        ServerWorld serverWorld = (ServerWorld) world;

        RegistryObject<ParticleType<SimpleParticleData>> type = getTypeForNumber(number);
        if (type == null || type.get() == null) return;

        SimpleParticleData data = new SimpleParticleData(type.get());
        data.setLife(60);
        data.setSize(20.0f);
        data.setMotion(0, 0.02, 0);
        data.setColor(1f, 1f, 1f, 1f);
        data.setHasScaleDecay(false);
        data.setHasMotionDecay(false);

        serverWorld.sendParticles(data,
                x, y, z,
                1,       // count
                0, 0, 0, // no spread
                0);      // no speed
    }

    public static void spawnSpinningNumber(Entity entity, World world, int number) {
        if (!(world instanceof ServerWorld)) return;
        ServerWorld serverWorld = (ServerWorld) world;

        RegistryObject<ParticleType<SimpleParticleData>> type = getTypeForNumber(number);
        if (type == null || type.get() == null) return;

        SimpleParticleData data = new SimpleParticleData(type.get());
        data.setLife(3);          // very short — disappears before the next one spawns
        data.setSize(10.0f);
        data.setMotion(0, 0.02, 0);
        data.setColor(1f, 1f, 1f, 1f);
        data.setHasScaleDecay(false);
        data.setHasMotionDecay(false);

        double spawnY = entity.getY() + entity.getBbHeight() + 1.5;
        serverWorld.sendParticles(data,
                entity.getX(), spawnY, entity.getZ(),
                1, 0, 0, 0, 0);
    }

    private static RegistryObject<ParticleType<SimpleParticleData>> getTypeForNumber(int n) {
        switch (n) {
            case 0: return KaziParticleTypes.LUCKY_SLOT_0;
            case 1: return KaziParticleTypes.LUCKY_SLOT_1;
            case 2: return KaziParticleTypes.LUCKY_SLOT_2;
            case 3: return KaziParticleTypes.LUCKY_SLOT_3;
            case 4: return KaziParticleTypes.LUCKY_SLOT_4;
            case 5: return KaziParticleTypes.LUCKY_SLOT_5;
            case 6: return KaziParticleTypes.LUCKY_SLOT_6;
            case 7: return KaziParticleTypes.LUCKY_SLOT_7;
            case 8: return KaziParticleTypes.LUCKY_SLOT_8;
            case 9: return KaziParticleTypes.LUCKY_SLOT_9;
            default: return null;
        }
    }

    public static void spawnForNumber(Entity entity, World world, int number) {
        if (!(world instanceof ServerWorld)) return;
        ServerWorld serverWorld = (ServerWorld) world;

        RegistryObject<ParticleType<SimpleParticleData>> type = getTypeForNumber(number);
        if (type == null || type.get() == null) return;

        SimpleParticleData data = new SimpleParticleData(type.get());
        data.setLife(60);
        data.setSize(10.0f);
        data.setMotion(0, 0.02, 0);
        data.setColor(1f, 1f, 1f, 1f);
        data.setHasScaleDecay(false);
        data.setHasMotionDecay(false);

        double spawnY = entity.getY() + entity.getBbHeight() + 1.5;
        serverWorld.sendParticles(data,
                entity.getX(), spawnY, entity.getZ(),
                1,
                0, 0, 0,
                0);
    }
}