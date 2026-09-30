package net.kazi.kazimod.abilities.Koku;

import net.kazi.kazimod.entities.KokuVfxEntity;
import net.minecraft.entity.LivingEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

public final class KokuChargeVisual {
    private KokuVfxEntity entity;

    public void start(LivingEntity owner, IAbility ability, int mode, int duration) {
        this.stop();
        this.entity = KokuVfxEntity.charge(owner, ability, mode, duration);
    }

    public void update(float progress) {
        if (this.entity != null && this.entity.isAlive()) {
            this.entity.refresh(progress);
        }
    }

    public void stop() {
        if (this.entity != null) {
            this.entity.remove();
        }
        this.entity = null;
    }
}
