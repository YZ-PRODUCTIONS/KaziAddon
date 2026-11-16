package net.kazi.kazimod.config;
public final class KaziConfig {
    public static final KaziConfig INSTANCE = new KaziConfig();
    public final Flag disableFruitChanges = new Flag();
    public static final class Flag { public boolean value; public Boolean get() { return value; } }
}
