package net.kazi.kazimod.abilities.VampAwaken;

import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.PassiveStatBonusAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Awakening-only Vampire passive which permanently grants the exact attribute
 * bonuses of Cart Addon's Vampire Assault Point.
 */
public class VampireAwakeningPassive extends PassiveStatBonusAbility {

    private static final ResourceLocation ICON =
            new ResourceLocation("cartaddon", "textures/abilities/vampire_assault_point.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "vampire_awakening_passive",
            new Pair[]{ImmutablePair.of(
                    "The awakened Vampire permanently gains the physical bonuses of Vampire Assault Point.",
                    null)});

    public static final AbilityCore<VampireAwakeningPassive> INSTANCE;

    private static final AttributeModifier REGEN_RATE;
    private static final AttributeModifier TOUGHNESS;
    private static final AttributeModifier MOVEMENT_SPEED;

    private static final Predicate<LivingEntity> AWAKENED_VAMPIRE_CHECK =
            VampireAwakeningPassive::isAwakenedVampire;

    public VampireAwakeningPassive(AbilityCore<VampireAwakeningPassive> core) {
        super(core);
        this.pushStaticAttribute(ModAttributes.REGEN_RATE.get(), REGEN_RATE);
        this.pushStaticAttribute(ModAttributes.TOUGHNESS.get(), TOUGHNESS);
        this.pushStaticAttribute(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED);
    }

    @Override
    public Predicate<LivingEntity> getCheck() {
        return AWAKENED_VAMPIRE_CHECK;
    }

    private static boolean isAwakenedVampire(LivingEntity entity) {
        IDevilFruit devilFruit = DevilFruitCapability.get(entity);
        return devilFruit != null
                && devilFruit.hasAwakenedFruit()
                && devilFruit.hasDevilFruit(CartAbilities.BATTO_BATTO_NO_MI_MODEL_VAMPIRE);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Vampire Awakening Passive",
                AbilityCategory.DEVIL_FRUITS,
                AbilityType.PASSIVE,
                VampireAwakeningPassive::new
        )
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChangeStatsComponent.getTooltip()
                })
                .setIcon(ICON)
                .setUnlockCheck(VampireAwakeningPassive::isAwakenedVampire)
                .build();

        REGEN_RATE = modifier("a1a6b6f5-c9dc-48ec-b49b-b034842f4b9e", "Vampire Awakening Regen Rate Modifier", 0.2D);
        TOUGHNESS = modifier("bfffe73d-5c48-414d-b5ad-e6b1d8310bf0", "Vampire Awakening Toughness Modifier", 3.0D);
        MOVEMENT_SPEED = modifier("dbc6c5b5-7c64-4ad7-86eb-2dd411eb90f7", "Vampire Awakening Movement Modifier", 0.04D);
    }

    private static AttributeModifier modifier(String uuid, String name, double amount) {
        return new AttributeModifier(
                UUID.fromString(uuid),
                name,
                amount,
                AttributeModifier.Operation.ADDITION
        );
    }
}
