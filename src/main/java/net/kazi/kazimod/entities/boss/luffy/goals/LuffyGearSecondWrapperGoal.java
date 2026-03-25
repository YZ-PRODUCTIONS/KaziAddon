package net.kazi.kazimod.entities.boss.luffy.goals;

import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.minecraft.entity.MobEntity;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GearSecondAbility;

public class LuffyGearSecondWrapperGoal extends DirectAbilityGoal<GearSecondAbility> {
    public LuffyGearSecondWrapperGoal(MobEntity entity) {
        super(entity, GearSecondAbility.INSTANCE);
    }
    @Override
    protected boolean canUseExtra() {
        return !(entity instanceof LuffyBossEntity && ((LuffyBossEntity) entity).isGear5Awakened());
    }
    @Override protected boolean canContinueExtra() { return isContinuous() && canUseExtra(); }
}