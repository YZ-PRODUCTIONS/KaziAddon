package net.kazi.kazimod.abilities.GoruRework;

import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;

/** Always-on observation display and a Seriousness-scaled chance to dodge attacks. */
public final class ShaNaqbaImuruAbility extends PassiveAbility2 {
    public static final AbilityCore<ShaNaqbaImuruAbility> INSTANCE = new AbilityCore.Builder<>(
            "Sha Naqba Imuru", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, ShaNaqbaImuruAbility::new)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/kenbunshoku_haki_future_sight.png"))
            .addDescriptionLine(new StringTextComponent("Constantly reveals nearby players' ability icons using Observation Haki's ability display, without an aura or glow."),
                    new StringTextComponent("Requires awakened Goru. Observation range scales with Doriki and Haki, with a minimum of 26.5 blocks."),
                    new StringTextComponent("Passive dodge chance by Seriousness: Normal 1/32 (3.125%), Stage 1 1/16 (6.25%), Stage 2 1/8 (12.5%), Stage 3 1/4 (25%)."),
                    new StringTextComponent("Dodges incoming attacks, excluding unavoidable damage, thorns and environmental hazards."))
            .setUnlockCheck(ShaNaqbaImuruAbility::eligible).build();

    public ShaNaqbaImuruAbility(AbilityCore<ShaNaqbaImuruAbility> core) {
        super(core);
        addComponents(new DamageTakenComponent(this, this::dodgeAttack, DamageTakenComponent.DamageState.ATTACK));
    }

    private float dodgeAttack(LivingEntity user, IAbility ability, DamageSource source, float damage) {
        if (user.level.isClientSide || damage <= 0.0F || !active(user)
                || AbilityHelper.isWeakenedByKairosekiOrWater(user)
                || !AbilityHelper.canUseMomentumAbilities(user) || AbilityHelper.isGrabbing(user)
                || source.getEntity() == null || source.isBypassInvul()
                || source instanceof ModDamageSource && ((ModDamageSource) source).isUnavoidable()
                || source instanceof EntityDamageSource && ((EntityDamageSource) source).isThorns()) {
            return damage;
        }
        int denominator = 32 >> SeriousnessAbility.getStage(user).getLevel();
        return user.getRandom().nextInt(denominator) == 0 ? 0.0F : damage;
    }

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
