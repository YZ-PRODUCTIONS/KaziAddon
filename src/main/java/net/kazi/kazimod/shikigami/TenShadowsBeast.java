package net.kazi.kazimod.shikigami;

import net.minecraft.entity.LivingEntity;

public interface TenShadowsBeast {
    LivingEntity owner();
    String shadowId();
    boolean isTaming();
    void setTaming(boolean taming);
}
