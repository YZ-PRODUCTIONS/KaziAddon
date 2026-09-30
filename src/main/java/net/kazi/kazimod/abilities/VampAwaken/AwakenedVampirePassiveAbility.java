package net.kazi.kazimod.abilities.VampAwaken;

import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.PassiveAbility2;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;

/** Cart 0.7.5 Vampire Passive, with only its Zoan-form requirement removed. */
public class AwakenedVampirePassiveAbility extends PassiveAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "cartaddon", "vampire_passive",
            new Pair[]{ImmutablePair.of("The user gains passive night vision.", null)});
    private static final ResourceLocation ICON =
            new ResourceLocation("cartaddon", "textures/abilities/vampire_passive.png");

    public static final AbilityCore<AwakenedVampirePassiveAbility> INSTANCE;

    public AwakenedVampirePassiveAbility(AbilityCore<AwakenedVampirePassiveAbility> core) {
        super(core);
        this.addDuringPassiveEvent(this::duringPassiveEvent);
    }

    public void duringPassiveEvent(LivingEntity entity) {
        if (!entity.level.isClientSide
                && (!entity.hasEffect(Effects.NIGHT_VISION)
                || entity.getEffect(Effects.NIGHT_VISION).getDuration() <= 250)) {
            entity.addEffect(new EffectInstance(Effects.NIGHT_VISION, 500, 0, true, false));
        }
    }

    private static boolean canUnlock(LivingEntity entity) {
        IDevilFruit devilFruit = DevilFruitCapability.get(entity);
        return devilFruit != null
                && devilFruit.hasAwakenedFruit()
                && devilFruit.hasDevilFruit(CartAbilities.BATTO_BATTO_NO_MI_MODEL_VAMPIRE);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Vampire Passive",
                AbilityCategory.DEVIL_FRUITS,
                AbilityType.PASSIVE,
                AwakenedVampirePassiveAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .setIcon(ICON)
                .setUnlockCheck(AwakenedVampirePassiveAbility::canUnlock)
                .build();
    }
}
