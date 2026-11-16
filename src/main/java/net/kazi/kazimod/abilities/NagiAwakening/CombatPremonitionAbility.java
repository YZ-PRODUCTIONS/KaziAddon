package net.kazi.kazimod.abilities.NagiAwakening;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

/** A brief timing-based dodge, without flight, gravity changes or lasting effects. */
public class CombatPremonitionAbility extends Ability {
    public static final float DURATION = 30.0F;
    public static final float COOLDOWN = 360.0F;
    public static final AbilityCore<CombatPremonitionAbility> INSTANCE = new AbilityCore.Builder<>(
            "Combat Premonition", AbilityCategory.DEVIL_FRUITS, CombatPremonitionAbility::new)
            // Reserved for future use; no fruit or progression currently grants this ability.
            .setUnlockCheck(user -> false)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/kamie.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.combat_premonition.description"))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                    ContinuousComponent.getTooltip(DURATION), CooldownComponent.getTooltip(COOLDOWN))
            .build();

    private final ContinuousComponent continuous = new ContinuousComponent(this, true)
            .addTickEvent(this::tickDodge).addEndEvent(this::end);
    private final DamageTakenComponent damageTaken = new DamageTakenComponent(this).addOnAttackEvent(this::dodge);

    public CombatPremonitionAbility(AbilityCore<CombatPremonitionAbility> core) {
        super(core);
        // Required for the same component-based use/tick path as Vampire and Supa.
        this.isNew = true;
        addComponents(continuous, damageTaken);
        addUseEvent((user, ability) -> continuous.triggerContinuity(user, DURATION));
    }

    private float dodge(LivingEntity user, IAbility ability, DamageSource source, float damage) {
        // Do not cancel void, fall, fire, suffocation or other environmental damage.
        return continuous.isContinuous() && source.getEntity() != null
                && source.getEntity() != user && !source.isBypassInvul()
                && !(source instanceof ModDamageSource && ((ModDamageSource) source).isUnavoidable())
                ? 0.0F : damage;
    }

    private void tickDodge(LivingEntity user, IAbility ability) {
        if (user.tickCount % 5 == 0 && user.level instanceof ServerWorld) {
            ((ServerWorld) user.level).sendParticles(ParticleTypes.CLOUD,
                    user.getX(), user.getY() + 1, user.getZ(), 3, .25, .45, .25, .01);
        }
    }

    private void end(LivingEntity user, IAbility ability) {
        if (!user.level.isClientSide) cooldownComponent.startCooldown(user, COOLDOWN);
    }
}
