package xyz.pixelatedw.mineminenomi.api.abilities;
public final class AbilityUseResult {
    public final boolean allowed;
    private AbilityUseResult(boolean allowed) { this.allowed = allowed; }
    public static AbilityUseResult success() { return new AbilityUseResult(true); }
    public static AbilityUseResult fail(Object text) { return new AbilityUseResult(false); }
}
