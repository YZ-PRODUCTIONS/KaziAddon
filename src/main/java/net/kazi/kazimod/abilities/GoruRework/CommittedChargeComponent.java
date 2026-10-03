package net.kazi.kazimod.abilities.GoruRework;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;

/** Once started, a live cast finishes its timer even if its ability becomes disabled. */
final class CommittedChargeComponent extends ChargeComponent {
    private final BooleanSupplier committed;
    private final BiConsumer<LivingEntity, IAbility> duringCharge;
    private float elapsed;

    CommittedChargeComponent(IAbility ability, BooleanSupplier committed,
                             BiConsumer<LivingEntity, IAbility> duringCharge) {
        super(ability);
        this.committed = committed;
        this.duringCharge = duringCharge;
    }

    @Override public void startCharging(LivingEntity user, float duration) {
        if (isCharging()) return;
        elapsed = 0;
        super.startCharging(user, duration);
        // Enkidu's already-activated counter may trigger after a fruit-disable hit.
        // Its follow-up has no start callbacks and must still complete the short charge.
        if (!isCharging() && committed.getAsBoolean() && user.isAlive()) {
            CompoundNBT tag = super.save();
            tag.putFloat("chargeTime", Float.MIN_VALUE);
            tag.putFloat("maxChargeTime", Math.max(1, duration));
            super.load(tag);
        }
    }

    @Override protected void doTick(LivingEntity user) {
        if (!isCharging()) return;
        elapsed++;
        duringCharge.accept(user, getAbility());
        if (isCharging() && elapsed >= getMaxChargeTime()) stopCharging(user);
    }

    @Override public void stopCharging(LivingEntity user) {
        if (committed.getAsBoolean() && user.isAlive() && elapsed < getMaxChargeTime()) return;
        super.stopCharging(user);
    }

    @Override public void forceStopCharging(LivingEntity user) {
        if (committed.getAsBoolean() && user.isAlive()) return;
        super.forceStopCharging(user);
        elapsed = 0;
    }

    @Override public float getChargeTime() { return elapsed; }
    @Override public float getChargePercentage() {
        return getMaxChargeTime() <= 0 ? 0 : Math.min(1, elapsed / getMaxChargeTime());
    }

    @Override public CompoundNBT save() {
        CompoundNBT tag = super.save();
        tag.putFloat("chargeTime", isCharging() ? elapsed : 0);
        return tag;
    }

    @Override public void load(CompoundNBT tag) {
        super.load(tag);
        elapsed = super.getChargeTime();
    }
}
