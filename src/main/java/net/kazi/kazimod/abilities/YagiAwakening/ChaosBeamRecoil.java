package net.kazi.kazimod.abilities.YagiAwakening;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.server.ServerWorld;

/** True self-damage after Chaos Beam's finale has fully faded back to gameplay. */
public final class ChaosBeamRecoil {
    public static final float DAMAGE = 100F;
    private static final DamageSource SOURCE = new DamageSource("kazimod.chaos_beam_recoil")
            .bypassArmor().bypassMagic();

    private ChaosBeamRecoil() { }

    public static boolean isRecoil(DamageSource source) { return source == SOURCE; }

    public static void hurtCaster(LivingEntity caster) {
        if (caster == null || !(caster.level instanceof ServerWorld) || caster.level.isClientSide
                || !caster.isAlive() || caster.removed || caster.isSpectator()) return;
        int previousInvulnerability = caster.invulnerableTime;
        caster.invulnerableTime = 0;
        try {
            caster.hurt(SOURCE, DAMAGE);
        } finally {
            caster.invulnerableTime = Math.max(previousInvulnerability, caster.invulnerableTime);
        }
    }
}
