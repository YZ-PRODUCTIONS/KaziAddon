package net.kazi.kazimod.abilities.TripelT;

import net.kazi.kazimod.init.KaziItems2;
import net.kazi.kazimod.init.KaziMorphs;
import net.kazi.kazimod.init.KaziSounds;
import net.kazi.kazimod.entities.boss.rebelus.TrueFormRebelusBossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.SoundCategory;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public final class TripelTHelper {

    private TripelTHelper() {
    }

    public static boolean isTripelTActive(LivingEntity entity) {
        return KaziMorphs.TRIPEL_T.get().isActive(entity) || KaziMorphs.TRIPEL_T_GOD.get().isActive(entity);
    }

    public static boolean isGodForm(LivingEntity entity) {
        return KaziMorphs.TRIPEL_T_GOD.get().isActive(entity);
    }

    public static boolean isUsingTripelTWeapon(LivingEntity entity) {
        ItemStack mainHand = entity.getMainHandItem();
        return mainHand.getItem() == KaziItems2.TRIPLE_T_BAT.get() || mainHand.getItem() == KaziItems2.TRIPLE_T_STAFF.get();
    }

    public static AbilityUseResult canUseTripelTMove(LivingEntity entity) {
        if (entity instanceof TrueFormRebelusBossEntity) {
            return isUsingTripelTWeapon(entity)
                    ? AbilityUseResult.success()
                    : AbilityUseResult.fail(new StringTextComponent("True Form Rebelus must be holding the Triple T Staff."));
        }
        if (!isTripelTActive(entity)) {
            return AbilityUseResult.fail(new StringTextComponent("You need a Tripel T transformation active."));
        }
        if (!isUsingTripelTWeapon(entity)) {
            return AbilityUseResult.fail(new StringTextComponent("You must be holding the Triple T Bat or Triple T Staff."));
        }
        return AbilityUseResult.success();
    }

    public static void spawnTexturedParticleBurst(ServerWorld world, ParticleType<SimpleParticleData> type,
                                                  double x, double y, double z, int count,
                                                  float size, int life, double spread) {
        for (int i = 0; i < count; i++) {
            SimpleParticleData data = new SimpleParticleData(type);
            data.setLife(life);
            data.setSize(size + world.random.nextFloat() * 0.4F);
            double px = x + (world.random.nextDouble() - 0.5D) * spread;
            double py = y + (world.random.nextDouble() - 0.5D) * spread;
            double pz = z + (world.random.nextDouble() - 0.5D) * spread;
            WyHelper.spawnParticles(data, world, px, py, pz);
        }
    }

    public static void applyShortDizzy(LivingEntity target, Effect dizzyEffect, int ticks, int amplifier) {
        target.addEffect(new EffectInstance(dizzyEffect, ticks, amplifier, false, true));
    }

    public static Vector3d getAimPoint(LivingEntity entity, double distance) {
        RayTraceResult rayTraceResult = WyHelper.rayTraceBlocksAndEntities(entity, distance);
        return rayTraceResult.getLocation();
    }

    public static void playTungSound(LivingEntity entity, float volume, float pitch) {
        entity.level.playSound(null, entity.blockPosition(), KaziSounds.TUNG_SFX.get(), SoundCategory.PLAYERS, volume, pitch);
    }

    public static void sendMessage(LivingEntity entity, String message) {
        if (entity instanceof PlayerEntity) {
            entity.sendMessage((ITextComponent) new StringTextComponent(message), entity.getUUID());
        }
    }
}
