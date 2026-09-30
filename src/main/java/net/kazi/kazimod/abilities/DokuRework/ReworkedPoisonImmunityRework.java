package net.kazi.kazimod.abilities.DokuRework;

import net.MrMagicalCart.cartaddon.init.CartEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.PotionPassiveAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class ReworkedPoisonImmunityRework
extends PotionPassiveAbility {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"kazimod", (String)"poison_immunity", (Pair[])new Pair[]{ImmutablePair.of((Object)"Provides poison immunity.", null)});
    public static final AbilityCore<ReworkedPoisonImmunityRework> INSTANCE = new AbilityCore.Builder<ReworkedPoisonImmunityRework>("Poison Immunity", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, ReworkedPoisonImmunityRework::new).addDescriptionLine(DESCRIPTION).build();

    public ReworkedPoisonImmunityRework(AbilityCore<ReworkedPoisonImmunityRework> ability) {
        super(ability);
        this.checkPotionEvent = this::checkPotionEvent;
    }

    private boolean checkPotionEvent(PlayerEntity player, EffectInstance effect) {
        return !effect.getEffect().equals(Effects.POISON) && !effect.getEffect().equals(ModEffects.DOKU_POISON.get()) && !effect.getEffect().equals(CartEffects.POISON_PINK.get());
    }
}

