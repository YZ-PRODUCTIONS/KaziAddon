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

public class PunctureWilleRework extends WilleAbility {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "puncture_wille",
            new Pair[]{ImmutablePair.of("Drive an enormous, connected K-Room blade through enemies and terrain, then release Puncture Wille and retract the sword.", null)});
    public static final AbilityCore<PunctureWilleRework> INSTANCE = new AbilityCore.Builder("Puncture Wille", AbilityCategory.DEVIL_FRUITS, PunctureWilleRework::new)
            .addDescriptionLine(DESCRIPTION)
            .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE,
                    DealDamageComponent.getTooltip(40F), ChargeComponent.getTooltip(100F), CooldownComponent.getTooltip(1000F)})
            .setSourceElement(SourceElement.LIGHTNING).setSourceHakiNature(SourceHakiNature.SPECIAL)
            .setUnlockCheck(PunctureWilleRework::canUnlock).build();
    public PunctureWilleRework(AbilityCore<PunctureWilleRework> core) { super(core, true); }
    private static boolean canUnlock(LivingEntity user) { return DevilFruitCapability.get(user).hasAwakenedFruit(); }
}
