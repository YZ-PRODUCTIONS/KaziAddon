package net.kazi.kazimod.ope;

import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public abstract class WilleAbility extends Ability {
    private final boolean puncture;
    private final int chargeTicks, cooldown;
    private final AnimationComponent animation = new AnimationComponent(this);
    private final ChargeComponent charge = new ChargeComponent(this)
            .addStartEvent(this::start).addTickEvent(this::charging).addEndEvent(this::finish);

    protected WilleAbility(AbilityCore core, boolean puncture) {
        super(core);
        this.puncture = puncture;
        this.chargeTicks = puncture ? 100 : 40;
        this.cooldown = puncture ? 1000 : 400;
        this.isNew = true;
        addComponents(animation, charge);
        addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        addUseEvent((user, ability) -> {
            if (charge.isCharging() || OpeWille.active(user)) return;
            if (!hasKRoomActive(user)) {
                user.sendMessage(new StringTextComponent("You need to activate K-Room!"), user.getUUID());
                return;
            }
            charge.startCharging(user, chargeTicks);
        });
        addTickEvent((user, ability) -> {
            if (!user.level.isClientSide && !charge.isCharging() && !OpeWille.active(user)) animation.stop(user);
        });
    }
    private void start(LivingEntity user, IAbility ability) {
        animation.start(user, net.kazi.kazimod.init.KaziAnimations.WILLE_THRUST);
        if (!user.level.isClientSide) OpeWille.begin(user, this, puncture);
    }
    private void charging(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        if (!OpeWille.active(user) || !hasKRoomActive(user) || !AbilityHelper.canUseSwordsmanAbilities(user) || !user.isAlive()) {
            charge.forceStopCharging(user);
            OpeWille.cancel(user);
            animation.stop(user);
            cooldownComponent.startCooldown(user, 40);
            return;
        }
        OpeWille.charge(user, charge.getChargePercentage());
        if (user.tickCount % 20 == 0)
            user.level.playSound(null, user.blockPosition(), ModSounds.ROOM_CHARGE_SFX.get(), SoundCategory.PLAYERS, 1.5F, .9F);
    }
    private void finish(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        if (charge.getChargePercentage() < .99F || !hasKRoomActive(user) || !AbilityHelper.canUseSwordsmanAbilities(user)) {
            OpeWille.cancel(user); animation.stop(user); return;
        }
        OpeWille.discharge(user);
        OpeRoomMode.consume(user);
        cooldownComponent.startCooldown(user, cooldown);
    }
    public boolean hasKRoomActive(LivingEntity user) {
        return OpeRoomMode.isActive(user);
    }
    public boolean isBusy() { return charge.isCharging(); }
    // Retain cleanup entry points for projectiles saved by older versions.
    public void clearActiveProjectile(UUID ignored) {}
    public void clearProjectileState(LivingEntity user, UUID ignored) {}
    public void clearSwordStretch(LivingEntity user) {}
}
