package net.kazi.kazimod.abilities.OpeRework;

import net.kazi.kazimod.ope.WilleAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;

public class ShockWilleRework extends WilleAbility {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "shock_wille",
            new Pair[]{ImmutablePair.of("Extend the held K-Room sword through enemies, then release an internal shockwave before retracting the blade.", null)});
    public static final AbilityCore<ShockWilleRework> INSTANCE = new AbilityCore.Builder("Shock Wille", AbilityCategory.DEVIL_FRUITS, ShockWilleRework::new)
            .addDescriptionLine(DESCRIPTION)
            .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE,
                    DealDamageComponent.getTooltip(10F), ChargeComponent.getTooltip(40F), CooldownComponent.getTooltip(400F)})
            .setSourceElement(SourceElement.LIGHTNING).setSourceHakiNature(SourceHakiNature.SPECIAL)
            .setUnlockCheck(ShockWilleRework::canUnlock).build();
    public ShockWilleRework(AbilityCore<ShockWilleRework> core) { super(core, false); }
    private static boolean canUnlock(LivingEntity user) { return DevilFruitCapability.get(user).hasAwakenedFruit(); }
}
