package net.kazi.kazimod.abilities.GoruRework;

import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;

/** Always-on observation display; does not activate Haki or grant its defensive effects. */
public final class ShaNaqbaImuruAbility extends PassiveAbility2 {
    public static final AbilityCore<ShaNaqbaImuruAbility> INSTANCE = new AbilityCore.Builder<>(
            "Sha Naqba Imuru", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, ShaNaqbaImuruAbility::new)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/kenbunshoku_haki_future_sight.png"))
            .addDescriptionLine(new StringTextComponent("Constantly reveals nearby players' ability icons using Observation Haki's ability display, without an aura or glow."),
                    new StringTextComponent("Requires awakened Goru. Observation range scales with Doriki and Haki, with a minimum of 26.5 blocks."))
            .setUnlockCheck(ShaNaqbaImuruAbility::eligible).build();

    public ShaNaqbaImuruAbility(AbilityCore<ShaNaqbaImuruAbility> core) { super(core); }

    public static boolean eligible(LivingEntity user) {
        if (user == null) return false;
        IDevilFruit fruit = DevilFruitCapability.get(user);
        return fruit != null && fruit.hasAwakenedFruit() && fruit.hasDevilFruit(CartAbilities.GORU_GORU_NO_MI);
    }

    public static boolean active(LivingEntity user) {
        if (!eligible(user) || !user.isAlive() || user.isSpectator()) return false;
        IAbilityData data = AbilityDataCapability.get(user);
        ShaNaqbaImuruAbility passive = data == null ? null : data.getPassiveAbility(INSTANCE);
        return passive != null && !passive.disableComponent.isDisabled() && !passive.isPaused();
    }
}
