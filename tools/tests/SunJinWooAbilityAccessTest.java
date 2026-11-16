package net.kazi.kazimod.entities.boss.sunjinwoo.abilities;

import net.kazi.kazimod.entities.boss.sunjinwoo.SunJinWooBossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

/** Runs the real boss-only base and its registered can-use callback. */
public final class SunJinWooAbilityAccessTest {
    private static int checks;
    public static void main(String[] args) {
        Probe move = new Probe();
        check(move.componentDispatch(), "Boss access check must use the component dispatch path");
        LivingEntity[] denied = {null, new PlayerEntity(), new PlayerEntity() {}, new LivingEntity()};
        for (LivingEntity entity : denied) {
            check(!SunJinWooOnlyAbility.canUnlock(entity), "Non-boss must not unlock the old attack");
            check(!move.fixtureCanUse(entity), "Non-boss must not execute an externally granted attack");
        }
        for (LivingEntity boss : new LivingEntity[] {new SunJinWooBossEntity(), new SunJinWooBossEntity() {}}) {
            check(SunJinWooOnlyAbility.canUnlock(boss), "Boss keeps its existing combat kit");
            check(move.fixtureCanUse(boss), "Boss remains able to execute the attack");
        }
        System.out.println("Sun Jin Woo production access guard: " + checks + " checks passed.");
    }
    private static final class Probe extends SunJinWooOnlyAbility {
        Probe() { super(new AbilityCore<IAbility>()); }
        boolean componentDispatch() { return isNew; }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
