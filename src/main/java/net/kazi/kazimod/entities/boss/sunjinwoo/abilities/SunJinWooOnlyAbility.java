package net.kazi.kazimod.entities.boss.sunjinwoo.abilities;

import net.kazi.kazimod.entities.boss.sunjinwoo.SunJinWooBossEntity;
import net.minecraft.entity.LivingEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

/** Internal boss attacks, isolated from Nagi's playable awakening. */
abstract class SunJinWooOnlyAbility extends Ability {
    protected SunJinWooOnlyAbility(AbilityCore<? extends IAbility> core) {
        super(core);
        this.isNew = true;
        this.addCanUseCheck((entity, ability) -> canUnlock(entity)
                ? AbilityUseResult.success() : AbilityUseResult.fail(null));
    }

    static boolean canUnlock(LivingEntity entity) {
        return entity instanceof SunJinWooBossEntity;
    }
}
