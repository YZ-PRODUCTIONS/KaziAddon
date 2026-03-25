package net.kazi.kazimod.entities.boss.luffy.goals;

import net.kazi.kazimod.abilities.GomuRework.GearFifthRework;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GearSecondAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GearThirdAbility;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.EnumSet;

/**
 * Triggers GearFifthRework when Luffy drops to 50% HP.
 * After awakening: re-triggers whenever Gear Fifth expires so it stays active.
 * canUse() always returns false — all logic runs as side effects each tick.
 */
public class LuffyGearFifthWrapperGoal extends Goal {

    private final MobEntity entity;

    public LuffyGearFifthWrapperGoal(MobEntity entity) {
        this.entity = entity;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    private LuffyBossEntity luffy() { return (LuffyBossEntity) entity; }

    private void stopGearIfActive(IAbilityData data, xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core) {
        xyz.pixelatedw.mineminenomi.api.abilities.Ability a =
                (xyz.pixelatedw.mineminenomi.api.abilities.Ability) data.getEquippedAbility(core);
        if (a == null) return;
        a.getComponent(ModAbilityKeys.CONTINUOUS)
                .filter(c -> ((xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent) c).isContinuous())
                .ifPresent(c -> ((xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent) c)
                        .stopContinuity(entity));
    }

    private void tryAwaken() {
        if (luffy().isGear5Awakened()) return;
        if (entity.getHealth() > entity.getMaxHealth() * 0.5F) return;

        luffy().setGear5Awakened(true);

        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) return;

        stopGearIfActive(data, GearSecondAbility.INSTANCE);
        stopGearIfActive(data, GearThirdAbility.INSTANCE);

        // Full heal on awakening
        entity.setHealth(entity.getMaxHealth());

        GearFifthRework g5 = (GearFifthRework) data.getEquippedAbility(GearFifthRework.INSTANCE);
        if (g5 != null && !g5.isContinuous()) g5.use(entity);
    }

    private void maintainGearFifth() {
        if (!luffy().isGear5Awakened()) return;
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) return;
        GearFifthRework g5 = (GearFifthRework) data.getEquippedAbility(GearFifthRework.INSTANCE);
        if (g5 != null && !g5.isContinuous()) g5.use(entity);
    }

    @Override
    public boolean canUse() {
        tryAwaken();
        maintainGearFifth();
        return false; // never occupies a goal slot — pure side effects
    }

    @Override public boolean canContinueToUse() { return false; }
    @Override public void start() {}
    @Override public void tick()  {}
    @Override public void stop()  {}
}