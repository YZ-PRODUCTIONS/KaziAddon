package xyz.pixelatedw.mineminenomi.api.abilities;
public interface IAbility {
    AbilityCore<?> getCore();
    void stop();
    void onRemove();
}
