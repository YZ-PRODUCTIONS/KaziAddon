package net.kazi.kazimod.projectiles;

public interface ClashState {
    float kazi$getClashHealth();
    void kazi$setClashHealth(float health);
    long kazi$getLastClash(int otherId);
    void kazi$markClash(int otherId, long tick);
}
