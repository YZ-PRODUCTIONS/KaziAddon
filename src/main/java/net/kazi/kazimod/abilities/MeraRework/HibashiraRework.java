package net.kazi.kazimod.abilities.MeraRework;

import net.minecraft.entity.LivingEntity;
import xyz.pixelatedw.mineminenomi.abilities.mera.HibashiraAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;

public class HibashiraRework extends HibashiraAbility {

    public static final AbilityCore<HibashiraRework> INSTANCE =
            new AbilityCore.Builder<>("Hibashira", AbilityCategory.DEVIL_FRUITS, HibashiraRework::new)
                    .setUnlockCheck(HibashiraRework::canUnlock)
                    .build();

    public HibashiraRework(AbilityCore<HibashiraRework> core) {
        super((AbilityCore) core);
    }

    private static boolean canUnlock(LivingEntity entity) {
        IDevilFruit devilFruit = DevilFruitCapability.get(entity);
        return devilFruit != null
                && devilFruit.hasDevilFruit(ModAbilities.MERA_MERA_NO_MI)
                && !devilFruit.hasAwakenedFruit();
    }
}
