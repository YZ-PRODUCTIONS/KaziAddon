package net.kazi.kazimod.entities.boss.luffy.goals;

import net.minecraft.entity.MobEntity;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuGomuNoBazookaAbility;

public class LuffyBazookaWrapperGoal extends DirectAbilityGoal<GomuGomuNoBazookaAbility> {
    public LuffyBazookaWrapperGoal(MobEntity entity) {
        super(entity, GomuGomuNoBazookaAbility.INSTANCE);
    }
}