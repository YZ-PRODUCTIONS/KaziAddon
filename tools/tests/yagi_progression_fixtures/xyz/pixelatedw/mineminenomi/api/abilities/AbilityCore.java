package xyz.pixelatedw.mineminenomi.api.abilities;
public final class AbilityCore<T extends IAbility> {
    public final String name;
    public AbilityCore(String name) { this.name = name; }
    @Override public String toString() { return name; }
}
