package net.kazi.kazimod.abilities.BomuRework;

import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.server.ServerWorld;

public class StunGrenadeAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "stun_grenade",
            new Pair[]{ImmutablePair.of(
                    "The user detonates a blinding explosive burst, blinding and disorienting " +
                            "all enemies nearby. Longer charges means a larger radius.",
                    (Object) null)}
    );

    private static final int    COOLDOWN    = 200;
    private static final int    CHARGE_TIME = 40;
    private static final int    MIN_RANGE   = 2;
    private static final int    MAX_RANGE   = 20;
    /** Duration in ticks for both Blindness and Dizzy (140 ticks = 7 seconds). */
    private static final int    EFFECT_DURATION = 140;

    public static final AbilityCore<StunGrenadeAbility> INSTANCE;

    private final ChargeComponent chargeComponent = (new ChargeComponent(this,
            (comp) -> (double) comp.getChargePercentage() > 0.2))
            .addTickEvent(this::duringChargeEvent)
            .addEndEvent(this::stopChargeEvent);

    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final RangeComponent     rangeComponent     = new RangeComponent(this);

    public StunGrenadeAbility(AbilityCore<StunGrenadeAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.animationComponent,
                this.rangeComponent
        });
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity player, IAbility ability) {
        this.chargeComponent.startCharging(player, (float) CHARGE_TIME);
    }

    private void duringChargeEvent(LivingEntity player, IAbility ability) {
        // Bakugo particles pulse around the user while charging
        WyHelper.spawnParticleEffect(
                (ParticleEffect) KaziParticleEffects.BAKUGO.get(),
                player,
                player.getX(), player.getY(), player.getZ()
        );
    }

    private void stopChargeEvent(LivingEntity player, IAbility ability) {
        // Explosion sound at the user's position
        ((ServerWorld) player.level).playSound(
                null,
                player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE,
                SoundCategory.PLAYERS,
                4.0F, 1.0F
        );
        // Clear crowd-control effects on the user (mirrors Flash)
        AbilityHelper.reduceEffect(player.getEffect((Effect) ModEffects.FROZEN.get()),     10.0F);
        AbilityHelper.reduceEffect(player.getEffect((Effect) ModEffects.FROSTBITE.get()),  10.0F);
        AbilityHelper.reduceEffect(player.getEffect((Effect) ModEffects.CANDY_STUCK.get()), 10.0F);
        AbilityHelper.reduceEffect(player.getEffect((Effect) ModEffects.CANDLE_LOCK.get()), 10.0F);

        // Scale radius with charge, up to MAX_RANGE
        float radius = this.chargeComponent.getChargePercentage() * (float) MAX_RANGE;

        for (LivingEntity target : this.rangeComponent.getTargetsInArea(player, radius)) {
            // Blindness — same duration and amplifier as Flash
            target.addEffect(new EffectInstance(Effects.BLINDNESS, EFFECT_DURATION, 3));
            // Dizzy — same duration
            target.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), EFFECT_DURATION, 0));

            // Bakugo particle burst at each target's eye level
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) KaziParticleEffects.BAKUGO.get(),
                    player,
                    target.getX(),
                    target.getY() + (double) target.getEyeHeight(),
                    target.getZ()
            );
        }

        this.cooldownComponent.startCooldown(player, (float) COOLDOWN);
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Stun Grenade", AbilityCategory.DEVIL_FRUITS, StunGrenadeAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip((float) COOLDOWN),
                        ChargeComponent.getTooltip((float) CHARGE_TIME),
                        RangeComponent.getTooltip((float) MIN_RANGE, (float) MAX_RANGE, RangeType.AOE)
                })
                .setUnlockCheck(StunGrenadeAbility::canUnlock)
                .setSourceElement(SourceElement.LIGHT)
                .build();
    }
}
