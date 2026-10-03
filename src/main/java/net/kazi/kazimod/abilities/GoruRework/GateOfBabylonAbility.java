package net.kazi.kazimod.abilities.GoruRework;

import net.kazi.kazimod.entities.GateOfBabylonEntity;
import net.kazi.kazimod.entities.BabylonVolleyPattern;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;

/** One press opens the treasury for a weapon barrage aimed at the nearest hostile player. */
public final class GateOfBabylonAbility extends Ability {
    public static final int CHARGE_TICKS = BabylonVolleyPattern.PREPARATION_TICKS, COOLDOWN_TICKS = 30 * 20, CANCEL_COOLDOWN_TICKS = 10 * 20;
    public static final float DAMAGE = 1;
    public static final double RANGE = 64;
    public enum Mode {
        NORMAL("Gate of Babylon", 75), URUK("Treasury of Uruk", 50), EX("Gate of Babylon EX", 100);
        public final String title;
        public final int portals;
        Mode(String title, int portals) { this.title = title; this.portals = portals; }
        @Override public String toString() { return title; }
    }
    public static final AbilityCore<GateOfBabylonAbility> INSTANCE = new AbilityCore.Builder<>(
            "Gate of Babylon", AbilityCategory.DEVIL_FRUITS, GateOfBabylonAbility::new)
            .setIcon(new ResourceLocation("kazimod", "textures/abilities/gate_of_babylon.png"))
            .setSourceHakiNature(SourceHakiNature.SPECIAL)
            .setSourceType(SourceType.INTERNAL).setSourceElement(SourceElement.SHOCKWAVE)
            .addDescriptionLine(new StringTextComponent("Open golden gates in a randomized grid two blocks behind your cast location. Each gate fires once."),
                    new StringTextComponent("Press once. Each shot aims at the nearest hostile player within 64 blocks."),
                    new StringTextComponent("Seriousness selects the form automatically: Normal - Treasury of Uruk (50 gates); Stage 1 - Gate of Babylon (75 gates); Stages 2 and 3 - Gate of Babylon EX (100 gates)."),
                    new StringTextComponent("One weapon per gate, alternating 1- and 2-tick gaps. When no hostile player is in range, aim with your crosshair."),
                    new StringTextComponent("Each weapon deals 1 base damage and flies for up to 30 seconds, passing through entities. Walls block the weapons."))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE, ChargeComponent.getTooltip(CHARGE_TICKS),
                    DealDamageComponent.getTooltip(DAMAGE), CooldownComponent.getTooltip(COOLDOWN_TICKS))
            .setUnlockCheck(SeriousnessAbility::isEligible)
            .build();

    private final ChargeComponent charge = new ChargeComponent(this)
            .addTickEvent(this::chargeTick).addEndEvent(this::release);
    private final AnimationComponent animation = new AnimationComponent(this);
    private final AltModeComponent<Mode> modes = new GateModes(this)
            .addChangeModeEvent((user, ability, mode) -> updateDisplay(mode));
    private GateOfBabylonEntity gates;
    private boolean pending;

    public GateOfBabylonAbility(AbilityCore<GateOfBabylonAbility> core) {
        super(core);
        isNew = true;
        addComponents(charge, animation, modes);
        updateDisplay(Mode.URUK);
        addEquipEvent((user, ability) -> syncSeriousnessMode(user));
        addUseEvent(this::use);
        addTickEvent((user, ability) -> {
            if (user.level.isClientSide) return;
            syncSeriousnessMode(user);
            if ((pending || charge.isCharging()) && (gates == null || !gates.isAlive() || gates.isFading()
                    || !charge.isCharging() || !user.isAlive() || isDisabled())) cancel(user);
            if (gates != null && gates.isAlive() && (!user.isAlive() || isDisabled())) gates.fade();
        });
        addRemoveEvent((user, ability) -> {
            cancel(user);
            if (gates != null && !user.level.isClientSide) gates.fade();
            gates = null;
        });
    }

    private void use(LivingEntity user, IAbility ability) {
        if (!SeriousnessAbility.isEligible(user)) return;
        if (user.level.isClientSide || pending || charge.isCharging() || gates != null && gates.isAlive()) return;
        syncSeriousnessMode(user);
        gates = new GateOfBabylonEntity(KaziEntities.GATE_OF_BABYLON.get(), user.level);
        gates.begin(user, this, modes.getCurrentMode().portals);
        if (!user.level.addFreshEntity(gates)) { gates = null; return; }
        pending = true;
        charge.startCharging(user, CHARGE_TICKS);
        animation.start(user, ModAnimations.POINT_LEFT_ARM, CHARGE_TICKS + 5,
                entity -> !entity.isAlive() || !charge.isCharging());
    }

    private void chargeTick(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide || !pending) return;
        if (!user.isAlive() || isDisabled() || gates == null || !gates.isAlive() || gates.isFading()) { cancel(user); return; }
        gates.setChargeProgress(charge.getChargePercentage());
        if (charge.getChargeTime() >= charge.getMaxChargeTime()) charge.stopCharging(user);
    }

    private void release(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        if (!pending || !charge.isCharging() || charge.getMaxChargeTime() <= 0
                || charge.getChargeTime() < charge.getMaxChargeTime() || !user.isAlive() || isDisabled()
                || gates == null || !gates.isAlive() || gates.isFading()) { cancel(user); return; }
        pending = false;
        animation.stop(user);
        gates.launch();
        cooldownComponent.startCooldown(user, COOLDOWN_TICKS);
    }

    private void cancel(LivingEntity user) {
        if (user.level.isClientSide || !pending && !charge.isCharging()) return;
        pending = false;
        if (charge.isCharging()) charge.forceStopCharging(user);
        animation.stop(user);
        if (gates != null) gates.fade();
        cooldownComponent.startCooldown(user, CANCEL_COOLDOWN_TICKS);
    }

    public boolean ownsCast(GateOfBabylonEntity entity) {
        return gates == entity && !isDisabled() && (!entity.isCharging() || pending && charge.isCharging());
    }

    public static Mode modeForStage(SeriousnessStage stage) {
        if (stage == SeriousnessStage.STAGE_TWO || stage == SeriousnessStage.STAGE_THREE) return Mode.EX;
        return stage == SeriousnessStage.STAGE_ONE ? Mode.NORMAL : Mode.URUK;
    }

    private void syncSeriousnessMode(LivingEntity user) {
        if (!user.level.isClientSide) {
            // The existing barrage owns its portal count; this selects the next cast only.
            modes.setMode(user, modeForStage(SeriousnessAbility.getStage(user)));
        }
    }

    @Override public void load(CompoundNBT tag) {
        super.load(tag);
        updateDisplay(modes.getCurrentMode());
        pending = charge.isCharging();
        gates = null; // Transient portals cannot resume from a saved ability charge.
    }

    private void updateDisplay(Mode mode) {
        setDisplayName(mode.title);
        String icon = mode == Mode.URUK ? "gate_treasury_of_uruk"
                : mode == Mode.EX ? "gate_of_babylon_ex" : "gate_of_babylon";
        setDisplayIcon(new ResourceLocation("kazimod", "textures/abilities/" + icon + ".png"));
    }

    private static final class GateModes extends AltModeComponent<Mode> {
        GateModes(GateOfBabylonAbility ability) { super(ability, Mode.class, Mode.URUK, true); }
        @Override public void setNextInCycle(LivingEntity user) {
            ((GateOfBabylonAbility)getAbility()).syncSeriousnessMode(user);
        }
        @Override public void load(CompoundNBT tag) {
            String saved = tag.getString("currentMode");
            Mode selected = "Gate of Babylon: EX".equals(saved) ? Mode.EX : Mode.URUK;
            for (Mode mode : Mode.values()) if (mode.name().equals(saved) || mode.title.equals(saved)) selected = mode;
            tag.putString("currentMode", selected.name());
            super.load(tag);
        }
    }
}
