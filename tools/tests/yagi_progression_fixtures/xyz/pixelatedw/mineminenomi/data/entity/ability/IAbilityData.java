package xyz.pixelatedw.mineminenomi.data.entity.ability;
import java.util.List;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
public interface IAbilityData {
    List<IAbility> getEquippedAndPassiveAbilities();
    boolean removeEquippedAbility(AbilityCore<?> core);
    IAbility getPassiveAbility(AbilityCore<?> core);
    void removePassiveAbility(AbilityCore<?> core);
    boolean hasUnlockedAbility(AbilityCore<?> core);
    void removeUnlockedAbility(AbilityCore<?> core);
    void addUnlockedAbility(AbilityCore<?> core, AbilityUnlock unlock);
}
