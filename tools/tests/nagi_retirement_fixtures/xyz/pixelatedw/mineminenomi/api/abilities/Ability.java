package xyz.pixelatedw.mineminenomi.api.abilities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.entity.LivingEntity;

public class Ability implements IAbility {
    protected boolean isNew;
    private final List<BiFunction<LivingEntity, IAbility, AbilityUseResult>> checks = new ArrayList<>();
    public Ability(AbilityCore<? extends IAbility> core) {}
    protected void addCanUseCheck(BiFunction<LivingEntity, IAbility, AbilityUseResult> check) { checks.add(check); }
    public boolean fixtureCanUse(LivingEntity user) {
        if (checks.isEmpty()) throw new AssertionError("No use-time guard was attached");
        for (BiFunction<LivingEntity, IAbility, AbilityUseResult> check : checks) {
            if (!check.apply(user, this).allowed) return false;
        }
        return true;
    }
}
